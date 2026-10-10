package com.ivy.appsflyer;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.appsflyer.AFInAppEventParameterName;
import com.appsflyer.AFInAppEventType;
import com.appsflyer.AppsFlyerConversionListener;
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.attribution.AppsFlyerRequestListener;
import com.appsflyer.share.LinkGenerator;
import com.appsflyer.share.ShareInviteHelper;
import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.billing.PurchaseState;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;
import com.ivy.base.track.AbsTrack;
import com.ivy.base.track.EventIDs;
import com.ivy.base.track.EventParams;
import com.ivy.base.track.EventType;
import com.ivy.base.track.IConversationCallback;
import com.ivy.base.track.TrackPlatform;
import com.ivy.base.util.IvyUtil;
import com.ivy.base.util.Util;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
class EventAttempt {
    final String id;
    final String eventId;
    final Runnable timeout;
    EventAttempt(String id, String eventId, Runnable timeout) {
        this.id = id;
        this.eventId = eventId;
        this.timeout = timeout;
    }
}
public class AppsflyerTrack extends AbsTrack implements IUnProguard {
    private static final int MAX_CONSECUTIVE_TIMEOUTS = 2;
    private static final long CALLBACK_TIMEOUT_MS = 60000L;
    private static final long CIRCUIT_RETRY_DELAY_MS = 30000L;
    private final Map<String, Object> userProperties = new HashMap<>();
    private final AppsflyerEventOutbox outbox = new AppsflyerEventOutbox();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean hasSetFcmToken = new AtomicBoolean(false);
    private final AtomicBoolean isNetworkListenerRegistered = new AtomicBoolean(false);
    private final Runnable scheduledFlush = this::runScheduledFlush;
    private volatile EventAttempt currentAttempt;
    private boolean sendingEnabled;
    private boolean circuitOpen;
    private int consecutiveTimeouts;
    private long scheduledFlushAt;
    @Override
    public void setup(Context context, String appId, String config, boolean debug, IConversationCallback conversationCallback, Runnable readyListener) {
        super.setup(context, appId, config, debug, conversationCallback, readyListener);
        outbox.init();
        try {
            JSONObject json = new JSONObject(config);
            enableAdPing = json.optBoolean("enableAdPing", true);
            enablePurchasePing = json.optBoolean("enablePurchasePing", true);
        } catch (Exception ignored) {
        }
        enableStatus = true;
        new AppsflyerInit(context, config, debug).initAppsflyer(new AppsFlyerConversionListener() {
            @Override
            public void onConversionDataSuccess(Map<String, Object> map) {
                ILog.i(AppsflyerInit.TAG, "onConversionDataSuccess: " + map);
                if (map != null) {
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        ILog.i(AppsflyerInit.TAG, entry.getKey() + ":" + entry.getValue());
                    }
                } else {
                    ILog.i(AppsflyerInit.TAG, "onConversionDataSuccess: empty !!!");
                }
                if (conversationCallback != null) {
                    conversationCallback.onConversionDataSuccess(map);
                }
            }
            @Override
            public void onConversionDataFail(String s) {
                ILog.i(AppsflyerInit.TAG, "onConversionDataFail:" + (s != null ? s : ""));
                if (conversationCallback != null) {
                    conversationCallback.onConversionDataFail(s);
                }
            }
            @Override
            public void onAppOpenAttribution(Map<String, String> map) {
                ILog.i(AppsflyerInit.TAG, "onAppOpenAttribution: " + map);
                if (map != null) {
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        ILog.i(AppsflyerInit.TAG, entry.getKey() + ":" + entry.getValue());
                    }
                } else {
                    ILog.i(AppsflyerInit.TAG, "onAppOpenAttribution: empty !!!");
                }
                if (conversationCallback != null) {
                    conversationCallback.onAppOpenAttribution(map);
                }
            }
            @Override
            public void onAttributionFailure(String s) {
                ILog.i(AppsflyerInit.TAG, "onAttributionFailure:" + (s != null ? s : ""));
                if (conversationCallback != null) {
                    conversationCallback.onAttributionFailure(s);
                }
            }
        }, status -> {
            ILog.i(AppsflyerInit.TAG, "init result:" + status);
            if (readyListener != null) {
                readyListener.run();
            }
        });
        // initAppsflyer 返回前已经调用 start；无需等待网络响应才开始冲刷持久化事件。
        sendingEnabled = true;
        registerGlobalNetworkRecovery();
        handler.post(this::flush);
    }
    @Override
    public void setUserProperty(String key, Object value) {
        if (key == null || value == null) {
            return;
        }
        if ("customer_user_id".equals(key) || "custom_user_id".equals(key)) {
            AppsFlyerLib.getInstance().setCustomerUserId(String.valueOf(value));
            return;
        }
        synchronized (this) {
            userProperties.put(key, value);
        }
    }
    @Override
    public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms) {
        Map<String, Object> fixedParams = new HashMap<>();
        if (params != null) fixedParams.putAll(params);
        fixedParams.put(EventParams.EVENT_PARAM_EVENT_SRC, eventSrc);
        fixedParams.put(EventParams.EVENT_PARAM_ROLE_ID, Util.roleId());
        if (EventType.EVENT_TYPE_PURCHASE.equals(eventType)) {
            logPurchase(fixedParams);
        } else if (EventType.EVENT_TYPE_AD_REVENUE.equals(eventType)) {
            logAdRevenue(fixedParams);
        } else {
            logEvent(eventName, fixedParams);
        }
    }
    @Override
    public String getAppsflyerId() {
        return AppsFlyerLib.getInstance().getAppsFlyerUID(App.getInstance());
    }
    private void logEvent(String eventName, Map<String, Object> params) {
        if (!enableStatus) {
            ILog.w(AppsflyerInit.TAG, "send event:" + eventName + " failed; not initialized");
            return;
        }
        enqueueEvent(eventName, params);
    }
    private void logAdRevenue(Map<String, Object> params) {
        if (!(enableStatus && enableAdPing)) {
            ILog.w(AppsflyerInit.TAG, "send ad revenue event failed;\nenableStatus:" + enableStatus + ";enableAdPing:" + enableAdPing);
            return;
        }
        if (params.containsKey(EventParams.EVENT_PARAM_AD_MEDIATION)) {
            params.put("mediationNetwork", params.get(EventParams.EVENT_PARAM_AD_MEDIATION));
        }
        if (params.containsKey(EventParams.EVENT_PARAM_AD_NETWORK)) {
            params.put("monetizationNetwork", params.get(EventParams.EVENT_PARAM_AD_NETWORK));
        }
        if (params.containsKey(EventParams.EVENT_PARAM_REVENUE)) {
            params.put(EventParams.EVENT_PARAM_AF_REVENUE, params.get(EventParams.EVENT_PARAM_REVENUE));
        }
        if (params.containsKey(EventParams.EVENT_PARAM_CURRENCY)) {
            params.put(EventParams.EVENT_PARAM_AF_CURRENCY, params.get(EventParams.EVENT_PARAM_CURRENCY));
            params.remove(EventParams.EVENT_PARAM_CURRENCY);
        }
        enqueueEvent("af_ad_revenue", params);
    }
    private void logPurchase(Map<String, Object> params) {
        if (debug && !params.containsKey("force")) {
            ILog.w(AppsflyerInit.TAG, "debug mode! stop send purchase event");
            return;
        }
        if (!(enableStatus && enablePurchasePing)) {
            ILog.w(AppsflyerInit.TAG, "send purchase event failed;\nenableStatus:" + enableStatus + ";enablePurchasePing:" + enablePurchasePing);
            return;
        }
        Object _state = params.get(EventParams.EVENT_PARAM_PAY_STATE);
        if (_state != null) {
            int state = (int) _state;
            if (PurchaseState.PRE_ORDER == state) {
                logEvent(EventIDs.IAP_PRE_ORDER, params);
            } else if (PurchaseState.VERIFICATION == state) {
                logEvent(EventIDs.IAP_VERIFICATION, params);
            } else if (PurchaseState.PAY_RESULT == state) {
                if (params.get("sdk_state") != null && (Integer) params.get("sdk_state") == 1) {
                    if (params.containsKey("price_amount"))
                        params.put(AFInAppEventParameterName.REVENUE, params.get("price_amount"));
                    if (params.containsKey("sku"))
                        params.put(AFInAppEventParameterName.CONTENT_ID, params.get("sku"));
                    if (params.containsKey("type"))
                        params.put(AFInAppEventParameterName.CONTENT_TYPE, params.get("type"));
                    if (params.containsKey("currency"))
                        params.put(AFInAppEventParameterName.CURRENCY, params.get("currency"));
                    enqueueEvent(AFInAppEventType.PURCHASE, params);
                }
            }
        }
    }
    private void enqueueEvent(String eventName, Map<String, Object> params) {
        if (TextUtils.isEmpty(eventName)) {
            return;
        }
        Map<String, Object> eventParams = params != null
                ? new HashMap<>(params) : new HashMap<>();
        eventParams.put("net_state", NetworkMonitor.getNetworkState());
        String protectedEventId = currentAttempt != null ? currentAttempt.eventId : null;
        PendingEvent pendingEvent = outbox.enqueue(eventName, eventParams, protectedEventId);
        if (pendingEvent == null) {
            return;
        }
        if (debug) {
            ILog.i(AppsflyerInit.TAG, "cache event:" + pendingEvent.name + ", id:"
                    + pendingEvent.id + ", params:" + pendingEvent.params);
        }
        handler.post(() -> {
            if (EventIDs.SDK_FIRST_APP_OPEN.equals(eventName)
                    || EventIDs.EVENT_APP_OPEN.equals(eventName)) {
                resetCircuit();
            }
            flush();
        });
    }
    private void flush() {
        if (!sendingEnabled || circuitOpen || currentAttempt != null) {
            return;
        }
        if (!NetworkMonitor.canAttemptConnection()) {
            scheduleFlushAt(System.currentTimeMillis() + CIRCUIT_RETRY_DELAY_MS);
            return;
        }
        long now = System.currentTimeMillis();
        PendingEvent nextEvent = outbox.next(now);
        long earliestRetryAt = nextEvent == null ? outbox.earliestRetryAt(now) : 0L;
        if (nextEvent == null) {
            if (earliestRetryAt > 0L) {
                scheduleFlushAt(earliestRetryAt);
            }
            return;
        }
        sendEvent(nextEvent);
    }
    private void sendEvent(PendingEvent event) {
        Map<String, Object> params = event.params;
        String attemptId = UUID.randomUUID().toString();
        long networkGeneration = NetworkMonitor.getNetworkGeneration();
        Runnable timeout = () -> handleEventTimeout(event.id, attemptId);
        currentAttempt = new EventAttempt(attemptId, event.id, timeout);
        handler.postDelayed(timeout, CALLBACK_TIMEOUT_MS);
        try {
            synchronized (this) {
                params.putAll(userProperties);
            }
            updateUninstallToken(params);
            try {
                String appsflyerId = getAppsflyerId();
                if (!TextUtils.isEmpty(appsflyerId)) {
                    params.put("appsflyer_id", appsflyerId);
                }
            } catch (Throwable ignored) {
            }
            if (debug) {
                ILog.i(AppsflyerInit.TAG, "start send:" + event.name + ", retry:" + event.retryCount
                        + ", params:" + params);
            }
            String testEventName = AFInAppEventType.PURCHASE.equals(event.name)
                    || "af_ad_revenue".equals(event.name) ? event.name : "事件:" + event.name;
            TestManager.getInstance().addEvent("Appsflyer", testEventName, EventState.NORMAL, params);
            AppsFlyerLib.getInstance().logEvent(context, event.name, params, new AppsFlyerRequestListener() {
                @Override
                public void onSuccess() {
                    handler.post(() -> handleEventSuccess(event.id, attemptId, networkGeneration));
                }
                @Override
                public void onError(int errorCode, String errorDesc) {
                    handler.post(() -> handleEventError(event.id, attemptId, errorCode, errorDesc));
                }
            });
        } catch (Throwable throwable) {
            if (debug) {
                ILog.e(AppsflyerInit.TAG, event.name + " send exception:"
                        + throwable.getMessage());
            }
            handler.post(() -> handleEventError(event.id, attemptId, 50, throwable.getMessage()));
        }
    }
    private void handleEventSuccess(String eventId, String attemptId, long networkGeneration) {
        NetworkMonitor.markAppsFlyerReachable(networkGeneration);
        boolean current = finishAttemptIfMatched(eventId, attemptId);
        PendingEvent event = outbox.remove(eventId);
        if (debug && event != null) {
            ILog.i(AppsflyerInit.TAG, event.name + " send success");
        }
        consecutiveTimeouts = 0;
        if (circuitOpen) {
            resetCircuit();
        }
        if (current || currentAttempt == null) {
            flush();
        }
    }
    private void handleEventError(String eventId, String attemptId, int errorCode, String errorDesc) {
        if (!finishAttemptIfMatched(eventId, attemptId)) {
            return;
        }
        PendingEvent event = outbox.markForRetry(eventId);
        consecutiveTimeouts = 0;
        if (debug) {
            ILog.e(AppsflyerInit.TAG, (event != null ? event.name : eventId) + " sent failed:"
                    + errorCode + ";" + errorDesc);
        }
        if (errorCode == 40 || !NetworkMonitor.isNetworkAvailable()) {
            openCircuit("network error");
        } else {
            flush();
        }
    }
    private void updateUninstallToken(Map<String, Object> params) {
        try {
            if (hasSetFcmToken.get() || params == null || !params.containsKey("fb_push_token")) {
                return;
            }
            String token = String.valueOf(params.get("fb_push_token"));
            if (!TextUtils.isEmpty(token) && hasSetFcmToken.compareAndSet(false, true)) {
                AppsFlyerLib.getInstance().updateServerUninstallToken(App.getInstance(), token);
            }
        } catch (Throwable ignored) {
        }
    }
    private void handleEventTimeout(String eventId, String attemptId) {
        if (!finishAttemptIfMatched(eventId, attemptId)) {
            return;
        }
        PendingEvent event = outbox.markForRetry(eventId);
        consecutiveTimeouts++;
        if (debug) {
            ILog.e(AppsflyerInit.TAG, (event != null ? event.name : eventId)
                    + " callback timeout, consecutive:" + consecutiveTimeouts);
        }
        if (!NetworkMonitor.isNetworkAvailable()
                || consecutiveTimeouts >= MAX_CONSECUTIVE_TIMEOUTS) {
            openCircuit("callbacks timed out");
        } else {
            flush();
        }
    }
    private boolean finishAttemptIfMatched(String eventId, String attemptId) {
        EventAttempt attempt = currentAttempt;
        if (attempt == null || !attempt.eventId.equals(eventId) || !attempt.id.equals(attemptId)) {
            return false;
        }
        handler.removeCallbacks(attempt.timeout);
        currentAttempt = null;
        return true;
    }
    private void openCircuit(String reason) {
        circuitOpen = true;
        ILog.w(AppsflyerInit.TAG, "event sender paused:" + reason);
        handler.removeCallbacks(scheduledFlush);
        scheduledFlushAt = System.currentTimeMillis() + CIRCUIT_RETRY_DELAY_MS;
        handler.postDelayed(scheduledFlush, CIRCUIT_RETRY_DELAY_MS);
    }
    private void resetCircuit() {
        circuitOpen = false;
        consecutiveTimeouts = 0;
        handler.removeCallbacks(scheduledFlush);
        scheduledFlushAt = 0L;
    }
    private void scheduleFlushAt(long timeMillis) {
        if (circuitOpen) {
            return;
        }
        if (scheduledFlushAt > 0L && scheduledFlushAt <= timeMillis) {
            return;
        }
        handler.removeCallbacks(scheduledFlush);
        scheduledFlushAt = timeMillis;
        handler.postDelayed(scheduledFlush, Math.max(0L, timeMillis - System.currentTimeMillis()));
    }
    private void runScheduledFlush() {
        scheduledFlushAt = 0L;
        if (circuitOpen) {
            circuitOpen = false;
            consecutiveTimeouts = 0;
        }
        flush();
    }
    private void registerGlobalNetworkRecovery() {
        if (isNetworkListenerRegistered.compareAndSet(false, true)) {
            try {
                NetworkMonitor.registerNetworkListener(() -> handler.post(() -> {
                    ILog.i(AppsflyerInit.TAG, "network recovered, resume event sender");
                    resetCircuit();
                    flush();
                }));
            } catch (Throwable throwable) {
                ILog.e(AppsflyerInit.TAG, "register network listener failed:" + throwable.getMessage());
            }
        }
    }
    @Override
    public void appsflyerInviteUser(String channel, String campaign, String inviterId, String inviterAppId) {
        super.appsflyerInviteUser(channel, campaign, inviterId, inviterAppId);
        if (ActivityUtil.getInstance().getActivity() != null) {
            LocalStorage.Instance.encodeString("invite_current_user_id", inviterId);
            LinkGenerator linkGenerator = ShareInviteHelper.generateInviteUrl(context);
            linkGenerator.addParameter("deep_link_sub1", inviterId);
            linkGenerator.addParameter("deep_link_sub2", inviterAppId);
            linkGenerator.setChannel("android_user_invite");
            linkGenerator.setCampaign("user_invite");
            linkGenerator.addParameter("af_sub1", inviterId);
            linkGenerator.generateLink(context, new LinkGenerator.ResponseListener() {
                @Override
                public void onResponse(String url) {
                    if (url != null) {
                        IvyUtil.systemShareText(ActivityUtil.getInstance().getActivity(), url);
                        Map<String, String> params = new HashMap<>();
                        params.put("referrerId", inviterId);
                        params.put("campaign", "user_invite");
                        params.put("channel", "android_user_invite");
                        params.put("roleId", Util.roleId());
                        ShareInviteHelper.logInvite(context, "android_user_invite", params);
                    } else {
                        ILog.e(AppsflyerInit.TAG, "generator share url failed:\n empty url");
                    }
                }
                @Override
                public void onResponseError(String error) {
                    ILog.e(AppsflyerInit.TAG, "generator share url failed:\nerr:" + error);
                }
            });
        } else {
            ILog.e(AppsflyerInit.TAG, "generator share url failed: current activity invalid");
        }
    }
    @Override
    public String getAppsflyerInviterId() {
        return LocalStorage.Instance.decodeString("af_invite_id", "");
    }
}

