package com.ivy.ads;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.interstitial.InterstitialAdPreloader;
import com.google.android.gms.ads.preload.PreloadCallbackV2;
import com.google.android.gms.ads.preload.PreloadConfiguration;

/**
 * AdMob 插屏广告
 */
public class AdmobInterstitialAd extends AdmobBaseAd {
    private InterstitialAd interstitialAd;

    private void fetchNetworkName(InterstitialAd ad){
        try {
            // Set paid event listener right after loading
            networkName = ad.getResponseInfo().getLoadedAdapterResponseInfo() != null ?
                    ad.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName() : "admob";
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private final FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
        @Override
        public void onAdShowedFullScreenContent() {
            // Called when ad is shown.
            if (showCallback != null) {
                showCallback.onAdDisplayed(AdmobInterstitialAd.this);
            }
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
            isShowing = false;
            interstitialAd = null; // Clean up reference
            // Called when ad fails to show.
            if (showCallback != null) {
                showCallback.onAdShowFailed(AdmobInterstitialAd.this, adError.getCode(), adError.getMessage());
            }
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
            loadCallback.onLoadFailed(AdmobInterstitialAd.this, loadAdError.getCode(), loadAdError.getMessage());
        }
    }

    private void onLoadSuccess(InterstitialAd ad){
        isLoading = false;
        interstitialAd = ad;
        interstitialAd.setOnPaidEventListener(AdmobInterstitialAd.this);
        interstitialAd.setFullScreenContentCallback(fullScreenContentCallback);
        fetchNetworkName(ad);

        if (loadCallback != null) {
            loadCallback.onLoadSuccess(AdmobInterstitialAd.this);
        }
    }

    @Override
    protected void loadAd() {
        if (isAdmanager) {
            AdManagerInterstitialAd.load(context, adUnit, new AdManagerAdRequest.Builder().build(), new AdManagerInterstitialAdLoadCallback() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    onLoadFailed(loadAdError);
                }

                @Override
                public void onAdLoaded(@NonNull AdManagerInterstitialAd adManagerInterstitialAd) {
                    onLoadSuccess(adManagerInterstitialAd);
                }
            });
        } else if (isPreload){
            InterstitialAdPreloader.start(adUnit, new PreloadConfiguration.Builder(adUnit).setBufferSize(preloadCount).build(), new PreloadCallbackV2() {
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
                        public void onAdFailedToPreload(@NonNull String preloadId, @NonNull AdError adError) {
                            // Called when preloaded ads failed to load.
                            isLoading = false;
                            interstitialAd = null;
                            if (loadCallback != null) {
                                loadCallback.onLoadFailed(AdmobInterstitialAd.this, adError.getCode(), adError.getMessage());
                            }
                        }
                    });
        } else {
            InterstitialAd.load(context, adUnit, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
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
                interstitialAd.setFullScreenContentCallback(fullScreenContentCallback);
                interstitialAd.setOnPaidEventListener(this);
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
            interstitialAd.setFullScreenContentCallback(null);
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