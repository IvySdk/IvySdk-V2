package com.ivy.ads;

import com.zeus.gmc.sdk.mobileads.columbus.ad.AdError;
import com.zeus.gmc.sdk.mobileads.columbus.ad.videoads.RewardedVideoAd;
import com.zeus.gmc.sdk.mobileads.columbus.ad.videoads.RewardedVideoAdListener;

public class XiaomiRewardedAd extends XiaomiBaseAd {
    private RewardedVideoAd ad;
    private boolean rewarded;

    private final RewardedVideoAdListener listener = new RewardedVideoAdListener() {
        @Override
        public void onAdLoaded() {
            isLoading = false;
            if (loadCallback != null) {
                loadCallback.onLoadSuccess(XiaomiRewardedAd.this);
            }
        }

        @Override
        public void onError(AdError error) {
            boolean wasShowing = isShowing;
            isLoading = false;
            isShowing = false;
            destroyAd();
            if (wasShowing) {
                if (showCallback != null) {
                    showCallback.onAdShowFailed(XiaomiRewardedAd.this,
                            error.getErrorCode(), error.getErrorMessage());
                }
            } else if (loadCallback != null) {
                loadCallback.onLoadFailed(XiaomiRewardedAd.this,
                        error.getErrorCode(), error.getErrorMessage());
            }
        }

        @Override
        public void onLoggingImpression() {
            if (showCallback != null) {
                showCallback.onAdDisplayed(XiaomiRewardedAd.this);
            }
        }

        @Override
        public void onRewardedVideoCompleted() {
            rewarded = true;
        }

        @Override
        public void onAdClicked() {
            if (showCallback != null) {
                showCallback.onAdClicked(XiaomiRewardedAd.this);
            }
        }

        @Override
        public void onAdClosed() {
            isShowing = false;
            destroyAd();
            if (showCallback != null) {
                showCallback.onAdClosed(XiaomiRewardedAd.this, rewarded);
            }
        }
    };

    @Override
    protected void loadAd() {
        destroyAd();
        rewarded = false;
        try {
            ad = new RewardedVideoAd(context, adUnit);
            ad.setAdListener(listener);
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
        rewarded = false;
        try {
            ad.show();
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
        rewarded = false;
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
