package com.ivy.ads;

import android.app.Activity;

import com.ivy.base.ad.AdType;

import org.json.JSONObject;

abstract class XiaomiBaseAd extends BaseAd {
    @Override
    public void init(AdType adType, JSONObject data, AdRevenueCallback revenueCallback,
                     Activity context) {
        super.init(adType, data, revenueCallback, context);
        networkName = "xiaomi";
        platform = "xiaomi";
        price = Math.max(0.0, data.optDouble("price", 0.0));
    }

    @Override
    protected boolean isNofillError(int errorCode, String errorMsg) {
        return errorCode == 1001;
    }
}
