package com.ivy.ads;

import androidx.annotation.NonNull;

import io.cloudx.sdk.CloudX;
import io.cloudx.sdk.CloudXAd;
import io.cloudx.sdk.CloudXError;
import io.cloudx.sdk.CloudXInterstitialListener;

public class CloudXInterstitialAd extends CloudXBaseAd {

    private io.cloudx.sdk.CloudXInterstitialAd interstitialAd;

    @Override
    protected void onInited() {

    }

    @Override
    protected void loadAd() {
        if (!CloudX.isInitialized()) {
            onLoadFailed(200, "NOT_INITIALIZED");
            return;
        }
        price = 0.0;
        isLoading = true;
        interstitialAd = CloudX.createInterstitial(adUnit);
        interstitialAd.setListener(listener);
        interstitialAd.setRevenueListener(this);
        interstitialAd.load();
    }

    public boolean isReady() {
        return !isShowing && (interstitialAd != null && interstitialAd.isAdReady());
    }

    @Override
    public void show(AdShowCallback callback) {
        if (!isReady()){
//            if (callback != null){
//                callback.onAdShowFailed(CloudXInterstitialAd.this, 400, "AD_NOT_READY");
//            }
            return;
        }
        this.showCallback = callback;
        isShowing = true;
        interstitialAd.show(context);
    }

    public void destroy() {
        if (interstitialAd != null) {
            interstitialAd.destroy();
        }
        isShowing = false;
        interstitialAd = null;
        loadCallback = null;
        showCallback = null;
        revenueCallback = null;
    }

    private CloudXInterstitialListener listener = new CloudXInterstitialListener() {
        @Override
        public void onAdDisplayed(@NonNull CloudXAd cloudXAd) {
            if (showCallback != null) {
                showCallback.onAdDisplayed(CloudXInterstitialAd.this);
            }
        }

        @Override
        public void onAdDisplayFailed(@NonNull CloudXAd cloudXAd, @NonNull CloudXError cloudXError) {
            isShowing = false;
            interstitialAd = null;
            if (showCallback != null) {
                showCallback.onAdShowFailed(CloudXInterstitialAd.this, cloudXError.getCode().getCode(), cloudXError.getMessage());
            }
        }

        @Override
        public void onAdHidden(@NonNull CloudXAd cloudXAd) {
            isShowing = false;
            interstitialAd = null;
            if (showCallback != null) {
                showCallback.onAdClosed(CloudXInterstitialAd.this, false);
            }
        }

        @Override
        public void onAdLoaded(@NonNull CloudXAd cloudXAd) {
            price = cloudXAd.getRevenue();
            networkName = cloudXAd.getNetworkName();
            isLoading = false;
            if (loadCallback != null) {
                loadCallback.onLoadSuccess(CloudXInterstitialAd.this);
            }
        }

        @Override
        public void onAdLoadFailed(@NonNull String s, @NonNull CloudXError cloudXError) {
            isLoading = false;
            if (loadCallback != null) {
                loadCallback.onLoadFailed(CloudXInterstitialAd.this, cloudXError.getCode().getCode(), cloudXError.getMessage());
            }
        }

        @Override
        public void onAdClicked(@NonNull CloudXAd cloudXAd) {
            if (showCallback != null) {
                showCallback.onAdClicked(CloudXInterstitialAd.this);
            }
        }
    };


}
