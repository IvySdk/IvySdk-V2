package com.ivy.ads;

import android.view.View;

import androidx.annotation.NonNull;

import io.cloudx.sdk.CloudX;
import io.cloudx.sdk.CloudXAd;
import io.cloudx.sdk.CloudXAdView;
import io.cloudx.sdk.CloudXAdViewListener;
import io.cloudx.sdk.CloudXError;

public class CloudXBannerAd extends CloudXBaseAd {

    private CloudXAdView bannerAd;
    private boolean isLoaded = false;

    private boolean isShown = false;

    @Override
    protected void onInited() {
        bannerAd = CloudX.createBanner(adUnit);
        bannerAd.setListener(listener);
        bannerAd.setRevenueListener(this);
        bannerAd.stopAutoRefresh();
    }

    @Override
    protected void loadAd() {
        isLoaded = false;
        isShown = false;
        bannerAd.load();
    }

    @Override
    public boolean isReady() {
        return isLoaded && !isShown;
    }

    @Override
    public View getView() {
        return bannerAd;
    }

    @Override
    public void show(AdShowCallback callback) {
        isShown = true;
        showCallback = callback;
//        adView.setVisibility(View.VISIBLE);
        if (getRefreshSeconds() == 0) {
            bannerAd.startAutoRefresh();
        }
    }

    public void close() {
        isShown = false;
//        adView.setVisibility(View.GONE);
        bannerAd.stopAutoRefresh();
    }

    private CloudXAdViewListener listener = new CloudXAdViewListener() {
        @Override
        public void onAdExpanded(@NonNull CloudXAd cloudXAd) {

        }

        @Override
        public void onAdCollapsed(@NonNull CloudXAd cloudXAd) {

        }

        @Override
        public void onAdLoaded(@NonNull CloudXAd cloudXAd) {
            isLoaded = true;
            price = cloudXAd.getRevenue();
            networkName = cloudXAd.getNetworkName();
            if (loadCallback != null) {
                loadCallback.onLoadSuccess(CloudXBannerAd.this);
            }
        }

        @Override
        public void onAdLoadFailed(@NonNull String s, @NonNull CloudXError cloudXError) {
            isLoading = false;
            price = 0.0;
            if (loadCallback != null) {
                loadCallback.onLoadFailed(CloudXBannerAd.this, cloudXError.getCode().getCode(), cloudXError.getMessage());
            }
        }

        @Override
        public void onAdClicked(@NonNull CloudXAd cloudXAd) {
            if (showCallback != null) {
                showCallback.onAdClicked(CloudXBannerAd.this);
            }
        }
    };

}
