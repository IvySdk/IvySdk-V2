package com.ivy.ads;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.ivy.base.ad.AdType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 广告核心管理类，作为统一门面 (Facade)，协同管理 AdLoadController、FullScreenAdController 和 BannerController
 */
public class AdCore {
    private static final String TAG = "hoho";
    private static Activity context = null;
    private static boolean schedulerMode = false;
    private static BlastAdStrategyBridge strategyBridge = null;
    static AdLoadController adLoadController;
    private static FullScreenAdController fullScreenController;

    private static final Map<AdType, MyAdListener> adListeners = new HashMap<>();
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static final BaseAd.AdShowCallback bannerShowCallback = new BaseAd.AdShowCallback() {
        @Override
        public void onAdClosed(BaseAd ad, boolean rewarded) {
            scheduleReload(ad);
        }

        @Override
        public void onAdDisplayed(BaseAd ad) {
            if (ad.getRefreshSeconds() > 0 && !BannerController.isPromotion(ad)) {
                scheduleReload(ad);
            }
        }

        @Override
        public void onAdShowFailed(BaseAd ad, int errorCode, String message) {
            scheduleReload(ad);
        }

        @Override
        public void onAdClicked(BaseAd ad) {
        }
    };

    private static final BannerController bannerController = new BannerController(handler, bannerShowCallback);

    private static final FullScreenAdController.Host fullScreenHost = new FullScreenAdController.Host() {
        @Override
        public long now() {
            return SystemClock.elapsedRealtime();
        }

        @Override
        public void post(Runnable task) {
            handler.post(task);
        }

        @Override
        public List<BaseAd> getAds(AdType type) {
            return AdCore.getAds(type);
        }

        @Override
        public void onStarted(BaseAd ad) {
            notifyShowStarted(ad);
        }

        @Override
        public void onFinished(BaseAd ad) {
            notifyShowFinished(ad);
            scheduleReload(ad);
        }

        @Override
        public void onDisplayed(BaseAd ad) {
            Log.d(TAG, String.format("show %s %s success", ad.getAdType(), ad.getAdUnit()));
            notifyShowStarted(ad);
            MyAdListener listener = adListeners.get(ad.getAdType());
            if (listener != null) {
                listener.onAdShowSuccess(ad.getAdUnit(), ad.getPlatform(), ad.getNetworkName(), ad.getSegments());
            }
            if (ad.getRefreshSeconds() > 0) {
                scheduleReload(ad);
            }
        }

        @Override
        public void onClosed(BaseAd ad, boolean rewarded) {
            Log.d(TAG, String.format("close %s %s with rewarded %s", ad.getAdType(), ad.getAdUnit(), rewarded));
            MyAdListener listener = adListeners.get(ad.getAdType());
            if (listener != null) {
                listener.onAdClosed(rewarded, ad.getPlatform(), ad.getAdUnit(), ad.getNetworkName(), ad.getSegments());
            }
        }

        @Override
        public void onClicked(BaseAd ad) {
            Log.d(TAG, String.format("click %s %s", ad.getAdType(), ad.getAdUnit()));
            MyAdListener listener = adListeners.get(ad.getAdType());
            if (listener != null) {
                listener.onAdClicked(ad.getAdUnit(), ad.getPlatform(), ad.getNetworkName(), ad.getSegments());
            }
        }

        @Override
        public void onFailed(BaseAd ad, int code, String message, boolean hasFallback) {
            Log.d(TAG, String.format("show %s %s failed code=%d msg=%s hasFallback=%b", ad.getAdType(), ad.getAdUnit(), code, message, hasFallback));
            MyAdListener listener = adListeners.get(ad.getAdType());
            if (listener != null) {
                listener.onAdShowFailed(ad.getAdUnit(), ad.getPlatform(), message, ad.getNetworkName(), ad.getSegments(), hasFallback);
            }
        }
    };

