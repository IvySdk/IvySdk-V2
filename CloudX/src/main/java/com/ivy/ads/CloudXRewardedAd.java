package com.ivy.ads;

import androidx.annotation.NonNull;

import io.cloudx.sdk.CloudX;
import io.cloudx.sdk.CloudXAd;
import io.cloudx.sdk.CloudXError;
import io.cloudx.sdk.CloudXReward;
import io.cloudx.sdk.CloudXRewardedListener;

public class CloudXRewardedAd extends CloudXBaseAd {

    private io.cloudx.sdk.CloudXRewardedAd rewardedAd;
    private boolean wasRewarded = false;

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
        wasRewarded = false;
        rewardedAd = CloudX.createRewarded(adUnit);
        rewardedAd.setListener(listener);
        rewardedAd.setRevenueListener(this);
        rewardedAd.load();
    }

    public boolean isReady() {
        return !isShowing && (rewardedAd != null && rewardedAd.isAdReady());
    }

    @Override
    public void show(AdShowCallback callback) {
        if (!isReady()) {
//            if (callback != null){
//                callback.onAdShowFailed(CloudXRewardedAd.this, 400, "AD_NOT_READY");
//            }
            return;
        }
        this.showCallback = callback;
        // 重置奖励状态
        wasRewarded = false;
        isShowing = true;
        rewardedAd.show(context);
    }

    private CloudXRewardedListener listener = new CloudXRewardedListener() {

        @Override
        public void onAdClicked(@NonNull CloudXAd cloudXAd) {
            if (showCallback != null) {
                showCallback.onAdClicked(CloudXRewardedAd.this);
            }
        }

        @Override
        public void onAdLoadFailed(@NonNull String s, @NonNull CloudXError cloudXError) {
            isLoading = false;
            price = 0.0;

            if (loadCallback != null) {
                loadCallback.onLoadFailed(CloudXRewardedAd.this, cloudXError.getCode().getCode(), cloudXError.getMessage());
            }
        }

        @Override
        public void onAdLoaded(@NonNull CloudXAd cloudXAd) {
            isLoading = false;
            price = cloudXAd.getRevenue();
            networkName = cloudXAd.getNetworkName();

            if (loadCallback != null) {
                loadCallback.onLoadSuccess(CloudXRewardedAd.this);
            }
        }

        @Override
        public void onAdHidden(@NonNull CloudXAd cloudXAd) {
            isShowing = false;
            if (showCallback != null) {
                showCallback.onAdClosed(CloudXRewardedAd.this, wasRewarded);
            }
        }

        @Override
        public void onAdDisplayFailed(@NonNull CloudXAd cloudXAd, @NonNull CloudXError cloudXError) {
            isShowing = false;
            if (showCallback != null) {
                showCallback.onAdShowFailed(CloudXRewardedAd.this, cloudXError.getCode().getCode(), cloudXError.getMessage());
            }
        }

        @Override
        public void onAdDisplayed(@NonNull CloudXAd cloudXAd) {
            if (showCallback != null) {
                showCallback.onAdDisplayed(CloudXRewardedAd.this);
            }
        }

        @Override
        public void onUserRewarded(@NonNull CloudXAd cloudXAd, @NonNull CloudXReward cloudXReward) {
            wasRewarded = true;
        }
    };

}
