package com.ivy.sdk.admob.mediation.aps;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.amazon.device.ads.*;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.*;
import org.json.JSONObject;

public class CustomInterstitialLoader implements MediationInterstitialAd, DTBAdCallback, DTBAdInterstitialListener {

    private final MediationInterstitialAdConfiguration adConfiguration;
    private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback;

    private MediationInterstitialAdCallback mediationInterstitialAdLoadCallback;
    private DTBAdInterstitial interstitialAd;

    public CustomInterstitialLoader(MediationInterstitialAdConfiguration adConfiguration,
                                    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        this.adConfiguration = adConfiguration;
        this.callback = callback;
    }

    @Override
    public void showAd(Context context) {
        // Implementation for showing interstitial ad
    }

    public void loadAd() {
        Context context = adConfiguration.getContext();
        String placementId = null;
        String appId = null;

        try {
            String serverParameter = adConfiguration.getServerParameters().getString("parameter");
            Log.i(CustomAdEvent.domain, "loaded config for interstitial:" + serverParameter);
            JSONObject json = new JSONObject(serverParameter);
            placementId = json.getString(CustomAdEvent.KEY_PLACEMENT_ID);
            appId = json.optString("appId");
        } catch (Exception e) {
            // Ignore exception
        }

        if (appId == null || appId.isEmpty()) {
            callback.onFailure(new AdError(CustomAdEvent.ERROR_INVALID_APP_ID, "invalid app id", CustomAdEvent.domain));
            return;
        }

        AdRegistration.getInstance(appId, context);

        if (placementId == null || placementId.isEmpty()) {
            callback.onFailure(new AdError(CustomAdEvent.ERROR_INVALID_ADUNIT, "invalid adunit id", CustomAdEvent.domain));
            return;
        }

        DTBAdNetworkInfo network = new DTBAdNetworkInfo(DTBAdNetwork.ADMOB);
        DTBAdRequest dTBAdRequest = new DTBAdRequest(network);
        dTBAdRequest.setSizes(new DTBAdSize.DTBInterstitialAdSize(placementId));
        dTBAdRequest.loadAd(this);
        Log.i(CustomAdEvent.domain, "start load aps interstitial");
    }

    @Override
    public void onFailure(com.amazon.device.ads.AdError adError) {
        Log.i(CustomAdEvent.domain, "aps interstitial load failed:" + adError.getMessage());
        callback.onFailure(new AdError(adError.getCode().ordinal(), adError.getMessage(), CustomAdEvent.domain));
    }

    @Override
    public void onSuccess(DTBAdResponse dtbAdResponse) {
        try {
            Bundle bidInfo = dtbAdResponse.getRenderingBundle();
            interstitialAd = new DTBAdInterstitial(adConfiguration.getContext(), this);
            if (interstitialAd != null) {
                Log.i(CustomAdEvent.domain, "aps interstitial load success, start bidding...");
                interstitialAd.fetchAd(bidInfo);
            } else {
                Log.i(CustomAdEvent.domain, "aps interstitial load success, but ad view is null");
            }
        } catch (Exception e) {
            Log.i(CustomAdEvent.domain, "aps interstitial load response success, but render failed");
            callback.onFailure(new AdError(0, "render failed", CustomAdEvent.domain));
        }
    }

    @Override
    public void onAdLoaded(View view) {
        Log.i(CustomAdEvent.domain, "aps interstitial load success finally");
        mediationInterstitialAdLoadCallback = callback.onSuccess(this);
    }

    @Override
    public void onAdFailed(View view) {
        Log.i(CustomAdEvent.domain, "aps interstitial load failed finally");
        callback.onFailure(new AdError(CustomAdEvent.ERROR_PLACEMENT_STATE_NO_FILL, "no fill", CustomAdEvent.domain));
    }

    @Override
    public void onAdClicked(View view) {
        if (mediationInterstitialAdLoadCallback != null) {
            mediationInterstitialAdLoadCallback.reportAdClicked();
        }
    }

    @Override
    public void onAdLeftApplication(View view) {
        if (mediationInterstitialAdLoadCallback != null) {
            mediationInterstitialAdLoadCallback.onAdLeftApplication();
        }
    }

    @Override
    public void onAdOpen(View view) {
        if (mediationInterstitialAdLoadCallback != null) {
            mediationInterstitialAdLoadCallback.onAdOpened();
        }
    }

    @Override
    public void onAdClosed(View view) {
        if (mediationInterstitialAdLoadCallback != null) {
            mediationInterstitialAdLoadCallback.onAdClosed();
        }
    }

    @Override
    public void onImpressionFired(View view) {
        Log.i(CustomAdEvent.domain, "aps interstitial impression");
        if (mediationInterstitialAdLoadCallback != null) {
            mediationInterstitialAdLoadCallback.reportAdImpression();
        }
    }
}