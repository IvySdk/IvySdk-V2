package com.ivy.ads;

import com.ivy.base.ad.AdType;

import java.util.List;

/**
 * 广告加载策略接口 (多态抽象)
 * 封装 Serial / Parallel / Strategy 模式下的独立加载、失败重试与轮次冷却行为
 */
interface AdLoader {
    AdType getAdType();
    List<BaseAd> getAds();
    void add(BaseAd ad);
    void onPlatformInitialized(String platform);
    void load(String platform);
    void request(BaseAd ad);
    void scheduleReload(BaseAd ad);
    void checkInventory();
    void onLoadSkipped(BaseAd ad);
    void onLoadStart(BaseAd ad);
    void onLoadSuccess(BaseAd ad);
    void onLoadFailed(BaseAd ad, int code, String message);
    void pause();
    void resume();
    void stop();
}
