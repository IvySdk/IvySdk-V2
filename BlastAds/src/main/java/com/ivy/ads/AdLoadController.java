package com.ivy.ads;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.ivy.base.ad.AdType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 统一高层广告加载协调器
 * 通过多态持有各 AdType 对应的 AdLoader (Parallel / Serial / Strategy)，统一收口调度
 */
final class AdLoadController implements BaseAd.AdLoadCallback {
    private static final int EXPIRED_CHECK_INTERVAL_MS = 10 * 60000;
    private final Handler handler;
    private final BaseAd.AdLoadCallback listener;
    private final Map<AdType, AdLoader> loaders = new EnumMap<>(AdType.class);
    private boolean schedulerMode;
    private boolean paused;
    private boolean stopped;

    private final Runnable checkExpiredAds = new Runnable() {
        @Override
        public void run() {
            if (stopped || paused || schedulerMode) return;
            checkInventory();
            handler.postDelayed(this, EXPIRED_CHECK_INTERVAL_MS);
        }
    };

    AdLoadController(Handler handler, boolean serial, BaseAd.AdLoadCallback listener) {
        this.handler = handler;
        this.listener = listener;

        loaders.put(AdType.BANNER, new ParallelAdLoader(AdType.BANNER, handler, this));

        if (serial) {
            loaders.put(AdType.INTERSTITIAL, new SerialAdLoader(AdType.INTERSTITIAL, createSerialHost()));
            loaders.put(AdType.REWARDED, new SerialAdLoader(AdType.REWARDED, createSerialHost()));
        } else {
            loaders.put(AdType.INTERSTITIAL, new ParallelAdLoader(AdType.INTERSTITIAL, handler, this));
            loaders.put(AdType.REWARDED, new ParallelAdLoader(AdType.REWARDED, handler, this));
        }

        handler.postDelayed(checkExpiredAds, EXPIRED_CHECK_INTERVAL_MS);
    }

    private SerialAdLoader.Host createSerialHost() {
        return new SerialAdLoader.Host() {
            @Override public long now() { return SystemClock.elapsedRealtime(); }
            @Override public void schedule(Runnable task, long delayMs) { handler.postDelayed(task, delayMs); }
            @Override public void cancel(Runnable task) { handler.removeCallbacks(task); }
            @Override public void load(BaseAd ad) { startLoad(ad); }
        };
    }

    AdLoader getLoader(AdType type) {
        return loaders.get(type);
    }

    List<BaseAd> getAds(AdType type) {
        AdLoader loader = getLoader(type);
        return loader != null ? loader.getAds() : Collections.emptyList();
    }

    void prepare(BaseAd ad, AdType type) {
        ad.loadController = this;
        AdLoader loader = getLoader(type);
        if (loader instanceof SerialAdLoader) {
            ad.serialLoader = (SerialAdLoader) loader;
        }
    }

    void add(BaseAd ad) {
        AdLoader loader = getLoader(ad.getAdType());
        if (loader != null) {
            loader.add(ad);
        }
    }

    void setSchedulerMode(boolean enabled) {
        if (this.schedulerMode == enabled) return;
        this.schedulerMode = enabled;
        handler.removeCallbacks(checkExpiredAds);
        if (enabled) {
            for (AdType type : new AdType[]{AdType.INTERSTITIAL, AdType.REWARDED}) {
                AdLoader current = loaders.get(type);
                if (current instanceof SerialAdLoader) {
                    ((SerialAdLoader) current).setSchedulerMode(true);
                    continue;
                }
                StrategyAdLoader strategyLoader = new StrategyAdLoader(type, this);
                if (current != null) {
                    for (BaseAd ad : current.getAds()) {
                        strategyLoader.add(ad);
                    }
                    current.stop();
                }
                loaders.put(type, strategyLoader);
            }
        } else if (!stopped && !paused) {
            for (AdType type : new AdType[]{AdType.INTERSTITIAL, AdType.REWARDED}) {
                AdLoader current = loaders.get(type);
                if (current instanceof SerialAdLoader) {
                    ((SerialAdLoader) current).setSchedulerMode(false);
                }
            }
            handler.postDelayed(checkExpiredAds, EXPIRED_CHECK_INTERVAL_MS);
        }
    }

