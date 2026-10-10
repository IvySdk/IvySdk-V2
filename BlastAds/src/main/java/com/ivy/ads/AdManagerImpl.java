package com.ivy.ads;

import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;

import com.ivy.adstrategy.AdStrategyConfig;
import com.ivy.adstrategy.runtime.PreviousProcessExitGuard;
import com.ivy.base.IUnProguard;
import com.ivy.base.ab.IAbManager;
import com.ivy.base.ad.AdType;
import com.ivy.base.ad.IAdListener;
import com.ivy.base.ad.IAdManager;
import com.ivy.base.grid.GridManager;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.track.EventParams;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;
import com.ivy.base.track.ITrack;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * AdManager 的实现类，桥接到 AdCore
 */
public class AdManagerImpl implements IAdManager, InitCallback, IUnProguard {
    private static final String TAG = "hoho";
    private static final long ONE_DAY_MILLIS = 86400000L;

    private Activity context;
    private ITrack tracker;
    private IAdListener adListener;
    private boolean isSetup = false;
    private MyAdListener videoListener;
    private MyAdListener interListener;
    private MyAdListener bannerListener;
    private String platformType;
    private String strategyGroup = "legacy";
    private String previousExitReason;
    private BlastAdStrategyBridge strategyBridge;
    private volatile InterstitialCdConfig interstitialCdConfig;
    private volatile long lastInterstitialShowTime;

    private IPlatformInit _maxPlatform = null;


    @Override
    public void setup(Activity context, ITrack tracker, IAbManager abManager, IAdListener adListener, boolean debug) {
        this.context = context;
        this.tracker = tracker;
        this.adListener = adListener;
        initPlatforms(debug);
        Log.d(TAG, "AdManagerImpl setup end");
    }

