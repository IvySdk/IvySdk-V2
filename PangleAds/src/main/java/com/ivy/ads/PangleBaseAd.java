package com.ivy.ads;

import android.app.Activity;
import android.util.Log;

import com.bytedance.sdk.openadsdk.api.PAGAdListener;
import com.ivy.base.ad.AdType;

import org.json.JSONArray;
import org.json.JSONObject;

public abstract class PangleBaseAd extends BaseAd {
    // refreshInterval
    protected int refreshInterval;

    @Override
    public void init(AdType adType, JSONObject data, AdRevenueCallback revenueCallback, Activity context) {
        super.init(adType, data, revenueCallback, context);
        refreshInterval = data.optInt("refreshInterval", 0);
        this.networkName = "pangle";
        onInited();
    }

    protected abstract void onInited();

    @Override
    protected boolean isNofillError(int errorCode, String errorMsg) {
        return errorCode == 20001 || errorCode == 41007;
    }

    //---------------------- MaxAdListener 接口实现 ----------------------
//
//    @Override
//    public void onAdLoaded(MaxAd ad) {
//        isLoading = false;
//        price = ad.getRevenue();
//        networkName = ad.getNetworkName();
//
//        if (loadCallback != null) {
//            loadCallback.onLoadSuccess(this);
//        }
//
//        printWatfallName(ad);
//    }
//
//    @Override
//    public void onAdLoadFailed(String adUnitId, MaxError error) {
//        isLoading = false;
//        price = 0.0;
//
//        if (loadCallback != null) {
//            loadCallback.onLoadFailed(this, error.getCode(), error.getMessage());
//        }
//    }
//
//    @Override
//    public void onAdDisplayed(MaxAd ad) {
//        if (showCallback != null) {
//            showCallback.onAdDisplayed(this);
//        }
//    }
//
//    @Override
//    public void onAdClicked(MaxAd ad) {
//        if (showCallback != null) {
//            showCallback.onAdClicked(this);
//        }
//    }
//
//    @Override
//    public void onAdHidden(MaxAd ad) {
//        isShowing = false;
//        // 插屏广告没有奖励，所以rewarded参数始终为false
//        if (showCallback != null) {
//            showCallback.onAdClosed(this, false);
//        }
//    }
//
//    @Override
//    public void onAdDisplayFailed(MaxAd ad, MaxError error) {
//        isShowing = false;
//        if (showCallback != null) {
//            showCallback.onAdShowFailed(this, error.getCode(), error.getMessage());
//        }
//    }
//
//    //---------------------- MaxAdRevenueListener 接口实现 ----------------------
//
//    @Override
//    public void onAdRevenuePaid(MaxAd ad) {
//        price = ad.getRevenue();
//        networkName = ad.getNetworkName();
//        // 触发收益回调
//        if (revenueCallback != null) {
//            revenueCallback.onAdRevenuePaid(this);
//        }
//    }
}
