package com.ivy.ads;

import android.util.Log;

import com.ivy.base.ad.AdType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 串行模式广告加载器 (实现 AdLoader)
 * 按照配置优先级瀑布流轮转，全失败后统一冷却。
 * 支持软超时（默认10s）：超时后允许放行下一层 candidate，同时保持当前层请求在途有效。
 * 若多层均加载成功，均进入就绪库存，展示层按最高 eCPM 选择。
 */
final class SerialAdLoader implements AdLoader, Runnable {
    private static final String TAG = "BlastAds_Serial";
    private static final long PLATFORM_INIT_TIMEOUT_MS = 8000L;

    interface Host {
        long now();
        void schedule(Runnable task, long delayMs);
        void cancel(Runnable task);
        void load(BaseAd ad);
    }

    private final AdType adType;
    private final Host host;
    private final List<BaseAd> ads = new ArrayList<>();
    private final Set<BaseAd> inFlightAds = new HashSet<>();
    private final Set<BaseAd> softTimedOutAds = new HashSet<>();
    private final Map<BaseAd, Runnable> timeoutTasks = new HashMap<>();
    private final long startTimeMs;
    private int nextIndex;
    private volatile boolean stopped;
    private boolean schedulerMode;

    SerialAdLoader(Host host) {
        this(AdType.INTERSTITIAL, host);
    }

    SerialAdLoader(AdType adType, Host host) {
        this.adType = adType;
        this.host = host;
        this.startTimeMs = host.now();
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
        int index = ads.size();
        if (!ad.isSecondary()) {
            index = 0;
            while (index < ads.size() && !ads.get(index).isSecondary()) {
                index++;
            }
        }
        ads.add(index, ad);
    }

    void setSchedulerMode(boolean schedulerMode) {
        this.schedulerMode = schedulerMode;
    }

    boolean isSchedulerMode() {
        return schedulerMode;
    }

    boolean isAnyLoading() {
        if (!inFlightAds.isEmpty()) return true;
        for (BaseAd ad : ads) {
            if ("promotion".equals(ad.getPlatform())) continue;
            if (ad.isLoading) return true;
        }
        return false;
    }

    boolean isAnyReady() {
        for (BaseAd ad : ads) {
            if ("promotion".equals(ad.getPlatform())) continue;
            if (ad.isReady()) return true;
        }
        return false;
    }

    void releaseReadyAds() {
        for (BaseAd ad : ads) {
            if ("promotion".equals(ad.getPlatform())) continue;
            if (ad.isReady()) {
                Log.w(TAG, adType + " 串行加载器释放已就绪广告: " + ad.getAdUnit());
                ad.close();
            }
        }
    }

    @Override
    public void onPlatformInitialized(String platform) {
        if (stopped) return;
        for (BaseAd ad : ads) {
            if (platform.equals(ad.getPlatform())) {
                ad.platformInitialized = true;
            }
        }
        if ("promotion".equals(platform)) {
            return;
        }
        Log.d(TAG, adType + " 收到平台初始化完成: " + platform);
        if (!schedulerMode) {
            request();
        }
    }

    @Override
    public void load(String platform) {
        request();
    }

    @Override
    public void request(BaseAd ad) {
        request();
    }

    void request() {
        if (stopped) return;
        host.cancel(this);
        host.schedule(this, 0L);
    }

    @Override
    public void scheduleReload(BaseAd ad) {
        if (schedulerMode) return;
        request();
    }

    @Override
    public void checkInventory() {
        if (schedulerMode) return;
        request();
    }

    boolean ownsLoad(BaseAd ad) {
        return !stopped && inFlightAds.contains(ad);
    }

    private void cancelTimeout(BaseAd ad) {
        Runnable task = timeoutTasks.remove(ad);
        if (task != null) {
            host.cancel(task);
        }
    }

    private void onSoftTimeout(BaseAd ad) {
        if (stopped) return;
        timeoutTasks.remove(ad);
        if (inFlightAds.contains(ad)) {
            Log.w(TAG, adType + " 广告软超时(" + ad.getTimeoutSeconds() + "s)，放行下一档: " + ad.getAdUnit());
            softTimedOutAds.add(ad);
            request();
        }
    }

    @Override
    public void onLoadSkipped(BaseAd ad) {
        if (!ownsLoad(ad)) return;
        Log.d(TAG, adType + " 广告跳过加载: " + ad.getAdUnit());
        cancelTimeout(ad);
        inFlightAds.remove(ad);
        softTimedOutAds.remove(ad);
        nextIndex = 0;
        request();
    }

    @Override
    public void onLoadStart(BaseAd ad) {
    }

    @Override
    public void onLoadSuccess(BaseAd ad) {
        if (!ownsLoad(ad)) return;
        Log.i(TAG, adType + " 广告加载成功: " + ad.getAdUnit() + ", 价格: " + ad.getPrice());
        cancelTimeout(ad);
        inFlightAds.remove(ad);
        softTimedOutAds.remove(ad);
        ad.serialRetryAtMs = 0L;
        ad.onLoadSuccess();
        // A new round starts only after the available inventory is consumed or expires.
        nextIndex = 0;
        request();
    }

    @Override
    public void onLoadFailed(BaseAd ad, int code, String message) {
        if (!ownsLoad(ad)) return;
        ad.onLoadFailed(code, message);
        long retryDelayMs = Math.max(1, ad.getDelaySeconds()) * 1000L;
        if (ad.isBlocked()) {
            long blockedRemainingMs = ad.getBlockStartTime()
                    + ad.getBlockDuration() * 60000L - System.currentTimeMillis();
            retryDelayMs = Math.max(retryDelayMs, blockedRemainingMs + 1L);
        }
        onLoadFailed(ad, retryDelayMs);
    }