    private void initPlatforms(boolean debug) {
        try {
//            String bxId = LocalStorage.Instance.decodeString("bxId", "default");
            String lastExp = LocalStorage.Instance.decodeString("_last_exp_", "");
            String adsConfig = GridManager.getInstance().getGridConfig("ads", "{}");

            JSONObject config = new JSONObject(adsConfig);
            boolean sampledUser;
            if (LocalStorage.Instance.containsKey("_sampled_user_")){
                sampledUser = LocalStorage.Instance.decodeBoolean("_sampled_user_", false);
            } else {
                sampledUser = config.optDouble("sample_rate", 1.0) > Math.random();
                LocalStorage.Instance.encodeBoolean("_sampled_user_", sampledUser);
            }
            if(debug){
                sampledUser = true;
            }
            Log.d("hoho", "sampled user " + sampledUser);
            this.videoListener = new MyAdListener(AdType.REWARDED, adListener, sampledUser);
            this.interListener = new MyAdListener(AdType.INTERSTITIAL, adListener, sampledUser) {
                @Override
                public void onAdClosed(boolean gotReward, String platform, String adUnit, String mediation, String segments) {
                    lastInterstitialShowTime = SystemClock.elapsedRealtime();
                    super.onAdClosed(gotReward, platform, adUnit, mediation, segments);
                }
            };
            this.bannerListener = new MyAdListener(AdType.BANNER, adListener, sampledUser);
            String testBxId = BxSelector.getTestBxId(debug);
            boolean usingTestBxId = !testBxId.isEmpty();
            String bxId = usingTestBxId ? testBxId : BxSelector.pickBx(config);

            JSONObject ads;
            if (config.has(bxId)) {
                ads = config.getJSONObject(bxId);
            } else {
                ads = config.getJSONObject("default");
                bxId = "default";
            }
            configureInterstitialCooldown(ads.optJSONObject("inter_cd"));
            strategyGroup = pickStrategyGroup(config, ads);
            String loadMode = ads.optString("loadMode", config.optString("loadMode", "parallel"));
            if (!"parallel".equals(loadMode) && !"serial".equals(loadMode)) {
                throw new IllegalArgumentException("Unsupported ads loadMode: " + loadMode);
            }
            boolean serialLoading = "serial".equals(loadMode);
            Log.d(TAG, "ad strategy group " + strategyGroup);
            AdStrategyConfig strategyConfig = buildStrategyConfig(config, ads);

            String oldbx = LocalStorage.Instance.decodeString("_bx_id_", "");
            tracker.setUserProperty("old_exp_name", lastExp);
            lastExp = LocalStorage.Instance.decodeString("_last_exp_", "");
            tracker.setUserProperty("exp_name", lastExp);
            tracker.setUserProperty("old_bx", oldbx != null ? oldbx : "");
            LocalStorage.Instance.encodeString("_old_bx_id_", oldbx);
            LocalStorage.Instance.encodeString("_bx_id_", bxId);
            tracker.setUserProperty("bx", bxId);
            AdValueTagProcessor.getInstance().setTrack(tracker);
            Log.d("hoho", "bxid " + bxId + " exp " + lastExp);

            if (shouldBlockAdsForPreviousExit(strategyConfig)) {
                return;
            }

            Set<String> platforms = AdCore.init(context, ads, bannerListener, interListener, videoListener, serialLoading);
            if (isSchedulerGroup()) {
                Log.d(TAG, "init scheduler strategy bridge");
                strategyBridge = new BlastAdStrategyBridge(context, adListener, strategyConfig, serialLoading);
                AdCore.setStrategyBridge(strategyBridge);
            } else {
                Log.d(TAG, "use legacy ad strategy");
                strategyBridge = null;
                AdCore.setStrategyBridge(null);
            }

            platformType = platforms.contains("admob") ? "admob"
                    : platforms.contains("yandex") ? "yandex"
                    : "max";
            JSONObject ps = config.getJSONObject("platforms");
            for (String platform : platforms) {
                try {
                    // 根据平台名称生成对应的初始化类名
                    IPlatformInit platformInit = AdPlatformFactory.createInitializer(platform);
                    if ("max".equals(platform) && platformInit != null) {
                        _maxPlatform = platformInit;
                    }
                    if (platformInit == null) {
                        Log.w(TAG, "No initialization class found for platform: " + platform);
                    } else {
                        JSONObject c = ps.optJSONObject(platform);
                        if (isSchedulerGroup() && strategyBridge != null) {
                            Log.d(TAG, "schedule platform init via Gate: " + platform);
                            strategyBridge.schedulePlatformInit(context, platform, c, debug, platformInit, this);
                        } else {
                            platformInit.init(context, c, debug, this);
                            Log.d(TAG, String.format("Successfully initialized platform %s with %s, debug %s", platform, platformInit.getClass(), debug));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            videoListener.resetEventParams(platformType);
            interListener.resetEventParams(platformType);
            bannerListener.resetEventParams(platformType);

            Map<String, Object> params = new HashMap<>();
            params.put(EventParams.EVENT_PARAM_FLOW_SEQ, "01");
            params.put(EventParams.EVENT_PARAM_AD_NETWORK, platformType);
            params.put("ad_strategy_group", strategyGroup);
            if (previousExitReason != null) {
                params.put("startup_block_reason", previousExitReason);
            }
            adListener.logEvent("sdk_ads_init", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params, false);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("hoho", "AdManagerImpl setup with error: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onInitialized(String countryCode, String platform) {
        if (countryCode != null && !countryCode.isEmpty()) {
            tracker.setUserProperty("max_country_code", countryCode);
        }
        if (isSchedulerGroup()) {
            if (strategyBridge != null) {
                Log.d(TAG, "scheduler group platform initialized " + platform + ", start scheduler");
                strategyBridge.onPlatformInitialized(platform);
                strategyBridge.start();
            } else {
                AdCore.load(AdType.BANNER, platform);
            }
        } else {
            Log.d(TAG, "legacy group initial load platform " + platform);
            AdCore.onPlatformInitialized(platform);
            AdCore.load(AdType.BANNER, platform);
            AdCore.load(AdType.INTERSTITIAL, platform);
            AdCore.load(AdType.REWARDED, platform);
        }
        if (!isSetup){
            isSetup = true;
            Map<String, Object> params1 = new HashMap<>();
            params1.put(EventParams.EVENT_PARAM_FLOW_SEQ, "02");
            params1.put(EventParams.EVENT_PARAM_AD_NETWORK, platformType);
            params1.put("ad_strategy_group", strategyGroup);
            if (previousExitReason != null) {
                params1.put("startup_block_reason", previousExitReason);
            }
            adListener.logEvent("sdk_ads_complete", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params1, false);
            adListener.onInited();
        }
    }

    @Override
    public boolean isAvailable(AdType adType) {
        if (!isSetup) {
            Log.w(TAG, "AdManager not setup yet");
            return false;
        }

        return AdCore.isReady(adType);
    }

    @Override
    public boolean isInterstitialAdAllowed() {
        return !isCoolDown();
    }

    @Override
    public void loadAd(AdType adType) {
        if (!isSetup) {
            Log.w(TAG, "AdManager not setup yet");
            return;
        }

        if (isSchedulerGroup() && adType != AdType.BANNER) {
            Log.d(TAG, "scheduler group ignores manual loadAd " + adType);
        } else {
            AdCore.load(adType, null);
        }
    }

    @Override
    public void showAd(AdType adType, String tag, String placement, String clientInfo) {
        if (!isSetup) {
            Log.w(TAG, "AdManager not setup yet");
            return;
        }
        if (adType == AdType.INTERSTITIAL) {
            if (isCoolDown()) {
                Log.d(TAG, "show interstitial ad blocked by cooldown");
                return;
            }
        }
        MyAdListener listener = adType == AdType.INTERSTITIAL ? interListener : videoListener;
        if (listener == null){
            return;
        }
        listener.onTryingShow();
        if (isAvailable(adType)) {
            listener.setupPlacementInfo(tag, placement, clientInfo);
            AdCore.showAd(adType);
        } else {
            listener.onAdShowFailedNotReady(adType, placement);
        }
    }

    @Override
    public void showBannerAd(int position, String tag, String placement, String clientInfo) {
        if (bannerListener == null){
            return;
        }
        bannerListener.setupPlacementInfo(tag, placement, clientInfo);
        AdCore.showBanner(position);
    }

    @Override
    public void closeBannerAd(String placement) {
        AdCore.closeBanner();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "AdManager onResume");
        AdCore.checkSerialInventory();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "AdManager onPause");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "AdManager onDestroy");

        // 清理资源
        this.context = null;
        this.tracker = null;
        this.adListener = null;
        this.isSetup = false;
        this.interstitialCdConfig = null;
        this.lastInterstitialShowTime = 0;
        AdCore.destroySerialAds();
        if (strategyBridge != null) {
            strategyBridge.stop();
            strategyBridge = null;
            AdCore.setStrategyBridge(null);
        }

        // 如果 AdCore 有清理方法，可以在这里调用
        // AdCore.cleanup();
    }

    @Override
    public float getPrice(AdType adType) {
        if (!isSetup) {
            Log.w(TAG, "AdManager not setup yet");
            return 0.0f;
        }

        // 获取指定类型中价格最高的广告价格
        BaseAd ad = AdCore.getHighestPriceAd(adType);
        return ad != null ? (float) ad.getPrice() : 0.0f;
    }

    @Override
    public void logEvent(String eventName, Map<String, Object> map) {
        if (_maxPlatform != null){
            _maxPlatform.logEvent(eventName, map);
        }
    }

    private void configureInterstitialCooldown(JSONObject config) {
        interstitialCdConfig = null;
        lastInterstitialShowTime = 0;
        if (config == null) {
            return;
        }

        JSONArray days = config.optJSONArray("days");
        JSONArray cooldowns = config.optJSONArray("cd");
        if (days == null || cooldowns == null) {
            return;
        }

        int size = Math.min(days.length(), cooldowns.length());
        long[] parsedDays = new long[size];
        long[] parsedCooldowns = new long[size];
        int validCount = 0;
        for (int i = 0; i < size; i++) {
            long day = days.optLong(i, -1);
            long cooldown = cooldowns.optLong(i, -1);
            if (day < 0 || cooldown < 0) {
                continue;
            }
            parsedDays[validCount] = day;
            parsedCooldowns[validCount] = cooldown;
            validCount++;
        }
        if (validCount == 0) {
            return;
        }

        long[] validDays = new long[validCount];
        long[] validCooldowns = new long[validCount];
        System.arraycopy(parsedDays, 0, validDays, 0, validCount);
        System.arraycopy(parsedCooldowns, 0, validCooldowns, 0, validCount);
        interstitialCdConfig = new InterstitialCdConfig(validDays, validCooldowns);
    }

    private boolean isCoolDown() {
        InterstitialCdConfig config = interstitialCdConfig;
        if (config == null || lastInterstitialShowTime == 0) {
            return false;
        }

        long installDays = getInstallDays();
        long matchedDay = -1;
        long cooldownSeconds = 0;
        for (int i = 0; i < config.days.length; i++) {
            if (config.days[i] <= installDays && config.days[i] >= matchedDay) {
                matchedDay = config.days[i];
                cooldownSeconds = config.cooldowns[i];
            }
        }

        long elapsed = SystemClock.elapsedRealtime() - lastInterstitialShowTime;
        Log.d(TAG, "cooldown " + cooldownSeconds + " elapsed " + elapsed/1000L);
        return elapsed < cooldownSeconds * 1000L;
    }

    private long getInstallDays() {
        long firstInstallTime = LocalStorage.Instance.decodeLong("_sdk_first_time_", 0L);
        if (firstInstallTime <= 0) {
            return 0;
        }
        long duration = System.currentTimeMillis() - firstInstallTime;
        return duration > 0 ? duration / ONE_DAY_MILLIS : 0;
    }

    private static final class InterstitialCdConfig {
        private final long[] days;
        private final long[] cooldowns;

        private InterstitialCdConfig(long[] days, long[] cooldowns) {
            this.days = days;
            this.cooldowns = cooldowns;
        }
    }

    private boolean isSchedulerGroup() {
        return "new".equalsIgnoreCase(strategyGroup);
    }

    private String pickStrategyGroup(JSONObject rootConfig, JSONObject adsConfig) {
        return adsConfig.optString("strategy", rootConfig.optString("strategy", "origin"));
    }

    private AdStrategyConfig buildStrategyConfig(JSONObject rootConfig, JSONObject adsConfig) {
        JSONObject c = adsConfig.optJSONObject("adStrategy");
        if (c == null) {
            c = rootConfig.optJSONObject("adStrategy");
        }
        AdStrategyConfig config = new AdStrategyConfig();
        if (c == null) {
            return config;
        }
        config.setLaunchPeriodMs(c.optLong("launchPeriodMs", config.getLaunchPeriodMs()));
        config.setForegroundTickIntervalMs(c.optLong("foregroundTickIntervalMs", config.getForegroundTickIntervalMs()));
        config.setStartupTickIntervalMs(c.optLong("startupTickIntervalMs", config.getStartupTickIntervalMs()));
        config.setNoFillRetryDelayMs(c.optLong("noFillRetryDelayMs", config.getNoFillRetryDelayMs()));
        config.setNetworkRetryDelayMs(c.optLong("networkRetryDelayMs", config.getNetworkRetryDelayMs()));
        config.setTimeoutRetryDelayMs(c.optLong("timeoutRetryDelayMs", config.getTimeoutRetryDelayMs()));
        config.setOtherRetryDelayMs(c.optLong("otherRetryDelayMs", config.getOtherRetryDelayMs()));
        config.setRetryCooldownMs(c.optLong("retryCooldownMs", config.getRetryCooldownMs()));
        config.setCriticalAvailableMemoryMb(c.optLong(
                "criticalAvailableMemoryMb", config.getCriticalAvailableMemoryMb()));
        config.setGateCheckIntervalMs(c.optLong(
                "gateCheckIntervalMs", config.getGateCheckIntervalMs()));
        config.setGateMaxWaitMs(c.optLong(
                "gateMaxWaitMs", config.getGateMaxWaitMs()));
        config.setGateMaxMainLagMs(c.optLong(
                "gateMaxMainLagMs", config.getGateMaxMainLagMs()));
        config.setBannerSevereStallCooldownMs(c.optLong(
                "bannerSevereStallCooldownMs", config.getBannerSevereStallCooldownMs()));
        config.setBannerSevereStallThresholdCount(c.optInt(
                "bannerSevereStallThresholdCount", config.getBannerSevereStallThresholdCount()));
        config.setMaxBannerSevereStallOccurrences(c.optInt(
                "maxBannerSevereStallOccurrences", config.getMaxBannerSevereStallOccurrences()));
        config.setPreviousExitGuardEnabled(c.optBoolean(
                "previousExitGuardEnabled", config.isPreviousExitGuardEnabled()));
        config.setPreviousExitMaxAgeMs(c.optLong(
                "previousExitMaxAgeMs", config.getPreviousExitMaxAgeMs()));
        config.setMaxRetryCount(c.optInt("maxRetryCount", config.getMaxRetryCount()));
        config.setKeepReady(com.ivy.adstrategy.AdType.REWARDED, c.optBoolean("keepReadyRewarded", c.optInt("rewardedTargetReadyCount", 1) > 0));
        config.setKeepReady(com.ivy.adstrategy.AdType.INTERSTITIAL, c.optBoolean("keepReadyInterstitial", c.optInt("interstitialTargetReadyCount", 1) > 0));
        config.setKeepReady(com.ivy.adstrategy.AdType.BANNER, c.optBoolean("keepReadyBanner", c.optInt("bannerTargetReadyCount", 0) > 0));
        return config;
    }

    private boolean shouldBlockAdsForPreviousExit(AdStrategyConfig config) {
        previousExitReason = null;
        if (!config.isPreviousExitGuardEnabled()) {
            return false;
        }
        PreviousProcessExitGuard.Decision decision = PreviousProcessExitGuard.checkAndConsume(
                context, config.getPreviousExitMaxAgeMs());
        if (!decision.hasAbnormalExit()) {
            return false;
        }
        previousExitReason = decision.getReason();
        // origin 只记录异常原因，继续正常初始化广告。
        if (!isSchedulerGroup()) {
            return false;
        }
        Log.w(TAG, "skip ad initialization for recovery session, reason=" + decision.getReason());
        platformType = "none";
        videoListener.resetEventParams(platformType);
        interListener.resetEventParams(platformType);
        bannerListener.resetEventParams(platformType);

        Map<String, Object> initParams = new HashMap<>();
        initParams.put(EventParams.EVENT_PARAM_FLOW_SEQ, "01");
        initParams.put(EventParams.EVENT_PARAM_AD_NETWORK, platformType);
        initParams.put("ad_strategy_group", strategyGroup);
        initParams.put("startup_block_reason", decision.getReason());
        adListener.logEvent("sdk_ads_init", EventType.EVENT_TYPE_COMMON,
                EventSrc.EVENT_SRC_SDK, initParams, false);

        isSetup = true;
        Map<String, Object> completeParams = new HashMap<>();
        completeParams.put(EventParams.EVENT_PARAM_FLOW_SEQ, "02");
        completeParams.put(EventParams.EVENT_PARAM_AD_NETWORK, platformType);
        completeParams.put("ad_strategy_group", strategyGroup);
        completeParams.put("startup_block_reason", decision.getReason());
        adListener.logEvent("sdk_ads_complete", EventType.EVENT_TYPE_COMMON,
                EventSrc.EVENT_SRC_SDK, completeParams, false);
        adListener.onInited();
        return true;
    }
}
