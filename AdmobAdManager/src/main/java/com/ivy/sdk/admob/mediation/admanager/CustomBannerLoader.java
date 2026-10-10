package com.ivy.sdk.admob.mediation.admanager;

import android.content.Context;
import android.view.View;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

import org.json.JSONObject;

public class CustomBannerLoader extends AdListener implements MediationBannerAd {

    private final MediationBannerAdConfiguration adConfiguration;
    private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback;
    private MediationBannerAdCallback mediationBannerAdCallback;
    private AdView adView;

    public CustomBannerLoader(MediationBannerAdConfiguration adConfiguration,
                              MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
        this.adConfiguration = adConfiguration;
        this.callback = callback;
    }

    @Override
    public View getView() {
        return adView;
    }

    public void loadAd() {
        Context context = adConfiguration.getContext();
        adView = new AdView(context);
        String placementId = null;

        try {
            String serverParameter = adConfiguration.getServerParameters().getString("parameter");
            placementId = new JSONObject(serverParameter).getString(CustomAdEvent.KEY_PLACEMENT_ID);
        } catch (Exception e) {
            // 忽略异常
        }

        if (placementId != null) {
            if (placementId.isEmpty()) {
                callback.onFailure(
                        new AdError(CustomAdEvent.ERROR_NULL_CONTEXT,
                                "Google ad manager requires a non-null ad Id.",
                                CustomAdEvent.DOMAIN));
                return;
            }

            AdRequest adRequest = new AdRequest.Builder().build();
            adView.setAdUnitId(placementId);
            adView.setAdSize(AdSize.BANNER);
            adView.setAdListener(this);
            adView.loadAd(adRequest);
        } else {
            callback.onFailure(
                    new AdError(CustomAdEvent.ERROR_NULL_CONTEXT,
                            "Google ad manager requires a non-null ad Id.",
                            CustomAdEvent.DOMAIN));
        }
    }

    @Override
    public void onAdLoaded() {
        super.onAdLoaded();
        mediationBannerAdCallback = callback.onSuccess(this);
    }

    @Override
    public void onAdFailedToLoad(LoadAdError loadAdError) {
        super.onAdFailedToLoad(loadAdError);
        callback.onFailure(loadAdError);
    }

    @Override
    public void onAdOpened() {
        super.onAdOpened();
        if (mediationBannerAdCallback != null) {
            mediationBannerAdCallback.onAdOpened();
        }
    }

    @Override
    public void onAdImpression() {
        super.onAdImpression();
        if (mediationBannerAdCallback != null) {
            mediationBannerAdCallback.reportAdImpression();
        }
    }

    @Override
    public void onAdClicked() {
        super.onAdClicked();
        if (mediationBannerAdCallback != null) {
            mediationBannerAdCallback.reportAdClicked();
        }
    }

    @Override
    public void onAdClosed() {
        super.onAdClosed();
        if (mediationBannerAdCallback != null) {
            mediationBannerAdCallback.onAdClosed();
        }
    }
}