    private static final BaseAd.AdLoadCallback adLoadCallback = new BaseAd.AdLoadCallback() {
        @Override
        public void onLoadStart(BaseAd ad) {
            Log.d(TAG, String.format("start load %s %s", ad.getAdType(), ad.getAdUnit()));
            notifyLoadStarted(ad);
            MyAdListener listener = adListeners.get(ad.getAdType());
            if (listener != null) {
                listener.onAdLoadStart(ad.getAdUnit(), ad.getPlatform(), ad.getSegments());
            }
        }

        @Override
        public void onLoadFailed(BaseAd ad, int errorCode, String message) {
            Log.d(TAG, String.format("failed load %s %s with %s %s", ad.getAdType(), ad.getAdUnit(), errorCode, message));
            notifyLoadFailed(ad, message);
            if (ad.getAdType() != AdType.BANNER && errorCode != -1 && !ad.isNofillError(errorCode, message)) {
                MyAdListener listener = adListeners.get(ad.getAdType());
                if (listener != null) {
                    listener.onAdLoadFailure(ad.getAdUnit(), ad.getPlatform(), errorCode, message, ad.getSegments(), (int) (SystemClock.elapsedRealtime() - ad.getLoadStartTime()));
                }
            }
        }

        @Override
        public void onLoadSuccess(BaseAd ad) {
            Log.d(TAG, String.format("loaded %s %s with price %f", ad.getAdType(), ad.getAdUnit(), ad.getPrice()));
            notifyLoadSuccess(ad);
            if (ad.getAdType() == AdType.BANNER) {
                bannerController.onAdLoaded(ad);
            } else {
                if (ad.getAdType() == AdType.INTERSTITIAL) {
                    if ("max".equals(ad.platform)) {
                        AdValueTagProcessor.getInstance().onAdLoaded(ad.price * 1000.0);
                    }
                }
                MyAdListener listener = adListeners.get(ad.getAdType());
                if (listener != null) {
                    listener.onAdLoadSuccess(ad.getAdUnit(), ad.getPlatform(), ad.getNetworkName(), ad.getSegments(), (int) (SystemClock.elapsedRealtime() - ad.getLoadStartTime()));
                }
            }
        }
    };

    private static final BaseAd.AdRevenueCallback revenueCallback = new BaseAd.AdRevenueCallback() {
        @Override
        public void onAdRevenuePaid(BaseAd adConfig) {
            Log.d(TAG, "onAdRevenuePaid: " + adConfig.getAdType() + " " + adConfig.getAdUnit() + " " + adConfig.getPrice() + " " + adConfig.getNetworkName() + " " + adConfig.getCurrencyCode());
            AdSessionStats.recordRevenue(adConfig);
            if (adConfig.getAdType() == AdType.INTERSTITIAL) {
                if ("admob".equals(adConfig.platform)) {
                    AdValueTagProcessor.getInstance().onAdLoaded(adConfig.price * 1000.0);
                }
            }

            MyAdListener listener = adListeners.get(adConfig.getAdType());
            if (listener != null) {
                listener.onAdRevenuePaid(adConfig.getPrice(), adConfig.getAdUnit(), adConfig.getPlatform(), adConfig.getCurrencyCode(), adConfig.getNetworkName(), adConfig.getSegments());
            }
        }
    };

    public static Set<String> init(Activity context, JSONObject config, MyAdListener bannerListener, MyAdListener interListener, MyAdListener rewardListener) {
        return init(context, config, bannerListener, interListener, rewardListener, false);
    }

    public static Set<String> init(Activity context, JSONObject config, MyAdListener bannerListener, MyAdListener interListener, MyAdListener rewardListener, boolean serialLoading) {
        try {
            destroySerialAds();
            adLoadController = new AdLoadController(handler, serialLoading, adLoadCallback);
            fullScreenController = new FullScreenAdController(fullScreenHost);
            AdCore.context = context;
            AdSessionStats.startSession(config.optInt("keepEcpmSessions", 1));

            adListeners.put(AdType.BANNER, bannerListener);
            adListeners.put(AdType.INTERSTITIAL, interListener);
            adListeners.put(AdType.REWARDED, rewardListener);
            bannerController.attach(context, bannerListener);

            if (config.has("banner")) {
                JSONArray bannerArray = config.getJSONArray("banner");
                parseAds(bannerArray, AdType.BANNER);
            }

            if (config.has("inter")) {
                JSONArray interArray = config.getJSONArray("inter");
                parseAds(interArray, AdType.INTERSTITIAL);
            }

            if (config.has("reward")) {
                JSONArray rewardArray = config.getJSONArray("reward");
                parseAds(rewardArray, AdType.REWARDED);
            }

            Log.d(TAG, String.format("AdCore initialized with %d banner, %d inter, %d reward ads",
                    getAds(AdType.BANNER).size(), getAds(AdType.INTERSTITIAL).size(), getAds(AdType.REWARDED).size()));

            Set<String> platforms = new HashSet<>();
            getPlatforms(getAds(AdType.BANNER), platforms);
            getPlatforms(getAds(AdType.INTERSTITIAL), platforms);
            getPlatforms(getAds(AdType.REWARDED), platforms);

            adLoadController.setSchedulerMode(schedulerMode);
            return platforms;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse ad config", e);
            return new HashSet<>();
        }
    }

    private static void getPlatforms(List<BaseAd> ads, Set<String> platforms) {
        for (BaseAd ad : ads) {
            platforms.add(ad.getPlatform());
            if (ad.getAmazonSlotId() != null) {
                platforms.add("aps");
            }
        }
    }

    public static void showAd(AdType adType) {
        if (fullScreenController != null) {
            boolean shown = fullScreenController.show(adType, () -> {});
            if (!shown) {
                Log.d(TAG, String.format("show ad %s not ready or busy", adType));
            }
        }
    }

    public static void showBanner(int pos) {
        bannerController.showBanner(pos);
    }

    public static void closeBanner() {
        bannerController.closeBanner();
    }

