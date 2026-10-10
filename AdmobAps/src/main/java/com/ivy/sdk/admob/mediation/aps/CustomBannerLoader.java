package com.ivy.sdk.admob.mediation.aps;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.DTBAdBannerListener;
import com.amazon.device.ads.DTBAdCallback;
import com.amazon.device.ads.DTBAdNetwork;
import com.amazon.device.ads.DTBAdNetworkInfo;
import com.amazon.device.ads.DTBAdRequest;
import com.amazon.device.ads.DTBAdResponse;
import com.amazon.device.ads.DTBAdSize;
import com.amazon.device.ads.DTBAdView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

import org.json.JSONObject;

public class CustomBannerLoader implements MediationBannerAd, DTBAdCallback, DTBAdBannerListener {

    private final MediationBannerAdConfiguration adConfiguration;
    private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback;

    private MediationBannerAdCallback mediationBannerAdCallback;
    private DTBAdView dtbAdView;

    public CustomBannerLoader(MediationBannerAdConfiguration adConfiguration, MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
        this.adConfiguration = adConfiguration;
        this.callback = callback;
    }

    @Override
    public View getView() {
        return dtbAdView;
    }

    public void loadAd() {
        Context context = adConfiguration.getContext();
        String placementId = null;
        int width = 320;
        int height = 50;
        String appId = null;

        try {
            String serverParameter = adConfiguration.getServerParameters().getString("parameter");
            Log.i(CustomAdEvent.domain, "loaded config for banner:" + serverParameter);
            JSONObject json = new JSONObject(serverParameter);
            placementId = json.getString(CustomAdEvent.KEY_PLACEMENT_ID);
            appId = json.optString("appId");
            width = json.has("width") ? json.optInt("width", 320) : 320;
            height = json.has("height") ? json.optInt("height", 50) : 50;
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
        dTBAdRequest.setSizes(new DTBAdSize(width, height, placementId));
        dTBAdRequest.loadAd(this);
        Log.i(CustomAdEvent.domain, "start load aps banner");
    }

    @Override
    public void onFailure(com.amazon.device.ads.AdError adError) {
        Log.i(CustomAdEvent.domain, "aps banner load failed:" + adError.getMessage());
        callback.onFailure(new AdError(adError.getCode().ordinal(), adError.getMessage(), CustomAdEvent.domain));
    }

    @Override
    public void onSuccess(DTBAdResponse dtbAdResponse) {
        try {
            Bundle bidInfo = dtbAdResponse.getRenderingBundle();
            dtbAdView = new DTBAdView(adConfiguration.getContext(), this);
            if (dtbAdView != null) {
                Log.i(CustomAdEvent.domain, "aps banner load success, start bidding...");
                dtbAdView.fetchAd(bidInfo);
            } else {
                Log.i(CustomAdEvent.domain, "aps banner load success, but ad view is null");
            }
        } catch (Exception e) {
            Log.i(CustomAdEvent.domain, "aps banner load response success, but render failed");
            callback.onFailure(new AdError(0, "render failed", CustomAdEvent.domain));
        }
    }

    @Override
    public void onAdLoaded(View view) {
        Log.i(CustomAdEvent.domain, "aps banner load success finally");
        mediationBannerAdCallback = callback.onSuccess(this);
    }

    @Override
    public void onAdFailed(View view) {
        Log.i(CustomAdEvent.domain, "aps banner load failed finally");
        callback.onFailure(new AdError(CustomAdEvent.ERROR_PLACEMENT_STATE_NO_FILL, "no fill", CustomAdEvent.domain));
    }

    @Override
    public void onAdClicked(View view) {
        if (mediationBannerAdCallback != null) {
            mediationBannerAdCallback.reportAdClicked();
        }
    }

    @Override
    public void onAdLeftApplication(View view) {
        if (mediationBannerAdCallback != null) {
            mediationBannerAdCallback.onAdLeftApplication();
        }
    }

    @Override
    public void onAdOpen(View view) {
        if (mediationBannerAdCallback != null) {
            mediationBannerAdCallback.onAdOpened();
        }
    }

    @Override
    public void onAdClosed(View view) {
        if (mediationBannerAdCallback != null) {
            mediationBannerAdCallback.onAdClosed();
        }
    }

    @Override
    public void onImpressionFired(View view) {
        Log.i(CustomAdEvent.domain, "aps banner impression");
        if (mediationBannerAdCallback != null) {
            mediationBannerAdCallback.reportAdImpression();
        }
    }
}