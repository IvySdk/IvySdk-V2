package com.ivy.ads;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.AdView;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.ivy.base.ad.AdType;

import org.json.JSONObject;

/**
 * AdMob横幅广告
 */
public class AdmobBannerAd extends AdmobBaseAd implements BannerAdEventCallback {
    private AdView adView;
    private boolean isLoaded = false;
    private boolean isShown = false;
    private void fetchNetworkName(AdView ad){
        try {
            // Set paid event listener right after loading
            networkName = ad.getBannerAd().getResponseInfo().getLoadedAdSourceResponseInfo() != null ?
                    ad.getBannerAd().getResponseInfo().getLoadedAdSourceResponseInfo().getName() : "admob";
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void init(AdType adType, JSONObject data, AdRevenueCallback revenueCallback, Activity context) {
        super.init(adType, data, revenueCallback, context);
        createBannerAd();
    }

    private AdSize getAdSize(Activity activity) {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    private void createBannerAd() {
        adView = new AdView(context);
    }

    @Override
    public void onAdClicked() {
        fetchNetworkName(adView);
        showCallback.onAdClicked(AdmobBannerAd.this);
    }

    @Override
    public void onAdDismissedFullScreenContent() {

    }

    @Override
    public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError) {

    }

    @Override
    public void onAdPaid(@NonNull AdValue value) {
        fetchNetworkName(adView);
        onPaidEvent(value);
    }

    @Override
    public void onAdShowedFullScreenContent() {

    }

    /**
     * 加载横幅广告（内部使用）
     */
    @Override
    protected void loadAd() {
        // 加载广告
        isLoaded = false;
        isShown = false;
        adView.loadAd(new BannerAdRequest.Builder(adUnit, getAdSize(context)).build(), new com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<BannerAd>() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                isLoading = false;
                loadCallback.onLoadFailed(AdmobBannerAd.this, loadAdError.getCode().getValue(), loadAdError.getMessage());
            }

            @Override
            public void onAdLoaded(BannerAd ad) {
                isLoading = false;
                isLoaded = true;

                fetchNetworkName(adView);
                loadCallback.onLoadSuccess(AdmobBannerAd.this);
                try {
                    adView.getBannerAd().setAdEventCallback(AdmobBannerAd.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void show(AdShowCallback callback) {
        isShown = true;
        showCallback = callback;
        adView.setVisibility(View.VISIBLE);
    }

    /**
     * 检查广告是否准备好展示
     * @return 是否可以展示广告
     */
    public boolean isReady() {
        // Banner广告总是可以尝试加载和展示
        return isLoaded && !isShown;
    }

    /**
     * 销毁广告实例，释放资源
     */
    public void destroy() {
        // 从父视图中移除广告
        if (adView != null && adView.getParent() != null) {
            ((ViewGroup) adView.getParent()).removeView(adView);
        }

        if (adView != null) {
            adView.destroy();
            adView = null;
        }

        revenueCallback = null;
        isLoaded = false;
        isLoading = false;
    }

    public void close() {
        isShown = false;
        if (adView != null) {
            adView.setVisibility(View.GONE);
        }
    }

    @Override
    public View getView() {
        return adView;
    }
}