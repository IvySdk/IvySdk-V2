package com.ivy.ads;

import com.zeus.gmc.sdk.mobileads.columbus.ad.AdError;
import com.zeus.gmc.sdk.mobileads.columbus.ad.interstitialad.InterstitialAd;
import com.zeus.gmc.sdk.mobileads.columbus.ad.interstitialad.InterstitialAdListener;

public class XiaomiInterstitialAd extends XiaomiBaseAd {
    private InterstitialAd ad;

    private final InterstitialAdListener listener = new InterstitialAdListener() {
        @Override
        public void onAdLoaded() {
            isLoading = false;
            if (loadCallback != null) {
                loadCallback.onLoadSuccess(XiaomiInterstitialAd.this);
            }
        }

        @Override
        public void onAdError(AdError error) {
            boolean wasShowing = isShowing;
            isLoading = false;
            isShowing = false;
            destroyAd();
            if (wasShowing) {
                if (showCallback != null) {
                    showCallback.onAdShowFailed(XiaomiInterstitialAd.this,
                            error.getErrorCode(), error.getErrorMessage());
                }
            } else if (loadCallback != null) {
                loadCallback.onLoadFailed(XiaomiInterstitialAd.this,
                        error.getErrorCode(), error.getErrorMessage());
            }
        }

        @Override
        public void onLoggingImpression() {
            if (showCallback != null) {
                showCallback.onAdDisplayed(XiaomiInterstitialAd.this);
            }
        }

        @Override
        public void onAdClicked() {
            if (showCallback != null) {
                showCallback.onAdClicked(XiaomiInterstitialAd.this);
            }
        }

        @Override
        public void onAdClosed() {
            isShowing = false;
            destroyAd();
            if (showCallback != null) {
                showCallback.onAdClosed(XiaomiInterstitialAd.this, false);
            }
        }
    };

    @Override
    protected void loadAd() {
        destroyAd();
        try {
            ad = new InterstitialAd(context, adUnit);
            ad.setAdEventListener(listener);
            ad.loadAd();
        } catch (Throwable throwable) {
            isLoading = false;
            destroyAd();
            if (loadCallback != null) {
                loadCallback.onLoadFailed(this, -1, throwable.getMessage());
            }
        }
    }

    @Override
    public boolean isReady() {
        return !isLoading && !isShowing && ad != null && ad.isAdLoaded();
    }

    @Override
    public void show(AdShowCallback callback) {
        if (!isReady()) {
            callback.onAdShowFailed(this, -1, "Ad not ready");
            return;
        }
        showCallback = callback;
        isShowing = true;
        try {
            ad.registerViewForInteraction();
        } catch (Throwable throwable) {
            isShowing = false;
            destroyAd();
            callback.onAdShowFailed(this, -1, throwable.getMessage());
        }
    }

    @Override
    public void destroy() {
        destroyAd();
        isLoading = false;
        isShowing = false;
        loadCallback = null;
        showCallback = null;
        revenueCallback = null;
    }

    private void destroyAd() {
        if (ad != null) {
            try {
                ad.destroy();
            } catch (Throwable ignored) {
            }
            ad = null;
        }
    }
}
