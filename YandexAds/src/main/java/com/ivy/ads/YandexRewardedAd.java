package com.ivy.ads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;

/**
 * AdMob 激励视频广告
 */
public class YandexRewardedAd extends YandexBaseAd {
    private RewardedAd rewardedAd = null;
    protected boolean isRewarded = false;

    private final RewardedAdEventListener adEventListener = new RewardedAdEventListener() {
        @Override
        public void onAdShown() {
            if (showCallback != null) {
                showCallback.onAdDisplayed(YandexRewardedAd.this);
            }
        }

        @Override
        public void onAdFailedToShow(@NonNull AdError adError) {
            rewardedAd = null;
            isShowing = false;
            // Called when ad fails to show.
            if (showCallback != null) {
                showCallback.onAdShowFailed(YandexRewardedAd.this, -1, adError.getDescription());
            }
        }

        @Override
        public void onAdDismissed() {
            // AdMob ads are one-time use. Nullify the reference.
            rewardedAd = null;
            isShowing = false;
            // Called when ad is dismissed.
            if (showCallback != null) {
                showCallback.onAdClosed(YandexRewardedAd.this, isRewarded);
            }
        }

        @Override
        public void onAdClicked() {
            if (showCallback != null) {
                showCallback.onAdClicked(YandexRewardedAd.this);
            }
        }

        @Override
        public void onAdImpression(@Nullable ImpressionData impressionData) {
            YandexRewardedAd.this.onPaidEvent(impressionData);
        }

        @Override
        public void onRewarded(@NonNull Reward reward) {
            isRewarded = true;
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
        isShowing = true;
        isRewarded = false;

        rewardedAd.show(context);
    }

    /**
     * 检查广告是否加载成功并可以展示
     */
    public boolean isReady() {
        return !isShowing && (rewardedAd != null && !isLoading);
    }

    @Override
    protected void loadAd() {
        RewardedAdLoader adLoader = new RewardedAdLoader(context);
        adLoader.loadAd(new AdRequest.Builder(adUnit).build(), new RewardedAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                isLoading = false;
                rewardedAd.setAdEventListener(adEventListener);
                YandexRewardedAd.this.rewardedAd = rewardedAd;
                loadCallback.onLoadSuccess(YandexRewardedAd.this);
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError loadAdError) {
                isLoading = false;
                rewardedAd = null; // Ensure ad is null on failure
                if (loadCallback != null) {
                    loadCallback.onLoadFailed(YandexRewardedAd.this, loadAdError.getCode(), loadAdError.getDescription());
                }
            }
        });
    }

    /**
     * 销毁广告实例，释放资源
     */
    public void destroy() {
        // AdMob RewardedAd doesn't have a specific destroy method.
        // Setting to null removes the reference, allowing garbage collection.
        if (rewardedAd != null) {
            rewardedAd.setAdEventListener(null);
            rewardedAd = null;
        }

        loadCallback = null;
        showCallback = null;
        revenueCallback = null;
        isLoading = false;
        isRewarded = false;
    }
}