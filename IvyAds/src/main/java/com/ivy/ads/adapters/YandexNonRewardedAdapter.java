package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ivy.base.ad.ivy.IvyAdType;
import com.ivy.base.util.Logger;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;

import org.json.JSONObject;

public class YandexNonRewardedAdapter extends FullpageAdapter<FullpageAdapter.GridParams> {
    private static final String TAG = "Adapter-Yandex-Interstitial";
    private InterstitialAd mInterstitial = null;

    public YandexNonRewardedAdapter(Context context, String gridName, IvyAdType adType) {
        super(context, gridName, adType);
    }

    @Override
    public void setup(Activity activity) {
        super.setup(activity);
        YandexManager.getInstance().initialize(activity, null);
    }

    private InterstitialAdEventListener adEventListener = new InterstitialAdEventListener() {
        @Override
        public void onAdShown() {
            Logger.debug(TAG, "onAdShown()");
            YandexNonRewardedAdapter.this.onAdShowSuccess();
        }

        @Override
        public void onAdFailedToShow(@NonNull AdError adError) {
            YandexNonRewardedAdapter.this.onAdShowFail();
        }

        @Override
        public void onAdDismissed() {
            Logger.debug(TAG, "onAdDismissed()");
            YandexNonRewardedAdapter.this.onAdClosed(false);
        }

        @Override
        public void onAdClicked() {
            Logger.debug(TAG, "onAdClicked()");
            YandexNonRewardedAdapter.this.onAdClicked();
        }

        @Override
        public void onAdImpression(@Nullable ImpressionData impressionData) {
            try{
                assert impressionData != null;
                JSONObject data = new JSONObject(impressionData.getRawData());
                JSONObject network = data.getJSONObject("network");
                if (adBundle == null) {
                    adBundle = new Bundle();
                } else {
                    adBundle.clear();
                }
                String ad_network = network.getString("adapter");
                String adUnitId = network.getString("ad_unit_id");
                double revenue = data.getDouble("revenueUSD");
                adBundle.putString("ad_network", ad_network);
                adBundle.putString("ad_source_instance", ad_network);
                adBundle.putString("mediation_group", network.getString("name"));

                onGmsPaidEvent(ad_network, "interstitial", "interstitial", adUnitId,"USD", 1, (long) (revenue * 1000000.0f));
            } catch (Exception e){
            }
        }
    };

    public void fetch(Activity activity) {
        String placement = getPlacementId();
        if (placement == null || "".equals(placement)) {
            super.onAdLoadFailed("INVALID");
            return;
        }
        if (!YandexManager.getInstance().isSdkInitialized()) {
            Logger.error("Yandex sdk not initialized");
            onAdLoadFailed("not_init");
            return;
        }
        InterstitialAdLoader adLoader = new InterstitialAdLoader(activity.getApplicationContext());
        adLoader.loadAd(new AdRequest.Builder(placement).build(), new InterstitialAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                YandexNonRewardedAdapter.this.mInterstitial = interstitialAd;
                YandexNonRewardedAdapter.this.mInterstitial.setAdEventListener(adEventListener);
                Logger.debug(TAG, "onAdLoaded()");
                YandexNonRewardedAdapter.this.onAdLoadSuccess();
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                int errorCode = adRequestError.getCode();
                String description = adRequestError.getDescription();
                Logger.debug(TAG, "onAdFailedToLoad, error: " + errorCode + ", description: " + description);
                YandexNonRewardedAdapter.this.onAdLoadFailed(description);
            }
        });
    }

    public void show(Activity activity) {
        if (this.mInterstitial != null) {
            this.mInterstitial.show(activity);
        } else {
            super.onAdShowFail();
        }
    }

    @Override
    public String getPlacementId() {
        return ((GridParams) getGridParams()).placement;
    }

    protected GridParams newGridParams() {
        return new GridParams();
    }

  @Override
  public String getMediation() {
    return "yandex";
  }

  public static class GridParams extends com.ivy.ads.adapters.BaseAdapter.GridParams {
        public String placement;

        public GridParams fromJSON(JSONObject jsonObject) {
            this.placement = jsonObject.optString("placement");
            return this;
        }

        protected String getParams() {
            return "placement=" + this.placement;
        }
    }
}