package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdapterResponseInfo;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.ivy.base.ad.ivy.IvyAdType;
import com.ivy.base.util.Logger;

import org.json.JSONObject;

import java.util.Locale;

public class AdmobNonRewardedAdapter extends FullpageAdapter<FullpageAdapter.GridParams> {
    private static final String TAG = "Adapter-Admob-Interstitial";
    private InterstitialAd mInterstitial;
    private String mediation = null;

    public AdmobNonRewardedAdapter(Context context, String gridName, IvyAdType adType) {
        super(context, gridName, adType);
    }

    private FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
        @Override
        public void onAdFailedToShowFullScreenContent(AdError adError) {
            String message = adError != null ? adError.toString() : " Null";
            Logger.debug(TAG, "onAdFailedToShowFullScreenContent, adError: " + message);
            onAdShowFail();
            mInterstitial = null;
        }

        @Override
        public void onAdClicked() {
            AdmobNonRewardedAdapter.this.onAdClicked();
        }

        @Override
        public void onAdShowedFullScreenContent() {
            Logger.debug(TAG, "onAdShowedFullScreenContent");
            AdmobNonRewardedAdapter.this.onAdShowSuccess();
            mInterstitial = null;
        }

        @Override
        public void onAdDismissedFullScreenContent() {
            Logger.debug(TAG, "onAdDismissedFullScreenContent");
            onAdClosed(false);
        }
    };
    private final OnPaidEventListener onPaidEventListener = adValue -> {
        Logger.debug(TAG, "onPaidEvent with " + adValue.toString());
        try {
            Logger.debug(TAG, String.format(Locale.US, "Paid event of value %d in currency %s of precision %s%n.", adValue.getValueMicros(), adValue.getCurrencyCode(), adValue.getPrecisionType()));
            String currencyCode = adValue.getCurrencyCode();
            int precisionType = adValue.getPrecisionType();
            long valueMicros = adValue.getValueMicros();
            onGmsPaidEvent("admob", "interstitial", "interstitial", getPlacementId(), currencyCode, precisionType, valueMicros);

        } catch (Throwable t) {
            Logger.error(TAG, "onPaidEvent exception", t);
        }
    };

    public void fetch(Activity activity) {
        String placement = getPlacementId();
        if (placement == null || "".equals(placement)) {
            super.onAdLoadFailed("INVALID");
            return;
        }
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();

        InterstitialAd.load(activity, placement, adRequestBuilder.build(), new InterstitialAdLoadCallback() {
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                AdmobNonRewardedAdapter.this.mInterstitial = interstitialAd;
                AdmobNonRewardedAdapter.this.mInterstitial.setFullScreenContentCallback(fullScreenContentCallback);
                AdmobNonRewardedAdapter.this.mInterstitial.setOnPaidEventListener(onPaidEventListener);

                try {
                    mediation = mInterstitial.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName();
                } catch (Exception e) {
                }
                onAdLoadSuccess();
            }

            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                int errorCode = loadAdError.getCode();
                Logger.warning(TAG, "onAdFailedToLoad : " + errorCode);
                AdmobNonRewardedAdapter.this.mInterstitial = null;
                onAdLoadFailed(loadAdError.getMessage());
            }
        });
    }

    public void show(Activity activity) {
        if (this.mInterstitial != null) {
            try {
                if (adBundle == null) {
                    adBundle = new Bundle();
                } else {
                    adBundle.clear();
                }
                mediation = mInterstitial.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName();
                AdapterResponseInfo loadedAdapterResponseInfo = AdmobNonRewardedAdapter.this.mInterstitial.getResponseInfo().getLoadedAdapterResponseInfo();
                String adSourceName = loadedAdapterResponseInfo.getAdSourceName();
//                String adSourceId = loadedAdapterResponseInfo.getAdSourceId();
                String adSourceInstanceName = loadedAdapterResponseInfo.getAdSourceInstanceName();
//                String adSourceInstanceId = loadedAdapterResponseInfo.getAdSourceInstanceId();
                Bundle extras = AdmobNonRewardedAdapter.this.mInterstitial.getResponseInfo().getResponseExtras();
                String mediationGroupName = extras.getString("mediation_group_name");
                String mediationABTestName = extras.getString("mediation_ab_test_name");
//                String mediationABTestVariant = extras.getString("mediation_ab_test_variant");
                adBundle.putString("ad_network", adSourceInstanceName);
                adBundle.putString("ad_source_instance", adSourceInstanceName);
                adBundle.putString("mediation_group", mediationGroupName);
                adBundle.putString("mediation_ab_test", mediationABTestName);
                Log.e("admobLog", "interstitial loaded");
                for (String s : adBundle.keySet()) {
                    Log.e("admobLog", "key:" + s + " :: value:" + adBundle.get(s));
                }
            } catch (Exception e) {
                AdmobNonRewardedAdapter.this.mediation = null;
            }
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
        return mediation;
    }

    public static class GridParams extends com.ivy.ads.adapters.BaseAdapter.GridParams {
        public String placement;

        public GridParams fromJSON(JSONObject jsonObject) {
            try {
                this.placement = jsonObject.optString("placement");
            } catch (Exception e) {
                Logger.debug(e.getMessage());
                this.placement = jsonObject.optString("placement");
            }
            //this.placement = jsonObject.optString("placement");
            return this;
        }

        protected String getParams() {
            return "placement=" + this.placement;
        }
    }
}