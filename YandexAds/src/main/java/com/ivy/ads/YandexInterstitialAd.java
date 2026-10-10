package com.ivy.ads;

import androidx.annotation.NonNull;

import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;

/**
 * AdMob 插屏广告
 */
public class YandexInterstitialAd extends YandexBaseAd {
    private InterstitialAd interstitialAd;

    private final InterstitialAdEventListener adEventListener = new InterstitialAdEventListener() {
        @Override
        public void onAdShown() {
            if (showCallback != null) {
                showCallback.onAdDisplayed(YandexInterstitialAd.this);
            }
        }

        @Override
        public void onAdFailedToShow(AdError adError) {
            isShowing = false;
            interstitialAd = null; // Clean up reference
            // Called when ad fails to show.
            if (showCallback != null) {
                showCallback.onAdShowFailed(YandexInterstitialAd.this, -1, adError.getDescription());
            }
        }

        @Override
        public void onAdDismissed() {
            isShowing = false;
            // AdMob ads are one-time use. Nullify the reference.
            // A new ad needs to be loaded.
            interstitialAd = null;
            // Called when ad is dismissed.
            // Interstitial ads don't have a rewarded state.
            if (showCallback != null) {
                showCallback.onAdClosed(YandexInterstitialAd.this, false);
            }
        }

        @Override
        public void onAdClicked() {
            if (showCallback != null) {
                showCallback.onAdClicked(YandexInterstitialAd.this);
            }
        }

        @Override
        public void onAdImpression(ImpressionData impressionData) {
            YandexInterstitialAd.this.onPaidEvent(impressionData);
        }
    };
    private void onLoadFailed(AdRequestError loadAdError){
        isLoading = false;
        interstitialAd = null; // Ensure ad is null on failure
        if (loadCallback != null) {
            loadCallback.onLoadFailed(this, loadAdError.getCode(), loadAdError.getDescription());
        }
    }

    private void onLoadSuccess(InterstitialAd ad){
        isLoading = false;
        interstitialAd = ad;
        interstitialAd.setAdEventListener(adEventListener);

        if (loadCallback != null) {
            loadCallback.onLoadSuccess(this);
        }
    }

    @Override
    protected void loadAd() {
        InterstitialAdLoader adLoader = new InterstitialAdLoader(context);
        adLoader.loadAd(new AdRequest.Builder(adUnit).build(), new InterstitialAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                onLoadSuccess(interstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                onLoadFailed(adRequestError);
            }
        });
    }

    /**
     * 展示插屏广告
     * @param callback 展示回调
     * @return 是否成功调用展示 (AdMob show is async, success means attempted to show)
     */
    @Override
    public void show(AdShowCallback callback) {
        if (!isReady()) {
            callback.onAdShowFailed(this, -1, "Ad not ready"); // Use a custom error code/message
            return;
        }

        this.showCallback = callback;
        isShowing = true;

        if (interstitialAd != null) {
            interstitialAd.show(context);
        }
    }

    /**
     * 检查广告是否加载成功并可以展示
     * @return 是否可以展示广告
     */
    public boolean isReady() {
        return !isShowing && (interstitialAd != null && !isLoading);
    }

    /**
     * 销毁广告实例，释放资源
     */
    public void destroy() {
        if (interstitialAd != null) {
            interstitialAd.setAdEventListener(null);
            interstitialAd = null;
        }

        loadCallback = null;
        showCallback = null;
        revenueCallback = null;
        isLoading = false; // Reset loading state
    }
}