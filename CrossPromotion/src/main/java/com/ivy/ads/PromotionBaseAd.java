package com.ivy.ads;

import android.app.Activity;

import com.ivy.base.ad.AdType;
import com.ivy.crosspromotion.CrossPromotion;
import com.ivy.crosspromotion.CrossPromotionConfig;

import org.json.JSONObject;

abstract class PromotionBaseAd extends BaseAd {
    private boolean displayed;
    private boolean rewarded;

    @Override
    public void init(
            AdType adType,
            JSONObject data,
            AdRevenueCallback revenueCallback,
            Activity context
    ) {
        super.init(adType, data, revenueCallback, context);
        networkName = "promotion";
        currencyCode = "USD";
        price = data.optDouble("ecpm", 0.0) / 1000.0;
        PromotionEventRouter.INSTANCE.register(promotionFormat().value(), this);
    }

    protected abstract CrossPromotionConfig.Format promotionFormat();

    @Override
    protected boolean isNofillError(int errorCode, String errorMsg) {
        return errorCode == 204;
    }

    @Override
    protected void loadAd() {

    }

    protected final void beginShow(AdShowCallback callback) {
        showCallback = callback;
        displayed = false;
        rewarded = false;
        isShowing = true;
    }

    protected final void showNotReady(AdShowCallback callback) {
        if (callback != null) {
            callback.onAdShowFailed(this, -2, "Promotion is not ready");
        }
    }

    final void handleImpression() {
        if (!displayed) {
            displayed = true;
            if (showCallback != null) {
                showCallback.onAdDisplayed(this);
            }
        }
    }

    final void handleClick() {
        if (showCallback != null) {
            showCallback.onAdClicked(this);
        }
    }

    final void handleReward() {
        rewarded = true;
    }

    final void handleClosed() {
        if (!isShowing) {
            return;
        }
        isShowing = false;
        if (showCallback != null) {
            showCallback.onAdClosed(this, rewarded);
        }
    }
}