    void onPlatformInitialized(String platform) {
        if (stopped) return;
        for (AdLoader loader : loaders.values()) {
            loader.onPlatformInitialized(platform);
        }
    }

    void load(AdType type, String platform) {
        if (stopped || paused) return;
        AdLoader loader = getLoader(type);
        if (loader != null) {
            loader.load(platform);
        }
    }

    void request(BaseAd ad) {
        handler.post(() -> {
            if (!owns(ad) || paused) return;
            AdLoader loader = getLoader(ad.getAdType());
            if (loader != null) {
                loader.request(ad);
            }
        });
    }

    boolean startLoad(BaseAd ad) {
        if (!owns(ad) || paused || !ad.platformInitialized || ad.loadPending) {
            AdLoader loader = getLoader(ad.getAdType());
            if (loader != null) {
                loader.onLoadSkipped(ad);
            }
            return false;
        }
        handler.removeCallbacks(ad);
        ad.delayed = false;
        ad.loadPending = true;
        try {
            if (!ad.tryLoad(this)) {
                ad.loadPending = false;
                AdLoader loader = getLoader(ad.getAdType());
                if (loader != null) {
                    loader.onLoadSkipped(ad);
                }
            }
        } catch (RuntimeException error) {
            Log.e("hoho", "SDK load threw: " + ad.getPlatform() + " " + ad.getAdUnit(), error);
            ad.isLoading = false;
            onLoadFailed(ad, -1, "SDK load exception: " + error.getClass().getSimpleName());
        }
        return ad.loadPending || ad.isLoading || ad.isReady();
    }

    void scheduleReload(BaseAd ad) {
        if (!owns(ad) || paused) return;
        AdLoader loader = getLoader(ad.getAdType());
        if (loader != null) {
            loader.scheduleReload(ad);
        }
    }

    void checkInventory() {
        if (stopped || paused) return;
        for (AdLoader loader : loaders.values()) {
            loader.checkInventory();
        }
    }

    void requestInventory(AdType type) {
        if (stopped || paused) return;
        AdLoader loader = getLoader(type);
        if (loader != null) {
            loader.checkInventory();
        }
    }

    void pause() {
        paused = true;
        handler.removeCallbacksAndMessages(null);
        for (AdLoader loader : loaders.values()) {
            loader.pause();
        }
    }

    void resume() {
        if (stopped) return;
        paused = false;
        for (AdLoader loader : loaders.values()) {
            loader.resume();
        }
        if (!schedulerMode) {
            handler.removeCallbacks(checkExpiredAds);
            handler.postDelayed(checkExpiredAds, EXPIRED_CHECK_INTERVAL_MS);
        }
    }

    void stop() {
        stopped = true;
        handler.removeCallbacksAndMessages(null);
        for (AdLoader loader : loaders.values()) {
            loader.stop();
        }
    }

    boolean owns(BaseAd ad) {
        return !stopped && ad != null && ad.loadController == this && !ad.destroyed;
    }

    @Override
    public void onLoadStart(BaseAd ad) {
        handler.post(() -> {
            if (owns(ad)) {
                AdLoader loader = getLoader(ad.getAdType());
                if (loader != null) {
                    loader.onLoadStart(ad);
                }
                listener.onLoadStart(ad);
            }
        });
    }

    @Override
    public void onLoadSuccess(BaseAd ad) {
        handler.post(() -> {
            if (!owns(ad)) return;
            ad.loadPending = false;
            AdLoader loader = getLoader(ad.getAdType());
            if (loader != null) {
                loader.onLoadSuccess(ad);
            }
            listener.onLoadSuccess(ad);
        });
    }

    @Override
    public void onLoadFailed(BaseAd ad, int code, String message) {
        handler.post(() -> {
            if (!owns(ad)) return;
            ad.loadPending = false;
            AdLoader loader = getLoader(ad.getAdType());
            if (loader != null) {
                loader.onLoadFailed(ad, code, message);
            }
            listener.onLoadFailed(ad, code, message);
        });
    }
}
