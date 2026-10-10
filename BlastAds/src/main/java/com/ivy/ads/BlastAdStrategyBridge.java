package com.ivy.ads;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.ivy.adstrategy.AdStrategy;
import com.ivy.adstrategy.AdStrategyConfig;
import com.ivy.adstrategy.event.AdEvent;
import com.ivy.adstrategy.event.AdEventType;
import com.ivy.adstrategy.gate.PlatformInitializationGate;
import com.ivy.adstrategy.metrics.StrategyEvent;
import com.ivy.adstrategy.metrics.StrategyEventListener;
import com.ivy.adstrategy.policy.MemoryPolicy;
import com.ivy.adstrategy.scheduler.AdAction;
import com.ivy.adstrategy.scheduler.AdActionExecutor;
import com.ivy.adstrategy.scheduler.AdActionType;
import com.ivy.adstrategy.scheduler.AdAutoScheduler;
import com.ivy.adstrategy.scheduler.InventoryProvider;
import com.ivy.adstrategy.slot.AdSlot;
import com.ivy.adstrategy.slot.AdSlotState;
import com.ivy.adstrategy.slot.SlotStatus;
import com.ivy.base.ad.AdType;
import com.ivy.base.ad.IAdListener;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class BlastAdStrategyBridge implements InventoryProvider, AdActionExecutor, StrategyEventListener {
    private static final String TAG = "hoho";
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Map<String, BaseAd> slotAds = new HashMap<>();
    private final Map<BaseAd, String> adSlots = new HashMap<>();
    private final Set<String> initializedPlatforms = new HashSet<>();
    private final IAdListener adListener;
    private final AdStrategy strategy;
    private final boolean serialLoading;
    private AdAutoScheduler scheduler;
    private boolean started;
    private volatile boolean criticalMemoryShutdown;

    BlastAdStrategyBridge(Activity activity, IAdListener adListener, AdStrategyConfig config) {
        this(activity, adListener, config, false);
    }

    BlastAdStrategyBridge(Activity activity, IAdListener adListener, AdStrategyConfig config, boolean serialLoading) {
        this.adListener = adListener;
        this.serialLoading = serialLoading;
        this.strategy = AdStrategy.init(activity, config, this);
        this.strategy.bindHostActivity(activity);
        List<AdSlot> slots = buildSlots();
        Log.d(TAG, "BlastAdStrategyBridge create slots=" + slots.size() + ", serialLoading=" + serialLoading);
        this.scheduler = strategy.createScheduler(slots, this, this);
    }

    void schedulePlatformInit(final Activity activity, final String platform, final JSONObject config,
                              final boolean debug, final IPlatformInit platformInit, final InitCallback callback) {
        if (platformInit == null) {
            return;
        }
        PlatformInitializationGate gate = strategy.createInitializationGate(null);
        gate.evaluateAndExecute(activity, new PlatformInitializationGate.Callback() {
            @Override
            public void onAllow() {
                try {
                    Log.d(TAG, "BlastAdStrategyBridge gate ALLOW: initializing platform " + platform);
                    platformInit.init(activity, config, debug, callback);
                } catch (Throwable throwable) {
                    Log.e(TAG, "BlastAdStrategyBridge platform init error " + platform, throwable);
                }
            }

            @Override
            public void onDrop(String reason) {
                Log.w(TAG, "BlastAdStrategyBridge gate DROP: platform " + platform + ", reason=" + reason);
                if (callback != null) {
                    callback.onInitialized("", platform);
                }
            }
        });
    }

    void start() {
        if (started) {
            return;
        }
        Log.d(TAG, "BlastAdStrategyBridge start");
        started = true;
        scheduler.start();
    }

    void onPlatformInitialized(String platform) {
        if (platform == null) {
            return;
        }
        initializedPlatforms.add(platform);
        Log.d(TAG, "BlastAdStrategyBridge platform initialized " + platform);
        AdCore.onPlatformInitialized(platform);
        if (strategy.getRuntimeState().isCriticalMemoryPressure()) {
            shutdownForCriticalMemory(MemoryPolicy.CRITICAL_MEMORY_REASON);
        } else {
            AdCore.load(AdType.BANNER, platform);
            if (scheduler != null) {
                scheduler.onEvent(new AdEvent(AdEventType.TICK));
            }
        }
    }

    void stop() {
        Log.d(TAG, "BlastAdStrategyBridge stop");
        started = false;
        if (scheduler != null) {
            scheduler.stop();
        }
    }

    void onForeground() {
        if (scheduler != null) {
            scheduler.onForeground();
        }
    }

    void onBackground() {
        if (scheduler != null) {
            scheduler.onBackground();
        }
    }

    void onMemoryPressure() {
        if (scheduler != null) {
            scheduler.onMemoryPressure();
        }
    }

    void releaseAll(String reason) {
        if (scheduler != null) {
            scheduler.releaseAll(reason);
        }
    }

    void shutdownForCriticalMemory(final String reason) {
        if (criticalMemoryShutdown) {
            return;
        }
        criticalMemoryShutdown = true;
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                AdCore.destroyAllAds(reason);
            }
        });
    }

    void onLoadStarted(BaseAd ad) {
        if (scheduler != null && isManagedAd(ad)) {
            scheduler.onLoadStarted(slotId(ad), ad.getAdUnit());
        }
    }

    void onLoadSuccess(BaseAd ad) {
        if (scheduler != null && isManagedAd(ad)) {
            scheduler.onLoadSuccess(slotId(ad), ad.getAdUnit());
        }
    }

    void onLoadFailed(BaseAd ad, String reason) {
        if (scheduler != null && isManagedAd(ad)) {
            if (serialLoading && (ad.getAdType() == AdType.INTERSTITIAL || ad.getAdType() == AdType.REWARDED)) {
                AdLoader loader = AdCore.adLoadController != null ? AdCore.adLoadController.getLoader(ad.getAdType()) : null;
                if (loader instanceof SerialAdLoader) {
                    SerialAdLoader serialLoader = (SerialAdLoader) loader;
                    if (serialLoader.isAnyLoading()) {
                        Log.d(TAG, "SerialAdLoader still attempting other tiers, skip slot-level failure");
                        return;
                    }
                }
            }
            scheduler.onLoadFailed(slotId(ad), ad.getAdUnit(), reason);
        }
    }

    void onShowStarted(BaseAd ad) {
        if (scheduler != null && isManagedAd(ad) && shouldTrackFullscreenShowing(ad)) {
            scheduler.onShowStarted(slotId(ad));
        }
    }

    void onShowFinished(BaseAd ad) {
        if (scheduler != null && isManagedAd(ad) && shouldTrackFullscreenShowing(ad)) {
            scheduler.onShowFinished(slotId(ad));
        }
    }

    @Override
    public List<AdSlotState> getSlotStates() {
        List<AdSlotState> states = new ArrayList<>();
        if (criticalMemoryShutdown) {
            return states;
        }
        if (serialLoading) {
            checkAndAddSerialSlotState(states, AdType.REWARDED, "serial_rewarded");
            checkAndAddSerialSlotState(states, AdType.INTERSTITIAL, "serial_interstitial");
            for (Map.Entry<String, BaseAd> entry : slotAds.entrySet()) {
                BaseAd ad = entry.getValue();
                if (ad.getAdType() == AdType.BANNER) {
                    int readyCount = ad.isReady() ? 1 : 0;
                    int loadingCount = ad.isLoading ? 1 : 0;
                    SlotStatus status = readyCount > 0 ? SlotStatus.READY : loadingCount > 0 ? SlotStatus.LOADING : SlotStatus.IDLE;
                    states.add(new AdSlotState(
                            entry.getKey(),
                            toStrategyAdType(ad.getAdType()),
                            readyCount,
                            loadingCount,
                            ad.getLoadFailedCount(),
                            0L,
                            0L,
                            status
                    ));
                }
            }
        } else {
            for (Map.Entry<String, BaseAd> entry : slotAds.entrySet()) {
                BaseAd ad = entry.getValue();
                int readyCount = ad.isReady() ? 1 : 0;
                int loadingCount = ad.isLoading ? 1 : 0;
                SlotStatus status = readyCount > 0 ? SlotStatus.READY : loadingCount > 0 ? SlotStatus.LOADING : SlotStatus.IDLE;
                states.add(new AdSlotState(
                        entry.getKey(),
                        toStrategyAdType(ad.getAdType()),
                        readyCount,
                        loadingCount,
                        ad.getLoadFailedCount(),
                        0L,
                        0L,
                        status
                ));
            }
        }
        Log.d(TAG, "BlastAdStrategyBridge inventory size=" + states.size());
        return states;
    }

    private void checkAndAddSerialSlotState(List<AdSlotState> states, AdType adType, String slotId) {
        List<BaseAd> ads = AdCore.getAds(adType);
        if (ads == null || ads.isEmpty()) {
            return;
        }
        int readyCount = 0;
        int loadingCount = 0;
        int failedCount = 0;
        for (BaseAd ad : ads) {
            if ("promotion".equals(ad.getPlatform())) {
                continue;
            }
            if (ad.isReady()) {
                readyCount++;
            } else if (ad.isLoading) {
                loadingCount++;
            }
            failedCount = Math.max(failedCount, ad.getLoadFailedCount());
        }
        SlotStatus status = readyCount > 0 ? SlotStatus.READY : loadingCount > 0 ? SlotStatus.LOADING : SlotStatus.IDLE;
        states.add(new AdSlotState(
                slotId,
                toStrategyAdType(adType),
                readyCount,
                loadingCount,
                failedCount,
                0L,
                0L,
                status
        ));
    }

    @Override
    public void execute(final AdAction action) {
        if (action == null) {
            return;
        }
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (serialLoading && ("serial_interstitial".equals(action.getSlotId()) || "serial_rewarded".equals(action.getSlotId()))) {
                    handleSerialAction(action);
                    return;
                }
                BaseAd ad = slotAds.get(action.getSlotId());
                if (ad == null) {
                    Log.w(TAG, "missing slot ad " + action.getSlotId());
                    return;
                }
                if (action.getType() != AdActionType.KEEP) {
                    Log.d(TAG, "BlastAdStrategyBridge execute action=" + action.getType()
                            + ", slot=" + action.getSlotId()
                            + ", adType=" + action.getAdType()
                            + ", reason=" + action.getReason()
                            + ", score=" + action.getScore());
                }
                switch (action.getType()) {
                    case KEEP:
                        if (ad.getAdType() == AdType.BANNER && "not_preloadable".equals(action.getReason())) {
                            AdCore.restoreSchedulerBanner();
                        }
                        break;
                    case LOAD:
                        if (!initializedPlatforms.contains(ad.getPlatform())) {
                            Log.d(TAG, "skip load before platform initialized " + ad.getPlatform() + " " + ad.getAdUnit());
                            return;
                        }
                        ad.load(AdCore.getAdLoadCallback());
                        break;
                    case RELEASE:
                        if (ad.getAdType() == AdType.BANNER) {
                            AdCore.suspendBannerByScheduler();
                        } else {
                            ad.close();
                        }
                        break;
                    case DISABLE:
                        if (ad.getAdType() == AdType.BANNER) {
                            AdCore.suspendBannerByScheduler();
                        } else if (MemoryPolicy.CRITICAL_MEMORY_REASON.equals(action.getReason())) {
                            shutdownForCriticalMemory(action.getReason());
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void handleSerialAction(AdAction action) {
        AdType type = "serial_interstitial".equals(action.getSlotId()) ? AdType.INTERSTITIAL : AdType.REWARDED;
        if (action.getType() != AdActionType.KEEP) {
            Log.d(TAG, "BlastAdStrategyBridge handleSerialAction action=" + action.getType()
                    + ", slot=" + action.getSlotId()
                    + ", adType=" + action.getAdType()
                    + ", reason=" + action.getReason());
        }
        switch (action.getType()) {
            case LOAD:
                AdLoader loader = AdCore.adLoadController != null ? AdCore.adLoadController.getLoader(type) : null;
                if (loader instanceof SerialAdLoader) {
                    ((SerialAdLoader) loader).request();
                }
                break;
            case RELEASE:
            case DISABLE:
                AdLoader currentLoader = AdCore.adLoadController != null ? AdCore.adLoadController.getLoader(type) : null;
                if (currentLoader instanceof SerialAdLoader) {
                    ((SerialAdLoader) currentLoader).releaseReadyAds();
                } else {
                    for (BaseAd ad : AdCore.getAds(type)) {
                        if (!"promotion".equals(ad.getPlatform()) && ad.isReady()) {
                            ad.close();
                        }
                    }
                }
                break;
            case KEEP:
            default:
                break;
        }
    }

    @Override
    public void onStrategyEvent(StrategyEvent event) {
        if (event != null) {
            Log.d(TAG, "BlastAdStrategyBridge event=" + event.getName() + ", adType=" + event.getAdType() + ", params=" + event.getParams());
        }
        if (adListener == null || event == null) {
            return;
        }
        Map<String, Object> params = new HashMap<>(event.getParams());
        if (event.getAdType() != null) {
            params.put("ad_type", event.getAdType().name());
        }
        adListener.logEvent(event.getName(), EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params, false);
    }

    private List<AdSlot> buildSlots() {
        List<AdSlot> slots = new ArrayList<>();
        if (serialLoading) {
            List<BaseAd> rewardAds = AdCore.getAds(AdType.REWARDED);
            if (rewardAds != null && !rewardAds.isEmpty()) {
                String slotId = "serial_rewarded";
                slots.add(new AdSlot(slotId, toStrategyAdType(AdType.REWARDED), 100, 80, 30, true));
                for (BaseAd ad : rewardAds) {
                    adSlots.put(ad, slotId);
                }
            }
            List<BaseAd> interAds = AdCore.getAds(AdType.INTERSTITIAL);
            if (interAds != null && !interAds.isEmpty()) {
                String slotId = "serial_interstitial";
                slots.add(new AdSlot(slotId, toStrategyAdType(AdType.INTERSTITIAL), 80, 70, 35, true));
                for (BaseAd ad : interAds) {
                    adSlots.put(ad, slotId);
                }
            }
            addSlots(slots, AdCore.getAds(AdType.BANNER), 20, 20, 50, false);
        } else {
            addSlots(slots, AdCore.getAds(AdType.REWARDED), 100, 80, 30);
            addSlots(slots, AdCore.getAds(AdType.INTERSTITIAL), 80, 70, 35);
            addSlots(slots, AdCore.getAds(AdType.BANNER), 20, 20, 50, false);
        }
        return slots;
    }

    private boolean shouldTrackFullscreenShowing(BaseAd ad) {
        if (ad == null) {
            return false;
        }
        AdType adType = ad.getAdType();
        return adType != AdType.BANNER;
    }

    private void addSlots(List<AdSlot> slots, List<BaseAd> ads, int priority, int revenueScore, int resourceCost) {
        addSlots(slots, ads, priority, revenueScore, resourceCost, true);
    }

    private void addSlots(List<AdSlot> slots, List<BaseAd> ads, int priority, int revenueScore, int resourceCost, boolean preloadable) {
        for (BaseAd ad : ads) {
            if ("promotion".equals(ad.getPlatform())) {
                continue;
            }
            String slotId = slotId(ad);
            slotAds.put(slotId, ad);
            adSlots.put(ad, slotId);
            Log.d(TAG, "BlastAdStrategyBridge register slot=" + slotId
                    + ", type=" + ad.getAdType()
                    + ", platform=" + ad.getPlatform()
                    + ", adUnit=" + ad.getAdUnit()
                    + ", preloadable=" + preloadable);
            slots.add(new AdSlot(slotId, toStrategyAdType(ad.getAdType()), priority, revenueScore, resourceCost, preloadable));
        }
    }

    private String slotId(BaseAd ad) {
        String existing = adSlots.get(ad);
        if (existing != null) {
            return existing;
        }
        return ad.getAdType().getValue() + ":" + ad.getPlatform() + ":" + ad.getAdUnit();
    }

    private boolean isManagedAd(BaseAd ad) {
        return ad != null && adSlots.containsKey(ad);
    }

    private com.ivy.adstrategy.AdType toStrategyAdType(AdType adType) {
        if (adType == AdType.BANNER) {
            return com.ivy.adstrategy.AdType.BANNER;
        }
        if (adType == AdType.REWARDED) {
            return com.ivy.adstrategy.AdType.REWARDED;
        }
        return com.ivy.adstrategy.AdType.INTERSTITIAL;
    }
}
