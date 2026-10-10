package com.ivy.ads;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.BaseAdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.ivy.base.ad.AdType;

import org.json.JSONObject;

/**
 * AdMob横幅广告
 */
public class AdmobBannerAd extends AdmobBaseAd {
    private BaseAdView adView;
    private boolean isLoaded = false;
    private boolean isShown = false;
    private void fetchNetworkName(BaseAdView ad){
        try {
            // Set paid event listener right after loading
            networkName = ad.getResponseInfo().getLoadedAdapterResponseInfo() != null ?
                    ad.getResponseInfo().getLoadedAdapterResponseInfo().getAdSourceName() : "admob";
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
        adView = isAdmanager ? new AdManagerAdView(context) : new AdView(context);
        // Prevent the internal ad WebView from retaining focus after it is detached.
        adView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        adView.setAdUnitId(adUnit);
        adView.setAdSize(getAdSize(context));

        // Set up ad listeners
        adView.setAdListener(createAdListener());

        // Set up paid event listener for revenue tracking
        adView.setOnPaidEventListener(this);
    }

    private AdListener createAdListener() {
        return new AdListener() {
            @Override
            public void onAdLoaded() {
                isLoading = false;
                isLoaded = true;

                fetchNetworkName(adView);
                loadCallback.onLoadSuccess(AdmobBannerAd.this);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                isLoading = false;
                loadCallback.onLoadFailed(AdmobBannerAd.this, loadAdError.getCode(), loadAdError.getMessage());
            }

            @Override
            public void onAdImpression() {
                showCallback.onAdDisplayed(AdmobBannerAd.this);
            }

            @Override
            public void onAdClicked() {
                // Banner广告点击事件
                showCallback.onAdClicked(AdmobBannerAd.this);
            }

            @Override
            public void onAdClosed() {
                // Banner广告关闭事件 (通常是点击后返回)
            }

            @Override
            public void onAdOpened() {
                // Banner广告打开事件 (通常是点击后打开)
            }
        };
    }

    /**
     * 加载横幅广告（内部使用）
     */
    @Override
    protected void loadAd() {
        // 加载广告
        isLoaded = false;
        isShown = false;
        if (isAdmanager){
            ((AdManagerAdView)adView).loadAd(new AdManagerAdRequest.Builder().build());
        } else {
            adView.loadAd(new AdRequest.Builder().build());
        }
    }

    @Override
    public void show(AdShowCallback callback) {
        isShown = true;
        showCallback = callback;
        adView.setVisibility(View.VISIBLE);
        adView.resume();
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
            // AdMob doesn't have a direct equivalent to stopAutoRefresh, but we can pause it
            adView.pause();
        }
    }

    @Override
    public View getView() {
        return adView;
    }
}