//package com.ivy.appsflyer;
//
//import android.content.Context;
//import android.os.Handler;
//import android.os.Looper;
//import android.text.TextUtils;
//
//import androidx.annotation.NonNull;
//
//import com.appsflyer.AFAdRevenueData;
//import com.appsflyer.AFInAppEventParameterName;
//import com.appsflyer.AFInAppEventType;
//import com.appsflyer.AFPurchaseDetails;
//import com.appsflyer.AFPurchaseType;
//import com.appsflyer.AdRevenueScheme;
//import com.appsflyer.AppsFlyerConversionListener;
//import com.appsflyer.AppsFlyerInAppPurchaseValidationCallback;
//import com.appsflyer.AppsFlyerLib;
//import com.appsflyer.MediationNetwork;
//import com.appsflyer.attribution.AppsFlyerRequestListener;
//import com.appsflyer.share.LinkGenerator;
//import com.appsflyer.share.ShareInviteHelper;
//import com.ivy.base.ActivityUtil;
//import com.ivy.base.App;
//import com.ivy.base.ILog;
//import com.ivy.base.IUnProguard;
//import com.ivy.base.billing.PurchaseState;
//import com.ivy.base.local.LocalStorage;
//import com.ivy.base.test.TestManager;
//import com.ivy.base.test.data.EventState;
//import com.ivy.base.track.AbsTrack;
//import com.ivy.base.track.EventIDs;
//import com.ivy.base.track.EventParams;
//import com.ivy.base.track.EventType;
//import com.ivy.base.track.IConversationCallback;
//import com.ivy.base.track.TrackPlatform;
//import com.ivy.base.util.IvyUtil;
//import com.ivy.base.util.Util;
//
//import org.json.JSONObject;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//class EventAttempt {
//    final String id;
//    final String eventId;
//    final Runnable timeout;
//
//    EventAttempt(String id, String eventId, Runnable timeout) {
//        this.id = id;
//        this.eventId = eventId;
//        this.timeout = timeout;
//    }
//}
//
//public class AppsflyerTrack extends AbsTrack implements IUnProguard {
//
//    private static final int MAX_CONSECUTIVE_TIMEOUTS = 2;
//    private static final long CALLBACK_TIMEOUT_MS = 60000L;
//    private static final long CIRCUIT_RETRY_DELAY_MS = 30000L;
//
//    private final Map<String, Object> userProperties = new HashMap<>();
//    private final AppsflyerEventOutbox outbox = new AppsflyerEventOutbox();
//    private final Handler handler = new Handler(Looper.getMainLooper());
//    private final AtomicBoolean hasSetFcmToken = new AtomicBoolean(false);
//    private final AtomicBoolean isNetworkListenerRegistered = new AtomicBoolean(false);
//    private final Runnable scheduledFlush = this::runScheduledFlush;
//    private volatile EventAttempt currentAttempt;
//    private boolean sendingEnabled;
//    private boolean circuitOpen;
//    private int consecutiveTimeouts;
//    private long scheduledFlushAt;
//
//    @Override
//    public void setup(Context context, String appId, String config, boolean debug, IConversationCallback conversationCallback, Runnable readyListener) {
//        super.setup(context, appId, config, debug, conversationCallback, readyListener);
//        outbox.init();
//        try {
//            JSONObject json = new JSONObject(config);
//            enableAdPing = json.optBoolean("enableAdPing", true);
//            enablePurchasePing = json.optBoolean("enablePurchasePing", true);
//        } catch (Exception ignored) {
//        }
//
//        enableStatus = true;
//        new AppsflyerInit(context, config, debug).initAppsflyer(new AppsFlyerConversionListener() {
//            @Override
//            public void onConversionDataSuccess(Map<String, Object> map) {
//                ILog.i(AppsflyerInit.TAG, "onConversionDataSuccess: " + map);
//                if (map != null) {
//                    for (Map.Entry<String, Object> entry : map.entrySet()) {
//                        ILog.i(AppsflyerInit.TAG, entry.getKey() + ":" + entry.getValue());
//                    }
//                } else {
//                    ILog.i(AppsflyerInit.TAG, "onConversionDataSuccess: empty !!!");
//                }
//                if (conversationCallback != null) {
//                    conversationCallback.onConversionDataSuccess(map);
//                }
//            }
//
//            @Override
//            public void onConversionDataFail(String s) {
//                ILog.i(AppsflyerInit.TAG, "onConversionDataFail:" + (s != null ? s : ""));
//                if (conversationCallback != null) {
//                    conversationCallback.onConversionDataFail(s);
//                }
//            }
//
//            @Override
//            public void onAppOpenAttribution(Map<String, String> map) {
//                ILog.i(AppsflyerInit.TAG, "onAppOpenAttribution: " + map);
//                if (map != null) {
//                    for (Map.Entry<String, String> entry : map.entrySet()) {
//                        ILog.i(AppsflyerInit.TAG, entry.getKey() + ":" + entry.getValue());
//                    }
//                } else {
//                    ILog.i(AppsflyerInit.TAG, "onAppOpenAttribution: empty !!!");
//                }
//                if (conversationCallback != null) {
//                    conversationCallback.onAppOpenAttribution(map);
//                }
//            }
//
//            @Override
//            public void onAttributionFailure(String s) {
//                ILog.i(AppsflyerInit.TAG, "onAttributionFailure:" + (s != null ? s : ""));
//                if (conversationCallback != null) {
//                    conversationCallback.onAttributionFailure(s);
//                }
//            }
//        }, status -> {
//            ILog.i(AppsflyerInit.TAG, "init result:" + status);
//            if (readyListener != null) {
//                readyListener.run();
//            }
//        });
//        // initAppsflyer 返回前已经调用 start；无需等待网络响应才开始冲刷持久化事件。
//        sendingEnabled = true;
//        registerGlobalNetworkRecovery();
//        handler.post(this::flush);
//    }
//
//    @Override
//    public void setUserProperty(String key, Object value) {
//        if (key == null || value == null) {
//            return;
//        }
//        if ("customer_user_id".equals(key) || "custom_user_id".equals(key)) {
//            AppsFlyerLib.getInstance().setCustomerUserId(String.valueOf(value));
//            return;
//        }
//        synchronized (this) {
//            userProperties.put(key, value);
//        }
//    }
//
//    @Override
//    public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms) {
//        Map<String, Object> fixedParams = new HashMap<>();
//        if (params != null) fixedParams.putAll(params);
//
//        fixedParams.put(EventParams.EVENT_PARAM_EVENT_SRC, eventSrc);
//        fixedParams.put(EventParams.EVENT_PARAM_ROLE_ID, Util.roleId());
//        if (EventType.EVENT_TYPE_PURCHASE.equals(eventType)) {
//            logPurchase(fixedParams);
//        } else if (EventType.EVENT_TYPE_AD_REVENUE.equals(eventType)) {
//            logAdRevenue(fixedParams);
//        } else {
//            logEvent(eventName, fixedParams);
//        }
//    }
//
//    @Override
//    public String getAppsflyerId() {
//        return AppsFlyerLib.getInstance().getAppsFlyerUID(App.getInstance());
//    }
//
//    private void logEvent(String eventName, Map<String, Object> params) {
//        if (!enableStatus) {
//            ILog.w(AppsflyerInit.TAG, "send event:" + eventName + " failed; not initialized");
//            return;
//        }
//        enqueueEvent(eventName, params);
//    }
//
//    private void logAdRevenue(Map<String, Object> params) {
//        if (!(enableStatus && enableAdPing)) {
//            ILog.w(AppsflyerInit.TAG, "send ad revenue event failed;\nenableStatus:" + enableStatus + ";enableAdPing:" + enableAdPing);
//            return;
//        }
//        enqueueEvent("af_ad_revenue", params);
//    }
//
//    private void logPurchase(Map<String, Object> params) {
//        if (debug && !params.containsKey("force")) {
//            ILog.w(AppsflyerInit.TAG, "debug mode! stop send purchase event");
//            return;
//        }
//        if (!(enableStatus && enablePurchasePing)) {
//            ILog.w(AppsflyerInit.TAG, "send purchase event failed;\nenableStatus:" + enableStatus + ";enablePurchasePing:" + enablePurchasePing);
//            return;
//        }
//        Object _state = params.get(EventParams.EVENT_PARAM_PAY_STATE);
//        if (_state != null) {
//            int state = (int) _state;
//            if (PurchaseState.PRE_ORDER == state) {
//                logEvent(EventIDs.IAP_PRE_ORDER, params);
//            } else if (PurchaseState.VERIFICATION == state) {
//                logEvent(EventIDs.IAP_VERIFICATION, params);
//            } else if (PurchaseState.PAY_RESULT == state) {
//                if (params.get("sdk_state") != null && (Integer) params.get("sdk_state") == 1) {
//                    if (params.containsKey("price_amount"))
//                        params.put(AFInAppEventParameterName.REVENUE, params.get("price_amount"));
//                    if (params.containsKey("sku"))
//                        params.put(AFInAppEventParameterName.CONTENT_ID, params.get("sku"));
//                    if (params.containsKey("type"))
//                        params.put(AFInAppEventParameterName.CONTENT_TYPE, params.get("type"));
//                    if (params.containsKey("currency"))
//                        params.put(AFInAppEventParameterName.CURRENCY, params.get("currency"));
//                    enqueueEvent(AFInAppEventType.PURCHASE, params);
//                }
//            }
//        }
//    }
//
//    private void enqueueEvent(String eventName, Map<String, Object> params) {
//        if (TextUtils.isEmpty(eventName)) {
//            return;
//        }
//
//        Map<String, Object> eventParams = params != null ? new HashMap<>(params) : new HashMap<>();
//        eventParams.put("net_state", NetworkMonitor.getNetworkState());
//        String protectedEventId = currentAttempt != null ? currentAttempt.eventId : null;
//        PendingEvent pendingEvent = outbox.enqueue(eventName, eventParams, protectedEventId);
//        if (pendingEvent == null) {
//            return;
//        }
//        if (debug) {
//            ILog.i(AppsflyerInit.TAG, "cache event:" + pendingEvent.name + ", id:" + pendingEvent.id + ", params:" + pendingEvent.params);
//        }
//        handler.post(() -> {
//            if (EventIDs.SDK_FIRST_APP_OPEN.equals(eventName) || EventIDs.EVENT_APP_OPEN.equals(eventName)) {
//                resetCircuit();
//            }
//            flush();
//        });
//    }
//
//    private void flush() {
//        if (!sendingEnabled || circuitOpen || currentAttempt != null) {
//            return;
//        }
//        if (!NetworkMonitor.canAttemptConnection()) {
//            scheduleFlushAt(System.currentTimeMillis() + CIRCUIT_RETRY_DELAY_MS);
//            return;
//        }
//
//        long now = System.currentTimeMillis();
//        PendingEvent nextEvent = outbox.next(now);
//        long earliestRetryAt = nextEvent == null ? outbox.earliestRetryAt(now) : 0L;
//        if (nextEvent == null) {
//            if (earliestRetryAt > 0L) {
//                scheduleFlushAt(earliestRetryAt);
//            }
//            return;
//        }
//        sendEvent(nextEvent);
//    }
//
//    private MediationNetwork parseMediationNetwork(String src) {
//        if (TextUtils.isEmpty(src)) return null;
//        if (src.equalsIgnoreCase("admob") || src.equalsIgnoreCase("google_admob")) {
//            return MediationNetwork.GOOGLE_ADMOB;
//        } else if (src.equalsIgnoreCase("max") || src.equalsIgnoreCase("applovinmax")) {
//            return MediationNetwork.APPLOVIN_MAX;
//        } else if (src.equalsIgnoreCase("yandex")) {
//            return MediationNetwork.YANDEX;
//        } else if (src.equalsIgnoreCase("admost")) {
//            return MediationNetwork.ADMOST;
//        } else if (src.equalsIgnoreCase("appodeal")) {
//            return MediationNetwork.APPODEAL;
//        } else if (src.equalsIgnoreCase("chartboost")) {
//            return MediationNetwork.CHARTBOOST;
//        } else if (src.equalsIgnoreCase("fyber")) {
//            return MediationNetwork.FYBER;
//        } else if (src.equalsIgnoreCase("ironsource")) {
//            return MediationNetwork.IRONSOURCE;
//        } else if (src.equalsIgnoreCase("unity")) {
//            return MediationNetwork.UNITY;
//        } else if (src.equalsIgnoreCase("topon")) {
//            return MediationNetwork.TOPON;
//        } else if (src.equalsIgnoreCase("tradplus")) {
//            return MediationNetwork.TRADPLUS;
//        } else if (src.equalsIgnoreCase("topon_pte")) {
//            return MediationNetwork.TOPON_PTE;
//        }
//        return MediationNetwork.CUSTOM_MEDIATION;
//    }
//
//    private String convertAdType(String src) {
//        if (TextUtils.isEmpty(src)) return null;
//        if (src.equalsIgnoreCase("banner")) {
//            return "Banner";
//        } else if (src.equalsIgnoreCase("interstitial")) {
//            return "Interstitial";
//        } else if (src.equalsIgnoreCase("rewarded")) {
//            return "Rewarded";
//        } else if (src.equalsIgnoreCase("native")) {
//            return "NativeAd";
//        }
//        return src;
//    }
//
//    private Map<String, String> toAppsFlyerParameters(Map<String, Object> params) {
//        Map<String, String> result = new HashMap<>();
//        if (params == null) {
//            return result;
//        }
//        for (Map.Entry<String, Object> entry : params.entrySet()) {
//            String key = entry.getKey();
//            Object value = entry.getValue();
//            if (value == null) {
//                continue;
//            }
//            if ("is_auto_renewing".equals(key) || "is_acknowledged".equals(key) || "purchase_time".equals(key) || "purchase_token".equals(key) || "purchase_state".equals(key) || "signature".equals(key) || "original_json".equals(key) || "price_amount".equals(key)) {
//                continue;
//            }
//            result.put(key, value.toString());
//        }
//
//        return result;
//    }
//
//    private void sendEvent(PendingEvent event) {
//        Map<String, Object> params = event.params;
//        String attemptId = UUID.randomUUID().toString();
//        long networkGeneration = NetworkMonitor.getNetworkGeneration();
//        Runnable timeout = () -> handleEventTimeout(event.id, attemptId);
//        currentAttempt = new EventAttempt(attemptId, event.id, timeout);
//        handler.postDelayed(timeout, CALLBACK_TIMEOUT_MS);
//        try {
//            synchronized (this) {
//                params.putAll(userProperties);
//            }
//            updateUninstallToken(params);
//            try {
//                String appsflyerId = getAppsflyerId();
//                if (!TextUtils.isEmpty(appsflyerId)) {
//                    params.put("appsflyer_id", appsflyerId);
//                }
//            } catch (Throwable ignored) {
//            }
//
//            if (debug) {
//                ILog.i(AppsflyerInit.TAG, "start send:" + event.name + ", retry:" + event.retryCount + ", params:" + params);
//            }
//            String testEventName = AFInAppEventType.PURCHASE.equals(event.name) || "af_ad_revenue".equals(event.name) ? event.name : "事件:" + event.name;
//            TestManager.getInstance().addEvent("Appsflyer", testEventName, EventState.NORMAL, params);
//
//            if ("af_ad_revenue".equalsIgnoreCase(event.name)) {
//                Object mediationValue = params.get(EventParams.EVENT_PARAM_AD_NETWORK);
//                Object monetizationValue = params.get(EventParams.EVENT_PARAM_AD_MEDIATION);
//                Object revenueValue = params.get(EventParams.EVENT_PARAM_REVENUE);
//                Object currencyValue = params.get(EventParams.EVENT_PARAM_CURRENCY);
//                if (mediationValue == null || monetizationValue == null || revenueValue == null || currencyValue == null) {
//                    handler.post(() -> handleEventSuccess(event.id, attemptId, networkGeneration));
//                    return;
//                }
//
//                String mediationString = mediationValue.toString().trim();
//                String monetizationNetwork = monetizationValue.toString().trim();
//                String currency = currencyValue.toString();
//
//                double revenue;
//                try {
//                    revenue = Double.parseDouble(revenueValue.toString());
//                } catch (NumberFormatException e) {
//                    handler.post(() -> handleEventSuccess(event.id, attemptId, networkGeneration));
//                    return;
//                }
//
//                if (!Double.isFinite(revenue) || revenue <= 0) {
//                    handler.post(() -> handleEventSuccess(event.id, attemptId, networkGeneration));
//                    return;
//                }
//
//                MediationNetwork mediationNetwork = parseMediationNetwork(mediationString);
//                if (mediationNetwork == null) {
//                    handler.post(() -> handleEventSuccess(event.id, attemptId, networkGeneration));
//                    return;
//                }
//
//                Map<String, Object> additionalParams = new HashMap<>(params);
//                additionalParams.remove(EventParams.EVENT_PARAM_CURRENCY);
//
//                Object adFormat = additionalParams.get(EventParams.EVENT_PARAM_AD_FORMAT);
//                if (adFormat != null) {
//                    String adType = convertAdType(adFormat.toString());
//                    if (TextUtils.isEmpty(adType)) {
//                        handler.post(() -> handleEventSuccess(event.id, attemptId, networkGeneration));
//                        return;
//                    }
//                    additionalParams.put(AdRevenueScheme.AD_TYPE, adType);
//                }
//
//                AFAdRevenueData adRevenueData = new AFAdRevenueData(monetizationNetwork, mediationNetwork, currency, revenue);
//                AppsFlyerLib.getInstance().logAdRevenue(adRevenueData, additionalParams);
//                handler.post(() -> handleEventSuccess(event.id, attemptId, networkGeneration));
//                return;
//            }
//
//            if (event.name.equalsIgnoreCase("af_purchase")) {
//                Object payChannelValue = event.params.get(EventParams.EVENT_PARAM_PAY_CHANNEL);
//                if (payChannelValue != null && "google_pay".equalsIgnoreCase(payChannelValue.toString())) {
//                    Object o_original_json = event.params.get("original_json");
//                    if (o_original_json == null) {
//                        handler.post(() -> handleEventSuccess(event.id, attemptId, networkGeneration));
//                        return;
//                    }
//                    try {
//                        JSONObject purchaseJson = new JSONObject(o_original_json.toString());
//                        String purchaseToken = purchaseJson.optString("purchaseToken", null);
//                        String productId = purchaseJson.optString("productId", null);
//
//                        if (TextUtils.isEmpty(purchaseToken) || TextUtils.isEmpty(productId)) {
//                            handler.post(() -> handleEventSuccess(event.id, attemptId, networkGeneration));
//                            return;
//                        }
//                        Object purchaseTypeValue = event.params.get("type");
//                        AFPurchaseType purchaseType;
//                        if (purchaseTypeValue != null && "subs".equalsIgnoreCase(purchaseTypeValue.toString())) {
//                            purchaseType = AFPurchaseType.SUBSCRIPTION;
//                        } else {
//                            purchaseType = AFPurchaseType.ONE_TIME_PURCHASE;
//                        }
//                        AFPurchaseDetails purchaseDetails = new AFPurchaseDetails(purchaseType, purchaseToken, productId);
//                        Map<String, String> additionalParameters = toAppsFlyerParameters(event.params);
//                        AppsFlyerLib.getInstance().validateAndLogInAppPurchase(purchaseDetails, additionalParameters, new AppsFlyerInAppPurchaseValidationCallback() {
//                            @Override
//                            public void onInAppPurchaseValidationFinished(@NonNull Map<String, ?> map) {
//                                Object validationResult = map.get("result");
//                                boolean success = Boolean.TRUE.equals(validationResult);
//                                if (success) {
//                                    // AppsFlyer 已验证成功，自动产生 af_purchase事件
//                                    handler.post(() -> handleEventSuccess(event.id, attemptId, networkGeneration));
//                                } else {
//                                    // AppsFlyer 验证失败
//                                    handler.post(() -> handleEventSuccess(event.id, attemptId, networkGeneration));
//                                }
//                            }
//
//                            @Override
//                            public void onInAppPurchaseValidationError(@NonNull Map<String, ?> map) {
//                                // String msg = (String) map.get("error_message");
//                                handler.post(() -> handleEventError(event.id, attemptId, 1, "ValidationError"));
//                            }
//                        });
//                        return;
//                    } catch (Exception e) {
//                        handler.post(() -> handleEventSuccess(event.id, attemptId, networkGeneration));
//                        return;
//                    }
//                }
//            }
//
//            AppsFlyerLib.getInstance().logEvent(context, event.name, params, new AppsFlyerRequestListener() {
//                @Override
//                public void onSuccess() {
//                    handler.post(() -> handleEventSuccess(event.id, attemptId, networkGeneration));
//                }
//
//                @Override
//                public void onError(int errorCode, String errorDesc) {
//                    handler.post(() -> handleEventError(event.id, attemptId, errorCode, errorDesc));
//                }
//            });
//        } catch (Throwable throwable) {
//            if (debug) {
//                ILog.e(AppsflyerInit.TAG, event.name + " send exception:" + throwable.getMessage());
//            }
//            handler.post(() -> handleEventError(event.id, attemptId, 50, throwable.getMessage()));
//        }
//    }
//
//    private void handleEventSuccess(String eventId, String attemptId, long networkGeneration) {
//        NetworkMonitor.markAppsFlyerReachable(networkGeneration);
//        boolean current = finishAttemptIfMatched(eventId, attemptId);
//        PendingEvent event = outbox.remove(eventId);
//        if (debug && event != null) {
//            ILog.i(AppsflyerInit.TAG, event.name + " send success");
//        }
//        consecutiveTimeouts = 0;
//        if (circuitOpen) {
//            resetCircuit();
//        }
//        if (current || currentAttempt == null) {
//            flush();
//        }
//    }
//
//    private void handleEventError(String eventId, String attemptId, int errorCode, String errorDesc) {
//        if (!finishAttemptIfMatched(eventId, attemptId)) {
//            return;
//        }
//        PendingEvent event = outbox.markForRetry(eventId);
//        consecutiveTimeouts = 0;
//        if (debug) {
//            ILog.e(AppsflyerInit.TAG, (event != null ? event.name : eventId) + " sent failed:" + errorCode + ";" + errorDesc);
//        }
//        if (errorCode == 40 || !NetworkMonitor.isNetworkAvailable()) {
//            openCircuit("network error");
//        } else {
//            flush();
//        }
//    }
//
//    private void updateUninstallToken(Map<String, Object> params) {
//        try {
//            if (hasSetFcmToken.get() || params == null || !params.containsKey("fb_push_token")) {
//                return;
//            }
//            String token = String.valueOf(params.get("fb_push_token"));
//            if (!TextUtils.isEmpty(token) && hasSetFcmToken.compareAndSet(false, true)) {
//                AppsFlyerLib.getInstance().updateServerUninstallToken(App.getInstance(), token);
//            }
//        } catch (Throwable ignored) {
//        }
//    }
//
//    private void handleEventTimeout(String eventId, String attemptId) {
//        if (!finishAttemptIfMatched(eventId, attemptId)) {
//            return;
//        }
//        PendingEvent event = outbox.markForRetry(eventId);
//        consecutiveTimeouts++;
//        if (debug) {
//            ILog.e(AppsflyerInit.TAG, (event != null ? event.name : eventId) + " callback timeout, consecutive:" + consecutiveTimeouts);
//        }
//        if (!NetworkMonitor.isNetworkAvailable() || consecutiveTimeouts >= MAX_CONSECUTIVE_TIMEOUTS) {
//            openCircuit("callbacks timed out");
//        } else {
//            flush();
//        }
//    }
//
//    private boolean finishAttemptIfMatched(String eventId, String attemptId) {
//        EventAttempt attempt = currentAttempt;
//        if (attempt == null || !attempt.eventId.equals(eventId) || !attempt.id.equals(attemptId)) {
//            return false;
//        }
//        handler.removeCallbacks(attempt.timeout);
//        currentAttempt = null;
//        return true;
//    }
//
//    private void openCircuit(String reason) {
//        circuitOpen = true;
//        ILog.w(AppsflyerInit.TAG, "event sender paused:" + reason);
//        handler.removeCallbacks(scheduledFlush);
//        scheduledFlushAt = System.currentTimeMillis() + CIRCUIT_RETRY_DELAY_MS;
//        handler.postDelayed(scheduledFlush, CIRCUIT_RETRY_DELAY_MS);
//    }
//
//    private void resetCircuit() {
//        circuitOpen = false;
//        consecutiveTimeouts = 0;
//        handler.removeCallbacks(scheduledFlush);
//        scheduledFlushAt = 0L;
//    }
//
//    private void scheduleFlushAt(long timeMillis) {
//        if (circuitOpen) {
//            return;
//        }
//        if (scheduledFlushAt > 0L && scheduledFlushAt <= timeMillis) {
//            return;
//        }
//        handler.removeCallbacks(scheduledFlush);
//        scheduledFlushAt = timeMillis;
//        handler.postDelayed(scheduledFlush, Math.max(0L, timeMillis - System.currentTimeMillis()));
//    }
//
//    private void runScheduledFlush() {
//        scheduledFlushAt = 0L;
//        if (circuitOpen) {
//            circuitOpen = false;
//            consecutiveTimeouts = 0;
//        }
//        flush();
//    }
//
//    private void registerGlobalNetworkRecovery() {
//        if (isNetworkListenerRegistered.compareAndSet(false, true)) {
//            try {
//                NetworkMonitor.registerNetworkListener(() -> handler.post(() -> {
//                    ILog.i(AppsflyerInit.TAG, "network recovered, resume event sender");
//                    resetCircuit();
//                    flush();
//                }));
//            } catch (Throwable throwable) {
//                ILog.e(AppsflyerInit.TAG, "register network listener failed:" + throwable.getMessage());
//            }
//        }
//    }
//
//    @Override
//    public void appsflyerInviteUser(String channel, String campaign, String inviterId, String inviterAppId) {
//        super.appsflyerInviteUser(channel, campaign, inviterId, inviterAppId);
//
//        if (ActivityUtil.getInstance().getActivity() != null) {
//            LocalStorage.Instance.encodeString("invite_current_user_id", inviterId);
//
//            LinkGenerator linkGenerator = ShareInviteHelper.generateInviteUrl(context);
//            linkGenerator.addParameter("deep_link_sub1", inviterId);
//            linkGenerator.addParameter("deep_link_sub2", inviterAppId);
//            linkGenerator.setChannel("android_user_invite");
//            linkGenerator.setCampaign("user_invite");
//            linkGenerator.addParameter("af_sub1", inviterId);
//
//            linkGenerator.generateLink(context, new LinkGenerator.ResponseListener() {
//                @Override
//                public void onResponse(String url) {
//                    if (url != null) {
//                        IvyUtil.systemShareText(ActivityUtil.getInstance().getActivity(), url);
//
//                        Map<String, String> params = new HashMap<>();
//                        params.put("referrerId", inviterId);
//                        params.put("campaign", "user_invite");
//                        params.put("channel", "android_user_invite");
//                        params.put("roleId", Util.roleId());
//
//                        ShareInviteHelper.logInvite(context, "android_user_invite", params);
//                    } else {
//                        ILog.e(AppsflyerInit.TAG, "generator share url failed:\n empty url");
//                    }
//                }
//
//                @Override
//                public void onResponseError(String error) {
//                    ILog.e(AppsflyerInit.TAG, "generator share url failed:\nerr:" + error);
//                }
//            });
//
//        } else {
//            ILog.e(AppsflyerInit.TAG, "generator share url failed: current activity invalid");
//        }
//    }
//
//    @Override
//    public String getAppsflyerInviterId() {
//        return LocalStorage.Instance.decodeString("af_invite_id", "");
//    }
//}