    static void suspendBannerByScheduler() {
        bannerController.suspendBannerByScheduler();
    }

    static void restoreSchedulerBanner() {
        bannerController.restoreSchedulerBanner();
    }

    static boolean isBannerSuspended() {
        return bannerController.isSuspended();
    }

    public static boolean isReady(AdType adType) {
        List<BaseAd> ads = getAds(adType);
        for (BaseAd ad : ads) {
            if (ad.isReady()) {
                return true;
            }
        }
        if (adLoadController != null) {
            adLoadController.requestInventory(adType);
        }
        return false;
    }

    public static BaseAd getHighestPriceAd(AdType adType) {
        return FullScreenAdController.select(getAds(adType), null);
    }

    public static BaseAd getHighestPriceAdExcluding(AdType adType, BaseAd excludedAd) {
        return FullScreenAdController.select(getAds(adType), Collections.singletonList(excludedAd));
    }

    public static List<BaseAd> getAds(AdType adType) {
        return adLoadController != null ? adLoadController.getAds(adType) : Collections.emptyList();
    }

    public static void load(AdType adType, String platform) {
        if (adLoadController != null) {
            adLoadController.load(adType, platform);
        }
    }

    public static void scheduleReload(BaseAd ad) {
        if (adLoadController != null) {
            adLoadController.scheduleReload(ad);
        }
    }

    static void onPlatformInitialized(String platform) {
        if (adLoadController != null) {
            adLoadController.onPlatformInitialized(platform);
        }
    }

    static void checkSerialInventory() {
        if (adLoadController != null) {
            adLoadController.checkInventory();
        }
    }

    static void destroySerialAds() {
        if (adLoadController != null) {
            adLoadController.stop();
        }
        if (fullScreenController != null) {
            fullScreenController.stop();
        }
    }

    static void destroyAllAds(String reason) {
        Log.w(TAG, "destroy all ads reason=" + reason);
        handler.removeCallbacksAndMessages(null);
        if (adLoadController != null) {
            adLoadController.stop();
        }
        if (fullScreenController != null) {
            fullScreenController.stop();
        }
        bannerController.destroy();
        if (adLoadController != null) {
            destroyAds(adLoadController.getAds(AdType.BANNER));
            destroyAds(adLoadController.getAds(AdType.INTERSTITIAL));
            destroyAds(adLoadController.getAds(AdType.REWARDED));
        }
    }

    private static void destroyAds(List<BaseAd> ads) {
        for (BaseAd ad : ads) {
            try {
                ad.destroyForShutdown();
            } catch (Throwable throwable) {
                Log.w(TAG, "destroy ad failed " + ad.getAdType() + " " + ad.getAdUnit(), throwable);
            }
        }
        ads.clear();
    }

    static BaseAd.AdLoadCallback getAdLoadCallback() {
        return adLoadCallback;
    }

    public static void setStrategyBridge(BlastAdStrategyBridge bridge) {
        strategyBridge = bridge;
        schedulerMode = bridge != null;
        if (adLoadController != null) {
            adLoadController.setSchedulerMode(schedulerMode);
        }
    }

    public static boolean isSchedulerMode() {
        return schedulerMode;
    }

    private static void notifyLoadStarted(BaseAd ad) {
        if (strategyBridge != null) {
            strategyBridge.onLoadStarted(ad);
        }
    }

    private static void notifyLoadSuccess(BaseAd ad) {
        if (strategyBridge != null) {
            strategyBridge.onLoadSuccess(ad);
        }
    }

    private static void notifyLoadFailed(BaseAd ad, String reason) {
        if (strategyBridge != null) {
            strategyBridge.onLoadFailed(ad, reason);
        }
    }

    private static void notifyShowStarted(BaseAd ad) {
        if (strategyBridge != null) {
            strategyBridge.onShowStarted(ad);
        }
    }

    private static void notifyShowFinished(BaseAd ad) {
        if (strategyBridge != null) {
            strategyBridge.onShowFinished(ad);
        }
    }

    private static void parseAds(JSONArray adArray, AdType adType) {
        try {
            for (int i = 0; i < adArray.length(); i++) {
                JSONObject adConfig = adArray.getJSONObject(i);
                String platform = adConfig.getString("platform");
                String adUnit = adConfig.optString("adUnit");

                BaseAd ad = AdPlatformFactory.createAd(platform, adType);
                if (ad == null) {
                    Log.d(TAG, "unsupported ad " + platform + " " + adUnit);
                    continue;
                }
                if (adLoadController != null) {
                    adLoadController.prepare(ad, adType);
                }
                ad.init(adType, adConfig, revenueCallback, context);
                if (adLoadController != null) {
                    adLoadController.add(ad);
                }
                Log.d(TAG, String.format("Created ad instance: %s for platform: %s, adUnit: %s", ad.getClass(), platform, adUnit));
            }
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to parse %s ads", adType.toString()), e);
        }
    }
}
