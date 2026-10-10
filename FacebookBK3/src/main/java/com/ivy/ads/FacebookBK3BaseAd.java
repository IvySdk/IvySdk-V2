package com.ivy.ads;

import android.app.Activity;
import android.util.Log;

import com.facebook.ads.AdSettings;
import com.facebook.biddingkit.gen.BidWithNotification;
import com.facebook.biddingkit.gen.FacebookAdBidFormat;
import com.ivy.base.ad.AdType;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class FacebookBK3BaseAd extends BaseAd {
    private static final String TAG = "FacebookBK3BaseAd";
    private static final ExecutorService NOTIFICATION_EXECUTOR = Executors.newSingleThreadExecutor(runnable ->
            new Thread(runnable, "facebook-bk3-notification"));

    protected double bidFloor = 0.0;
    protected int timeoutMs = 0;
    protected boolean limitedDataUse = false;
    protected BidWithNotification bid;

    @Override
    public void init(AdType adType, JSONObject data, AdRevenueCallback revenueCallback, Activity context) {
        super.init(adType, data, revenueCallback, context);
        bidFloor = data.optDouble("bidfloor", 0.0);
        timeoutMs = data.optInt("timeout", 0);
        limitedDataUse = data.optBoolean("limitedDataUse", false);
        networkName = "facebook";
        platform = "facebook_bk3";
    }

    protected void requestBid(FacebookAdBidFormat format, FacebookBK3BidRequester.BidCallback callback) {
        FacebookBK3BidRequester.requestFacebookBid(
                context,
                FacebookBK3Init.getAppId(),
                adUnit,
                format,
                FacebookBK3Init.getPlatformId(),
                bidFloor,
                timeoutMs,
                FacebookBK3Init.isDebug(),
                false,
                limitedDataUse,
                callback
        );
    }

    protected void onBidLoaded(BidWithNotification bid) {
        this.bid = bid;
        price = bid.getPrice() / 100000.0;
        currencyCode = bid.getCurrency() == null ? "USD" : bid.getCurrency();
    }

    protected void notifyWin() {
        BidWithNotification currentBid = bid;
        double currentPrice = price;
        if (currentBid != null) {
            runNotificationAsync("notify-win", () -> currentBid.notifyWin(currentPrice * 100000.0));
        }
    }

    @Override
    public void onLoss(String bidder, float winningPrice) {
        BidWithNotification currentBid = bid;
        if (currentBid != null) {
            runNotificationAsync("notify-loss", () -> currentBid.notifyLoss(winningPrice * 100000.0));
        }
    }

    private void runNotificationAsync(String name, Runnable notification) {
        NOTIFICATION_EXECUTOR.execute(() -> {
            try {
                notification.run();
            } catch (Throwable e) {
                Log.w(TAG, name + " failed", e);
            }
        });
    }

    @Override
    protected boolean isNofillError(int errorCode, String errorMsg) {
        return errorCode == 1001 || errorCode == 9;
    }
}
