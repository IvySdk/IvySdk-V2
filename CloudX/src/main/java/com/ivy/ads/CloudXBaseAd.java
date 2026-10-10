package com.ivy.ads;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.ivy.base.ad.AdType;

import org.json.JSONObject;

import io.cloudx.sdk.CloudXAd;
import io.cloudx.sdk.CloudXAdRevenueListener;

public abstract class CloudXBaseAd extends BaseAd implements CloudXAdRevenueListener {

    @Override
    public void init(AdType adType, JSONObject data, AdRevenueCallback revenueCallback, Activity context) {
        super.init(adType, data, revenueCallback, context);
        networkName = "CloudX";
        platform = "cloudx";
        onInited();
    }

    protected abstract void onInited();

    @Override
    protected boolean isNofillError(int errorCode, String errorMsg) {
        return errorCode == 302;
    }

    @Override
    public void onAdRevenuePaid(@NonNull CloudXAd cloudXAd) {
        price = cloudXAd.getRevenue();
        this.currencyCode = "USD";
        if (revenueCallback != null) {
            revenueCallback.onAdRevenuePaid(this);
        }
    }
}
