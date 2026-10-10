package com.ivy.ads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration;
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo;
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader;

/**
 * AdMob 激励视频广告
 */
public class AdmobRewardedAd extends AdmobBaseAd implements OnUserEarnedRewardListener {
    private RewardedAd rewardedAd = null;
    protected boolean isRewarded = false;

    private void fetchNetworkName(RewardedAd ad){
        try {
            // Set paid event listener right after loading
            networkName = ad.getResponseInfo().getLoadedAdSourceResponseInfo() != null ?
                    ad.getResponseInfo().getLoadedAdSourceResponseInfo().getName() : "admob";
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    RewardedAdEventCallback fullScreenContentCallback = new RewardedAdEventCallback() {
        @Override
        public void onAdPaid(@NonNull AdValue value) {
            onPaidEvent(value);
        }

        @Override
        public void onAdShowedFullScreenContent() {
            // Called when ad is shown.
            if (showCallback != null) {
                showCallback.onAdDisplayed(AdmobRewardedAd.this);
            }
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError adError) {
            rewardedAd = null;
            isShowing = false;
            // Called when ad fails to show.
            if (showCallback != null) {
                showCallback.onAdShowFailed(AdmobRewardedAd.this, adError.getCode().getValue(), adError.getMessage());
            }
        }

        @Override
        public void onAdDismissedFullScreenContent() {
            // AdMob ads are one-time use. Nullify the reference.
            rewardedAd = null;
            isShowing = false;
            // Called when ad is dismissed.
            if (showCallback != null) {
                showCallback.onAdClosed(AdmobRewardedAd.this, isRewarded);
            }
        }

        @Override
        public void onAdClicked() {
            // Called when a click is recorded for an ad.
            if (showCallback != null) {
                showCallback.onAdClicked(AdmobRewardedAd.this);
            }
        }

        @Override
        public void onAdImpression() {
            // Called when an impression is recorded for an ad.
            // Revenue is handled by onPaidEventListener
        }
    };

    /**
     * 展示激励视频广告
     */
    @Override
    public void show(AdShowCallback callback) {
        if (!isReady()) {
            callback.onAdShowFailed(this, -1, "Ad not ready");
            return;
        }
        
        this.showCallback = callback;

        if (isPreload){
            rewardedAd = RewardedAdPreloader.pollAd(adUnit);
            if (rewardedAd != null){
                fetchNetworkName(rewardedAd);
                rewardedAd.setAdEventCallback(fullScreenContentCallback);
            }
        }

        if (rewardedAd != null){
            isShowing = true;
            isRewarded = false;
            rewardedAd.show(context, this);
        }
    }

    @Override
    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
        // User earned reward
        isRewarded = true;
    }

    /**
     * 检查广告是否加载成功并可以展示
     */
    public boolean isReady() {
        return !isShowing && ((rewardedAd != null && !isLoading) || (isPreload && RewardedAdPreloader.isAdAvailable(adUnit)));
    }

    @Override
    protected void loadAd() {
        if (isPreload){
            RewardedAdPreloader.start(adUnit, new PreloadConfiguration(new AdRequest.Builder(adUnit).build(), preloadCount), new PreloadCallback() {
                @Override
                public void onAdsExhausted(@NonNull String s) {
                    isLoading = false;
                    rewardedAd = null; // Ensure ad is null on failure
                    if (loadCallback != null) {
                        loadCallback.onLoadFailed(AdmobRewardedAd.this, -2, "exhaust");
                    }
                }

                @Override
                public void onAdPreloaded(@NonNull String s, @Nullable ResponseInfo responseInfo) {
                    isLoading = false;
                    try {
                        // Update AdConfig with initial info
                        networkName = responseInfo.getLoadedAdSourceResponseInfo().getName();
                    } catch (Exception e) {
                        e.printStackTrace();
                        networkName = "admob";
                    }
                    if (loadCallback != null) {
                        loadCallback.onLoadSuccess(AdmobRewardedAd.this);
                    }
                }

                @Override
                public void onAdFailedToPreload(@NonNull String s, @NonNull LoadAdError adError) {
                    isLoading = false;
                    rewardedAd = null; // Ensure ad is null on failure
                    if (loadCallback != null) {
                        loadCallback.onLoadFailed(AdmobRewardedAd.this, adError.getCode().getValue(), adError.getMessage());
                    }
                }
            });
        } else {
            RewardedAd.load(new AdRequest.Builder(adUnit).build(), new com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<>() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    isLoading = false;
                    rewardedAd = null; // Ensure ad is null on failure
                    if (loadCallback != null) {
                        loadCallback.onLoadFailed(AdmobRewardedAd.this, loadAdError.getCode().getValue(), loadAdError.getMessage());
                    }
                }

                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    isLoading = false;
                    rewardedAd = ad;
                    // Set paid event listener right after loading
                    rewardedAd.setAdEventCallback(fullScreenContentCallback);
                    fetchNetworkName(ad);

                    if (loadCallback != null) {
                        loadCallback.onLoadSuccess(AdmobRewardedAd.this);
                    }
                }
            });
        }
    }

    /**
     * 销毁广告实例，释放资源
     */
    public void destroy() {
        if (isPreload){
            RewardedAdPreloader.destroy(adUnit);
            rewardedAd = null;
        } else {
            if (rewardedAd != null) {
                rewardedAd.setAdEventCallback(null);
                rewardedAd.destroy();
                rewardedAd = null;
            }
        }

        loadCallback = null;
        showCallback = null;
        revenueCallback = null;
        isLoading = false;
        isRewarded = false;
    }
}