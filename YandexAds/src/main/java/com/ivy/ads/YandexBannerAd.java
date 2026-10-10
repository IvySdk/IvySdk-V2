package com.ivy.ads;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ivy.base.ad.AdType;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;

import org.json.JSONObject;

/**
 * AdMob横幅广告
 */
public class YandexBannerAd extends YandexBaseAd {
    private BannerAdView adView;
    private boolean isLoaded = false;

    private boolean isShown = false;

    @Override
    public void init(AdType adType, JSONObject data, AdRevenueCallback revenueCallback, Activity context) {
        super.init(adType, data, revenueCallback, context);
        createBannerAd();
    }

    private BannerAdSize getAdSize(Activity activity) {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return BannerAdSize.sticky(activity, adWidth);
    }

    private void createBannerAd() {
        adView = new BannerAdView(context);
        adView.setAdSize(getAdSize(context));

        // Set up ad listeners
        adView.setBannerAdEventListener(createAdListener());
    }

    private BannerAdEventListener createAdListener() {
        return new BannerAdEventListener() {
            @Override
            public void onImpression(@Nullable ImpressionData impressionData) {
                onPaidEvent(impressionData);
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError loadAdError) {
                isLoading = false;
                isLoaded = false;
                if (loadCallback != null){
                    loadCallback.onLoadFailed(YandexBannerAd.this, loadAdError.getCode(), loadAdError.getDescription());
                }
            }

            @Override
            public void onAdLoaded() {
                isLoading = false;
                isLoaded = true;
                loadCallback.onLoadSuccess(YandexBannerAd.this);
            }

            @Override
            public void onAdClicked() {
                // Banner广告点击事件
                showCallback.onAdClicked(YandexBannerAd.this);
            }
        };
    }

    /**
     * 加载横幅广告（内部使用）
     */
    @Override
    protected void loadAd() {
        isLoaded = false;
        isShown = false;
        adView.loadAd(new AdRequest.Builder(adUnit).build());
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
        if (adView != null) {
            adView.setVisibility(View.GONE);
        }
    }

    @Override
    public View getView() {
        return adView;
    }
}