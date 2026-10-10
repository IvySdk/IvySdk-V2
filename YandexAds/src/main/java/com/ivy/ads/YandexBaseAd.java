package com.ivy.ads;

import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;

import org.json.JSONObject;

public abstract class YandexBaseAd extends BaseAd {
    @Override
    protected boolean isNofillError(int errorCode, String errorMsg) {
        return errorCode == AdRequestError.Code.NO_FILL;
    }

    public void onPaidEvent(ImpressionData idata) {
        try {
            JSONObject data = new JSONObject(idata.getRawData());
            double revenue = data.getDouble("revenueUSD");

            this.currencyCode = "USD";
            this.price = revenue;

            JSONObject network = data.optJSONObject("network");
            if (network != null) {
                networkName = network.optString("name");
            }

            // Trigger revenue callback
            if (revenueCallback != null) {
                revenueCallback.onAdRevenuePaid(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
