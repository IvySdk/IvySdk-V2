package com.ivy.sdk.admob.mediation.admanager;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

import org.json.JSONObject;

public class CustomInterstitialLoader extends FullScreenContentCallback implements MediationInterstitialAd {

    private final MediationInterstitialAdConfiguration adConfiguration;
    private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback;

    private MediationInterstitialAdCallback mediationInterstitialAdCallback;
    private InterstitialAd interstitialAd;

    public CustomInterstitialLoader(
            MediationInterstitialAdConfiguration adConfiguration,
            MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback
    ) {
        this.adConfiguration = adConfiguration;
        this.callback = callback;
    }

    public void loadAd() {
        String placementId = null;
        try {
            String serverParameter = adConfiguration.getServerParameters().getString("parameter");
            placementId = new JSONObject(serverParameter).getString(CustomAdEvent.KEY_PLACEMENT_ID);
        } catch (Exception e) {
            // ignore
        }

        if (placementId != null && !placementId.isEmpty()) {
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(
                    adConfiguration.getContext(),
                    placementId,
                    adRequest,
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(LoadAdError adError) {
                            super.onAdFailedToLoad(adError);
                            callback.onFailure(adError);
                        }

                        @Override
                        public void onAdLoaded(InterstitialAd interstitialAd) {
                            super.onAdLoaded(interstitialAd);
                            CustomInterstitialLoader.this.interstitialAd = interstitialAd;
                            mediationInterstitialAdCallback = callback.onSuccess(CustomInterstitialLoader.this);
                        }
                    }
            );
        } else {
            callback.onFailure(new AdError(
                    CustomAdEvent.ERROR_NULL_CONTEXT,
                    "Google ad manager requires a non-null ad Id.",
                    CustomAdEvent.DOMAIN
            ));
        }
    }

    @Override
    public void showAd(Context context) {
        if (interstitialAd != null) {
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                interstitialAd.setFullScreenContentCallback(this);
                interstitialAd.show(activity);
            } else {
                if (mediationInterstitialAdCallback != null) {
                    mediationInterstitialAdCallback.onAdFailedToShow(
                            new AdError(
                                    CustomAdEvent.ERROR_AD_NOT_READY,
                                    "Activity instance required to show interstitial ad",
                                    CustomAdEvent.DOMAIN
                            )
                    );
                }
            }
        } else {
            if (mediationInterstitialAdCallback != null) {
                mediationInterstitialAdCallback.onAdFailedToShow(
                        new AdError(
                                CustomAdEvent.ERROR_AD_NOT_READY,
                                "Interstitial ad not ready",
                                CustomAdEvent.DOMAIN
                        )
                );
            }
        }
    }

    @Override
    public void onAdFailedToShowFullScreenContent(AdError adError) {
        super.onAdFailedToShowFullScreenContent(adError);
        if (mediationInterstitialAdCallback != null) {
            mediationInterstitialAdCallback.onAdFailedToShow(adError);
        }
    }

    @Override
    public void onAdClicked() {
        super.onAdClicked();
        if (mediationInterstitialAdCallback != null) {
            mediationInterstitialAdCallback.reportAdClicked();
        }
    }

    @Override
    public void onAdDismissedFullScreenContent() {
        super.onAdDismissedFullScreenContent();
        if (mediationInterstitialAdCallback != null) {
            mediationInterstitialAdCallback.onAdClosed();
        }
    }

    @Override
    public void onAdImpression() {
        super.onAdImpression();
        if (mediationInterstitialAdCallback != null) {
            mediationInterstitialAdCallback.reportAdImpression();
        }
    }

    @Override
    public void onAdShowedFullScreenContent() {
        super.onAdShowedFullScreenContent();
        if (mediationInterstitialAdCallback != null) {
            mediationInterstitialAdCallback.onAdOpened();
        }
    }
}
