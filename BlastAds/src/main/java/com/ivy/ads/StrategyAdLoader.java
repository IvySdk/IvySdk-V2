package com.ivy.ads;

import com.ivy.base.ad.AdType;

import java.util.ArrayList;
import java.util.List;

/**
 * 策略调度模式广告加载器
 * 受 AdAutoScheduler 策略引擎外部统一决策调度，禁用内部主动重试与巡检
 */
final class StrategyAdLoader implements AdLoader {
    private final AdType adType;
    private final AdLoadController controller;
    private final List<BaseAd> ads = new ArrayList<>();
    private boolean stopped;
    private boolean paused;

    StrategyAdLoader(AdType adType, AdLoadController controller) {
        this.adType = adType;
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
        // 策略调度模式下忽略外部全屏手动加载请求
    }

    @Override
    public void request(BaseAd ad) {
        if (stopped || paused) return;
        controller.startLoad(ad);
    }

    @Override
    public void scheduleReload(BaseAd ad) {
        // 策略调度模式下屏蔽内部自动重试，完全由 AdAutoScheduler 决策
    }

    @Override
    public void checkInventory() {
        // 策略调度模式下屏蔽内部轮询巡检
    }

    @Override
    public void onLoadSkipped(BaseAd ad) {
    }

    @Override
    public void onLoadStart(BaseAd ad) {
    }

    @Override
    public void onLoadSuccess(BaseAd ad) {
        ad.onLoadSuccess();
    }

    @Override
    public void onLoadFailed(BaseAd ad, int code, String message) {
        ad.onLoadFailed(code, message);
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
    }

    @Override
    public void stop() {
        stopped = true;
    }
}