    void onLoadFailed(BaseAd ad, long retryDelayMs) {
        if (!ownsLoad(ad)) return;
        Log.w(TAG, adType + " 广告加载失败: " + ad.getAdUnit() + ", 冷却重试延迟: " + retryDelayMs + "ms");
        cancelTimeout(ad);
        inFlightAds.remove(ad);
        softTimedOutAds.remove(ad);
        ad.serialRetryAtMs = host.now() + Math.max(1000L, retryDelayMs);
        request();
    }

    @Override
    public void pause() {
        host.cancel(this);
    }

    @Override
    public void resume() {
        if (!stopped) {
            request();
        }
    }

    @Override
    public void stop() {
        stopped = true;
        host.cancel(this);
        for (Runnable task : timeoutTasks.values()) {
            host.cancel(task);
        }
        timeoutTasks.clear();
        inFlightAds.clear();
        softTimedOutAds.clear();
    }

    @Override
    public void run() {
        if (stopped || ads.isEmpty()) {
            if (ads.isEmpty()) {
                Log.w(TAG, adType + " 串行队列为空，无已解析的广告");
            }
            return;
        }

        // If there is any active in-flight request that hasn't soft-timed out yet, wait.
        if (inFlightAds.size() > softTimedOutAds.size()) {
            Log.d(TAG, adType + " 串行队列等待中: 当前有请求在途 (" + inFlightAds.size() + " inFlight, " + softTimedOutAds.size() + " softTimedOut)");
            return;
        }

        // If any ad is ready or currently showing, pause serial queue.
        for (BaseAd ad : ads) {
            if (ad.isShowing) {
                Log.d(TAG, adType + " 串行队列暂停: 已有广告展示中: unit=" + ad.getAdUnit() + ", showing=true");
                return;
            }
            if ("promotion".equals(ad.getPlatform())) {
                continue;
            }
            if (ad.isReady()) {
                Log.d(TAG, adType + " 串行队列暂停: 已有广告就绪: unit=" + ad.getAdUnit() + ", ready=true");
                return;
            }
            if (ad.isLoading && !softTimedOutAds.contains(ad)) {
                Log.d(TAG, adType + " 串行队列等待中: 广告加载中: unit=" + ad.getAdUnit());
                return;
            }
        }

        long now = host.now();
        while (nextIndex < ads.size()) {
            BaseAd ad = ads.get(nextIndex);
            if ("promotion".equals(ad.getPlatform())) {
                nextIndex++;
                continue;
            }
            // Initialization order must not let a lower-priority platform win the first round,
            // but must not wait indefinitely if a high-priority platform fails or hangs during init.
            if (!ad.platformInitialized) {
                long elapsed = now - startTimeMs;
                if (elapsed < PLATFORM_INIT_TIMEOUT_MS) {
                    long remainingWait = Math.max(1L, PLATFORM_INIT_TIMEOUT_MS - elapsed);
                    Log.w(TAG, adType + " 等待高优先级平台初始化(剩余 " + remainingWait + "ms): platform=" + ad.getPlatform() + ", adUnit=" + ad.getAdUnit() + ", 暂不加载后续低优先级广告");
                    host.cancel(this);
                    host.schedule(this, remainingWait);
                    return;
                }
                Log.w(TAG, adType + " 高优先级平台初始化超时(" + elapsed + "ms)，跳过并放行后续阶位: platform=" + ad.getPlatform() + ", adUnit=" + ad.getAdUnit());
                nextIndex++;
                continue;
            }
            nextIndex++;
            if (inFlightAds.contains(ad)) continue;
            if (now < ad.serialRetryAtMs) {
                Log.d(TAG, adType + " 跳过冷却中的广告: unit=" + ad.getAdUnit() + ", 剩余冷却=" + (ad.serialRetryAtMs - now) + "ms");
                continue;
            }

            inFlightAds.add(ad);
            Runnable timeoutTask = new Runnable() {
                @Override
                public void run() {
                    onSoftTimeout(ad);
                }
            };
            timeoutTasks.put(ad, timeoutTask);
            long timeoutMs = Math.max(1, ad.getTimeoutSeconds()) * 1000L;
            host.schedule(timeoutTask, timeoutMs);
            Log.i(TAG, adType + " 发起串行加载: [" + (nextIndex - 1) + "] platform=" + ad.getPlatform() + ", adUnit=" + ad.getAdUnit() + ", softTimeout=" + timeoutMs + "ms");
            host.load(ad);
            return;
        }

        // Every candidate was attempted or cooling down. Wait before starting another round.
        nextIndex = 0;
        if (!inFlightAds.isEmpty()) {
            // Some candidates are still in flight (soft-timed-out). Wait for their SDK callbacks.
            Log.d(TAG, adType + " 本轮尝试完毕，等待已超时的在途请求回调");
            return;
        }

        long nextRetryAt = Long.MAX_VALUE;
        for (BaseAd ad : ads) {
            if ("promotion".equals(ad.getPlatform())) {
                continue;
            }
            if (ad.platformInitialized) {
                nextRetryAt = Math.min(nextRetryAt, ad.serialRetryAtMs);
            }
        }
        if (nextRetryAt != Long.MAX_VALUE) {
            long delay = Math.max(1L, nextRetryAt - now);
            Log.d(TAG, adType + " 本轮尝试完毕，所有广告均在冷却中，将在 " + delay + "ms 后重试下一轮");
            host.cancel(this);
            host.schedule(this, delay);
        }
    }
}
