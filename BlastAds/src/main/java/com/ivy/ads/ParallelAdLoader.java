package com.ivy.ads;

import android.os.Handler;

import com.ivy.base.ad.AdType;

import java.util.ArrayList;
import java.util.List;

/**
 * 并行模式广告加载器
 * 各广告独立发起请求，独立按指数退避或 refreshSeconds 延时重试
 */
final class ParallelAdLoader implements AdLoader {
    private final AdType adType;
    private final Handler handler;
    private final AdLoadController controller;
    private final List<BaseAd> ads = new ArrayList<>();
    private boolean stopped;
    private boolean paused;

    ParallelAdLoader(AdType adType, Handler handler, AdLoadController controller) {
        this.adType = adType;
        this.handler = handler;
        this.controller = controller;
    }

    @Override
    public AdType getAdType() {
        return adType;
    }

    @Override
    public List<BaseAd> getAds() {
        return ads;
    }

    @Override
    public void add(BaseAd ad) {
        ads.add(ad);
    }

    @Override
    public void onPlatformInitialized(String platform) {
        if (stopped) return;
        for (BaseAd ad : ads) {
            if (platform.equals(ad.getPlatform())) {
                ad.platformInitialized = true;
            }
        }
    }

    @Override
    public void load(String platform) {
        if (stopped || paused) return;
        for (BaseAd ad : ads) {
            if (platform == null || platform.equals(ad.getPlatform())) {
                controller.startLoad(ad);
            }
        }
    }

    @Override
    public void request(BaseAd ad) {
        if (stopped || paused) return;
        controller.startLoad(ad);
    }

    @Override
    public void scheduleReload(BaseAd ad) {
        if (stopped || paused || ad == null || ad.destroyed) return;
        if (adType == AdType.BANNER && AdCore.isBannerSuspended()) return;
        if (!ad.delayed) {
            long delayMs = (ad.getRefreshSeconds() == 0 ? ad.getDelaySeconds() : ad.getRefreshSeconds()) * 1000L;
            ad.delayed = true;
            handler.postDelayed(ad, delayMs);
        }
    }

    @Override
    public void checkInventory() {
        if (stopped || paused) return;
        for (BaseAd ad : ads) {
            if (ad.platformInitialized && !ad.isReady() && !ad.isLoading && !ad.loadPending) {
                scheduleReload(ad);
            }
        }
    }

    @Override
    public void onLoadSkipped(BaseAd ad) {
    }

    @Override
    public void onLoadStart(BaseAd ad) {
    }

    @Override
    public void onLoadSuccess(BaseAd ad) {
        handler.removeCallbacks(ad);
        ad.delayed = false;
        ad.onLoadSuccess();
    }

    @Override
    public void onLoadFailed(BaseAd ad, int code, String message) {
        ad.onLoadFailed(code, message);
        scheduleReload(ad);
    }

    @Override
    public void pause() {
        paused = true;
        for (BaseAd ad : ads) {
            handler.removeCallbacks(ad);
            ad.delayed = false;
        }
    }

    @Override
    public void resume() {
        if (stopped) return;
        paused = false;
        checkInventory();
    }

    @Override
    public void stop() {
        stopped = true;
        for (BaseAd ad : ads) {
            handler.removeCallbacks(ad);
            ad.delayed = false;
        }
    }
}
