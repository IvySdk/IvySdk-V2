package com.ivy.ads;

import androidx.annotation.NonNull;

import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration;
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader;

/**
 * AdMob 插屏广告
 */
public class AdmobInterstitialAd extends AdmobBaseAd {
    private InterstitialAd interstitialAd;

    private void fetchNetworkName(InterstitialAd ad){
        try {
            // Set paid event listener right after loading
            networkName = ad.getResponseInfo().getLoadedAdSourceResponseInfo() != null ?
                    ad.getResponseInfo().getLoadedAdSourceResponseInfo().getName() : "admob";
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private final InterstitialAdEventCallback fullScreenContentCallback = new InterstitialAdEventCallback() {
        @Override
        public void onAdShowedFullScreenContent() {
            // Called when ad is shown.
            if (showCallback != null) {
                showCallback.onAdDisplayed(AdmobInterstitialAd.this);
            }
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {
            // Called when ad fails to show.
            isShowing = false;
            interstitialAd = null; // Clean up reference
            if (showCallback != null) {
                showCallback.onAdShowFailed(AdmobInterstitialAd.this, fullScreenContentError.getCode().getValue(), fullScreenContentError.getMessage());
            }
        }

        @Override
        public void onAdPaid(@NonNull AdValue value) {
            AdmobInterstitialAd.this.onPaidEvent(value);
        }

        @Override
        public void onAdDismissedFullScreenContent() {
            isShowing = false;
            // AdMob ads are one-time use. Nullify the reference.
            // A new ad needs to be loaded.
            interstitialAd = null;
            // Called when ad is dismissed.
            // Interstitial ads don't have a rewarded state.
            if (showCallback != null) {
                showCallback.onAdClosed(AdmobInterstitialAd.this, false);
            }
        }

        @Override
        public void onAdClicked() {
            // Called when a click is recorded for an ad.
            if (showCallback != null) {
                showCallback.onAdClicked(AdmobInterstitialAd.this);
            }
        }

        @Override
        public void onAdImpression() {
            // Called when an impression is recorded for an ad.
            // Revenue is handled by onPaidEventListener
        }
    };

    private void onLoadFailed(LoadAdError loadAdError){
        isLoading = false;
        interstitialAd = null; // Ensure ad is null on failure
        if (loadCallback != null) {
            loadCallback.onLoadFailed(AdmobInterstitialAd.this, loadAdError.getCode().getValue(), loadAdError.getMessage());
        }
    }

    private void onLoadSuccess(InterstitialAd ad){
        isLoading = false;
        interstitialAd = ad;
        interstitialAd.setAdEventCallback(fullScreenContentCallback);
        fetchNetworkName(ad);

        if (loadCallback != null) {
            loadCallback.onLoadSuccess(AdmobInterstitialAd.this);
        }
    }

    @Override
    protected void loadAd() {
        if (isPreload){
            InterstitialAdPreloader.start(adUnit, new PreloadConfiguration(new AdRequest.Builder(adUnit).build(), preloadCount), new PreloadCallback() {
                @Override
                public void onAdPreloaded(
                        @NonNull String preloadId, ResponseInfo responseInfo) {
                    // Called when preloaded ads are available.
                    isLoading = false;
                    if (loadCallback != null) {
                        loadCallback.onLoadSuccess(AdmobInterstitialAd.this);
                    }
                }

                @Override
                public void onAdsExhausted(@NonNull String preloadId) {
                    // Called when no preloaded ads are available.
                    isLoading = false;
                    interstitialAd = null;
                    if (loadCallback != null) {
                        loadCallback.onLoadFailed(AdmobInterstitialAd.this, -2, "exhausted");
                    }
                }

                @Override
                public void onAdFailedToPreload(@NonNull String preloadId, @NonNull LoadAdError adError) {
                    // Called when preloaded ads failed to load.
                    isLoading = false;
                    interstitialAd = null;
                    if (loadCallback != null) {
                        loadCallback.onLoadFailed(AdmobInterstitialAd.this, adError.getCode().getValue(), adError.getMessage());
                    }
                }
            });
        } else {
            InterstitialAd.load(new AdRequest.Builder(adUnit).build(), new com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<>() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    onLoadFailed(loadAdError);
                }

                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    onLoadSuccess(ad);
                }
            });
        }
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

        if (isPreload){
            interstitialAd = InterstitialAdPreloader.pollAd(adUnit);
            if (interstitialAd != null) {
                fetchNetworkName(interstitialAd);
                interstitialAd.setAdEventCallback(fullScreenContentCallback);
            }
        }

        if (interstitialAd != null) {
            isShowing = true;
            interstitialAd.show(context);
        }
    }

    /**
     * 检查广告是否加载成功并可以展示
     * @return 是否可以展示广告
     */
    public boolean isReady() {
        return !isShowing && ((interstitialAd != null && !isLoading) || (isPreload && InterstitialAdPreloader.isAdAvailable(adUnit)));
    }

    /**
     * 销毁广告实例，释放资源
     */
    public void destroy() {
        if (interstitialAd != null) {
            interstitialAd.setAdEventCallback(null);
            interstitialAd.destroy();
            interstitialAd = null;
        }

        if (isPreload){
            InterstitialAdPreloader.destroy(adUnit);
        }

        loadCallback = null;
        showCallback = null;
        revenueCallback = null;
        isLoading = false; // Reset loading state
    }
}