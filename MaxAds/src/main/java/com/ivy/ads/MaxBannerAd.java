package com.ivy.ads;

import android.view.View;
import android.view.ViewGroup;

import com.amazon.device.ads.AdError;
import com.amazon.device.ads.DTBAdCallback;
import com.amazon.device.ads.DTBAdNetwork;
import com.amazon.device.ads.DTBAdNetworkInfo;
import com.amazon.device.ads.DTBAdRequest;
import com.amazon.device.ads.DTBAdResponse;
import com.amazon.device.ads.DTBAdSize;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdkUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MAX横幅广告
 */
public class MaxBannerAd extends MaxBaseAd implements MaxAdViewAdListener {
    private MaxAdView adView;
    private boolean isLoadedAmazon = false;
    private boolean isLoaded = false;
    private boolean isShown = false;
    @Override
    protected void onInited() {
        adView = new MaxAdView(adUnit, context);
        adView.setListener(this);
        adView.setRevenueListener(this);
        
        // 设置刷新间隔
        if (refreshInterval > 0) {
            adView.setExtraParameter("ad_refresh_seconds", String.valueOf(refreshInterval));
        }
        
        // 设置是否禁止自动重试
        if (disableAutoRetry) {
            adView.setExtraParameter("disable_auto_retries", "true");
            adView.setExtraParameter("disable_precache", "true");
        }

        adView.setExtraParameter("adaptive_banner", "true");
        close();
    }

    private void loadAmazonAd() {
        AppLovinSdkUtils.Size size = MaxAdFormat.BANNER.getSize();
        DTBAdSize dTBAdSize = new DTBAdSize(size.getWidth(), size.getHeight(), amazonSlotId);
        DTBAdRequest adLoader = new DTBAdRequest(new DTBAdNetworkInfo(DTBAdNetwork.MAX));
        adLoader.setSizes(dTBAdSize);
//        final AtomicBoolean lock = new AtomicBoolean(false);
        adLoader.loadAd(new DTBAdCallback() {
            @Override
            public void onFailure(AdError p0) {
//                lock.set(true);
                adView.setLocalExtraParameter("amazon_ad_error", p0);
                loadAd();
            }

            @Override
            public void onSuccess(DTBAdResponse p0) {
//                lock.set(true);
                adView.setLocalExtraParameter("amazon_ad_response", p0);
                loadAd();
            }
        });
    }

    /**
     * 加载横幅广告（内部使用）
     */
    @Override
    protected void loadAd() {
        isLoaded = false;
        if (amazonSlotId != null && !isLoadedAmazon) {
            isLoadedAmazon = true;
            loadAmazonAd();
            return;
        }
        isShown = false;
        // 加载广告
        adView.loadAd();
    }

    @Override
    public boolean isReady() {
        return isLoaded && !isShown;
    }

    @Override
    public void show(AdShowCallback callback) {
        isShown = true;
        showCallback = callback;
        adView.setVisibility(View.VISIBLE);
        if (getRefreshSeconds() == 0){
            adView.startAutoRefresh();
        }
    }

    public void close(){
        isShown = false;
        adView.setVisibility(View.GONE);
        adView.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
        adView.stopAutoRefresh();
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
        }
        adView = null;
        revenueCallback = null;
        isLoading = false;
    }

    @Override
    public void onAdLoaded(MaxAd ad) {
        isLoaded = true;
        super.onAdLoaded(ad);
    }

    @Override
    public void onAdLoadFailed(String adUnitId, MaxError error) {
        isLoadedAmazon = false;
        super.onAdLoadFailed(adUnitId, error);
    }

    @Override
    public void onAdExpanded(MaxAd ad) {
        // Banner广告展开时的回调
    }
    
    @Override
    public void onAdCollapsed(MaxAd ad) {
        // Banner广告收起时的回调
    }

    @Override
    public View getView() {
        return adView;
    }
}