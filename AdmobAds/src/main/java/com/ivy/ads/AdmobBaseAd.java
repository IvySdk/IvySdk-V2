package com.ivy.ads;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.OnPaidEventListener;
import com.ivy.base.ad.AdType;

import org.json.JSONObject;

public abstract class AdmobBaseAd extends BaseAd implements OnPaidEventListener {
    protected boolean isAdmanager = false;
    protected boolean isPreload = false;
    protected int preloadCount = 0;

    @Override
    public void init(AdType adType, JSONObject data, AdRevenueCallback revenueCallback, Activity context) {
        super.init(adType, data, revenueCallback, context);
        if (data.has("admanager")){
            isAdmanager = data.optBoolean("admanager", false);
        }
        if (data.has("preload")){
            preloadCount = data.optInt("preload", 0);
            isPreload = preloadCount > 0;
        }
        networkName = "admob";
        platform = "admob";
    }

    @Override
    protected boolean isNofillError(int errorCode, String errorMsg) {
        return errorCode == 3;
    }

    @Override
    public void onPaidEvent(@NonNull AdValue adValue) {
        long revenueMicros = adValue.getValueMicros();
        String currencyCode = adValue.getCurrencyCode();

        // Convert revenue from micros to standard units
        price = revenueMicros / 1_000_000.0;
        this.currencyCode = currencyCode;

        // Trigger revenue callback
        if (revenueCallback != null) {
            revenueCallback.onAdRevenuePaid(this);
        }
    }
}
