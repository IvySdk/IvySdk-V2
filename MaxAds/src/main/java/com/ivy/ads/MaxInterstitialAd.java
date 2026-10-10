package com.ivy.ads;

/**
 * MAX插屏广告
 */
public class MaxInterstitialAd extends MaxBaseAd {
    
    private com.applovin.mediation.ads.MaxInterstitialAd interstitialAd;

    @Override
    protected void onInited() {
        interstitialAd = new com.applovin.mediation.ads.MaxInterstitialAd(adUnit, context);
        interstitialAd.setListener(this);
        interstitialAd.setRevenueListener(this);
        if (disableAutoRetry) {
            interstitialAd.setExtraParameter("disable_auto_retries", "true");
            interstitialAd.setExtraParameter("disable_precache", "true");
        }
    }

    /**
     * 检查广告是否准备好展示
     * @return 是否可以展示广告
     */
    public boolean isReady() {
        return !isShowing && (interstitialAd != null && interstitialAd.isReady());
    }

    @Override
    protected void loadAd() {
        interstitialAd.loadAd();
    }

    @Override
    public void show(AdShowCallback callback) {
        if (!isReady()) return;

        this.showCallback = callback;

        isShowing = true;

        interstitialAd.showAd(context);
    }

    /**
     * 销毁广告实例，释放资源
     */
    public void destroy() {
        if (interstitialAd != null) {
            interstitialAd.destroy();
        }
        isShowing = false;
        interstitialAd = null;
        loadCallback = null;
        showCallback = null;
        revenueCallback = null;
    }
}