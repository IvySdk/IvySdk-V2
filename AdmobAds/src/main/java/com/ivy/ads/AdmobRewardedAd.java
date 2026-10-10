package com.ivy.ads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.preload.PreloadCallbackV2;
import com.google.android.gms.ads.preload.PreloadConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAdPreloader;

/**
 * AdMob 激励视频广告
 */
public class AdmobRewardedAd extends AdmobBaseAd implements OnUserEarnedRewardListener {
    private RewardedAd rewardedAd = null;
    protected boolean isRewarded = false;

    private void fetchNetworkName(RewardedAd ad){
        try {
            // Set paid event listener right after loading
            networkName = ad.getResponseInfo().getLoadedAdapterResponseInfo() != null ?
                    ad.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName() : "admob";
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
        @Override
        public void onAdShowedFullScreenContent() {
            // Called when ad is shown.
            if (showCallback != null) {
                showCallback.onAdDisplayed(AdmobRewardedAd.this);
            }
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
            rewardedAd = null;
            isShowing = false;
            // Called when ad fails to show.
            if (showCallback != null) {
                showCallback.onAdShowFailed(AdmobRewardedAd.this, adError.getCode(), adError.getMessage());
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
                rewardedAd.setFullScreenContentCallback(fullScreenContentCallback);
                rewardedAd.setOnPaidEventListener(this);
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
            RewardedAdPreloader.start(adUnit, new PreloadConfiguration.Builder(adUnit).setBufferSize(preloadCount).build(), new PreloadCallbackV2() {
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
                        networkName = responseInfo.getLoadedAdapterResponseInfo().getAdSourceName();
                    } catch (Exception e) {
                        e.printStackTrace();
                        networkName = "admob";
                    }
                    if (loadCallback != null) {
                        loadCallback.onLoadSuccess(AdmobRewardedAd.this);
                    }
                }

                @Override
                public void onAdFailedToPreload(@NonNull String s, @NonNull AdError adError) {
                    isLoading = false;
                    rewardedAd = null; // Ensure ad is null on failure
                    if (loadCallback != null) {
                        loadCallback.onLoadFailed(AdmobRewardedAd.this, adError.getCode(), adError.getMessage());
                    }
                }
            });
        } else {
            RewardedAd.load(context, adUnit, isAdmanager ? new AdManagerAdRequest.Builder().build() : new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    isLoading = false;
                    rewardedAd = null; // Ensure ad is null on failure
                    if (loadCallback != null) {
                        loadCallback.onLoadFailed(AdmobRewardedAd.this, loadAdError.getCode(), loadAdError.getMessage());
                    }
                }

                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    isLoading = false;
                    rewardedAd = ad;
                    // Set paid event listener right after loading
                    rewardedAd.setOnPaidEventListener(AdmobRewardedAd.this);
                    rewardedAd.setFullScreenContentCallback(fullScreenContentCallback);
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
        // AdMob RewardedAd doesn't have a specific destroy method.
        // Setting to null removes the reference, allowing garbage collection.
        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(null);
            rewardedAd = null;
        }

        loadCallback = null;
        showCallback = null;
        revenueCallback = null;
        isLoading = false;
        isRewarded = false;
    }
}