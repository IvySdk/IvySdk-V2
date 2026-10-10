package com.ivy.ads;

import android.os.SystemClock;
import android.util.Log;

import com.ivy.base.ad.AdType;
import com.ivy.base.ad.IAdListener;
import com.ivy.base.track.EventIDs;
import com.ivy.base.track.EventParams;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MyAdListener {
    private final AdType adType;
    public final IAdListener adListener;
    private final Map<String, Object> eventParams = new HashMap<>();
    private long timeStartShow = 0;
    private int sessionCount = 0;
    private long lastSessionShowTime = 0;
    private int sessionDeltaTime = 0;
    private String tag = "default";
    private String placementId = "";
    private boolean sampledUser;
    private final Map<String, Object> clientInfos = new HashMap<>();

    public MyAdListener(AdType adType, IAdListener adListener, boolean sampledUser) {
        this.adType = adType;
        this.adListener = adListener;
        this.sessionCount = 0;
        this.sampledUser = sampledUser;
        lastSessionShowTime = SystemClock.elapsedRealtime();
    }
    
    public void onTryingShow(){
        sessionCount += 1;
        long now = SystemClock.elapsedRealtime();
        sessionDeltaTime = (int)(now - lastSessionShowTime);
        lastSessionShowTime = now;
    }

    public MyAdListener setupPlacementInfo(String tag, String placement, String clientInfo) {
        this.tag = tag;
        this.placementId = placement;
        if (clientInfo != null && !clientInfo.isEmpty()) {
            try {
                JSONObject json = new JSONObject(clientInfo);
                for (int i = 0; i < json.names().length(); i++) {
                    String key = json.names().getString(i);
                    Object value = json.get(key);
                    if (value instanceof Boolean) {
                        clientInfos.put(key, ((Boolean) value) ? 1 : 0);
                    } else {
                        clientInfos.put(key, value);
                    }
                }
            } catch (Exception e) {
                Log.e("error", "format client info failed:" + e.getMessage());
            }
        } else {
            clientInfos.clear();
        }
        return this;
    }

    public void onAdLoadSuccess(String adUnit, String platform, String mediation, String segments, int deltaTime) {
//        adListener.onAdLoadSuccess(adType);
        if (sampledUser) {
            Map<String, Object> params = new HashMap<>();
            params.put(EventParams.EVENT_PARAM_FLOW_SEQ, "04");
            params.put(EventParams.EVENT_PARAM_AD_UNIT, adUnit);
            params.putAll(eventParams);
            params.put("segment", segments != null ? segments : "null");
            params.put(EventParams.EVENT_PARAM_AD_NETWORK, platform);
            params.put(EventParams.EVENT_PARAM_AD_MEDIATION, mediation != null ? mediation : "");
            params.put(EventParams.EVENT_PARAM_AD_RESPONSE_LATENCY, deltaTime);
            params.put("session_show_ad_count", sessionCount);
            params.put("session_show_delta_time", sessionDeltaTime);
            logEvent(EventIDs.AD_LOAD_SUCCESS, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params, false);
        }
    }

    public void onAdLoadFailure(String adUnit, String platform, int code, String reason, String segments, int deltaTime) {
//        adListener.onAdLoadFailure(adType, reason);
        if (sampledUser) {
            Map<String, Object> params = new HashMap<>();
            params.put(EventParams.EVENT_PARAM_FLOW_SEQ, "05");
            params.put(EventParams.EVENT_PARAM_AD_UNIT, adUnit);
            params.putAll(eventParams);
            params.put("segment", segments != null ? segments : "null");
            if (reason != null) {
                params.put(EventParams.EVENT_PARAM_FAIL_REASON, reason);
            }

            params.put(EventParams.EVENT_PARAM_AD_NETWORK, platform);
            params.put(EventParams.EVENT_PARAM_AD_RESPONSE_LATENCY, deltaTime);
            params.put("session_show_ad_count", sessionCount);
            params.put("session_show_delta_time", sessionDeltaTime);
            logEvent(EventIDs.AD_LOAD_FAILED, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params, false);
        }
    }

    public void onAdShowSuccess(String adUnit, String platform, String mediation, String segments) {
        adListener.onAdShowSuccess(adType, tag, placementId);
        if (sampledUser && adType != AdType.BANNER) {
            timeStartShow = SystemClock.elapsedRealtime();
            Map<String, Object> params = new HashMap<>();
            params.put(EventParams.EVENT_PARAM_FLOW_SEQ, "09");
            params.put(EventParams.EVENT_PARAM_AD_UNIT, adUnit);
            params.putAll(eventParams);
            params.putAll(clientInfos);
            params.put("segment", segments != null ? segments : "null");
            params.put(EventParams.EVENT_PARAM_AD_NETWORK, platform);
            params.put(EventParams.EVENT_PARAM_AD_MEDIATION, mediation != null ? mediation : "");
            params.put(EventParams.EVENT_PARAM_AD_PLACEMENT, placementId);
            params.put("session_show_ad_count", sessionCount);
            params.put("session_show_delta_time", sessionDeltaTime);
            params.put(EventParams.EVENT_PARAM_AD_SHOW_DURATION, timeStartShow - lastSessionShowTime);
            logEvent(EventIDs.AD_SHOW_SUCCESS, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params, false);
        }
    }

    public void onAdShowFailed(String adUnit, String platform, String reason, String mediation, String segments, boolean fallback) {
        adListener.onAdShowFailed(adType, reason, tag, placementId);
        if (sampledUser) {
            Map<String, Object> params = new HashMap<>();
            params.put(EventParams.EVENT_PARAM_FLOW_SEQ, "10");
            params.put(EventParams.EVENT_PARAM_AD_UNIT, adUnit);
            params.putAll(eventParams);
            params.putAll(clientInfos);
            params.put("segment", segments != null ? segments : "null");
            params.put(EventParams.EVENT_PARAM_AD_NETWORK, platform);
            params.put(EventParams.EVENT_PARAM_AD_PLACEMENT, placementId);
            params.put("session_show_ad_count", sessionCount);
            params.put("session_show_delta_time", sessionDeltaTime);
            params.put(EventParams.EVENT_PARAM_AD_SHOW_DURATION, SystemClock.elapsedRealtime() - lastSessionShowTime);
            params.put("has_fallback_ad", fallback);
            params.put(EventParams.EVENT_PARAM_AD_MEDIATION, mediation != null ? mediation : "");
            if (reason != null) {
                params.put(EventParams.EVENT_PARAM_FAIL_REASON, reason);
            }
            logEvent(EventIDs.AD_SHOW_FAILED, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params, false);
        }
    }

    public void onAdShowFailedNotReady(AdType adType, String placement) {
        adListener.onAdShowFailed(adType, "not ready", tag, placement);
        if (sampledUser) {
            Map<String, Object> params = new HashMap<>();
            params.put(EventParams.EVENT_PARAM_AD_FORMAT, adType.value);
            params.put(EventParams.EVENT_PARAM_AD_PLACEMENT, placement);
            params.put(EventParams.EVENT_PARAM_FAIL_REASON, "not_ready");
            params.put("session_show_ad_count", sessionCount);
            params.put("session_show_delta_time", sessionDeltaTime);
            params.putAll(eventParams);
            logEvent("sdk_ad_show_not_ready", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params, false);
        }
    }

    public void onAdClicked(String adUnit, String platform, String mediation, String segments) {
        adListener.onAdClicked(adType, tag, placementId);
        Map<String, Object> params = new HashMap<>();
        params.put(EventParams.EVENT_PARAM_FLOW_SEQ, "11");
        params.put(EventParams.EVENT_PARAM_AD_UNIT, adUnit);
        params.putAll(eventParams);
        params.putAll(clientInfos);
        params.put(EventParams.EVENT_PARAM_AD_NETWORK, platform);
        params.put("segment", segments != null ? segments : "null");
        params.put(EventParams.EVENT_PARAM_AD_MEDIATION, mediation != null ? mediation : "");
        params.put(EventParams.EVENT_PARAM_AD_PLACEMENT, placementId);
        params.put(EventParams.EVENT_PARAM_AD_RESPONSE_LATENCY, SystemClock.elapsedRealtime() - timeStartShow);
        params.put("session_show_ad_count", sessionCount);
        params.put("session_show_delta_time", sessionDeltaTime);
        logEvent(EventIDs.AD_CLICKED, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params, false);
    }

    // Overloaded method with default parameter
    public void onAdClicked(String adUnit, String platform, String mediation) {
        onAdClicked(adUnit, platform, mediation, null);
    }

    public void onAdClosed(boolean gotReward, String platform, String adUnit, String mediation, String segments) {
        if (gotReward) {
            onUserRewarded(adUnit, platform);
        }
        adListener.onAdClosed(adType, gotReward, tag, placementId);
        Map<String, Object> params = new HashMap<>();
        params.put(EventParams.EVENT_PARAM_FLOW_SEQ, "12");
        params.put(EventParams.EVENT_PARAM_AD_UNIT, adUnit);
        if (adType != AdType.BANNER) {
            long showDuration = SystemClock.elapsedRealtime() - timeStartShow;
            params.put(EventParams.EVENT_PARAM_AD_SHOW_DURATION, showDuration);
        }
        params.putAll(eventParams);
        params.putAll(clientInfos);
        params.put("session_show_ad_count", sessionCount);
        params.put("session_show_delta_time", sessionDeltaTime);
        params.put(EventParams.EVENT_PARAM_AD_NETWORK, platform);
        params.put("segment", segments != null ? segments : "null");
        params.put(EventParams.EVENT_PARAM_AD_MEDIATION, mediation != null ? mediation : "");
        params.put(EventParams.EVENT_PARAM_AD_PLACEMENT, placementId);
        logEvent(EventIDs.AD_CLOSED, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params, false);
        resetPlacementInfo();
    }

    private void resetPlacementInfo() {
        tag = "default";
        placementId = "";
        clientInfos.clear();
    }

    private void onUserRewarded(String adUnit, String platform) {
        Map<String, Object> params = new HashMap<>();
        params.put(EventParams.EVENT_PARAM_FLOW_SEQ, "13");
        params.put(EventParams.EVENT_PARAM_AD_UNIT, adUnit);
        params.putAll(eventParams);
        params.putAll(clientInfos);
        params.put(EventParams.EVENT_PARAM_AD_NETWORK, platform);
        params.put(EventParams.EVENT_PARAM_AD_PLACEMENT, placementId);
        params.put("session_show_ad_count", sessionCount);
        params.put("session_show_delta_time", sessionDeltaTime);
        logEvent(EventIDs.AD_REWARD_USER, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params, false);
    }

    public void onAdRevenuePaid(double revenue, String adUnit, String platform, String currency, String mediation, String segments) {
        if (revenue >= 0.0 && revenue <= 10.0) {
            Map<String, Object> params = new HashMap<>();
            params.put(EventParams.EVENT_PARAM_REVENUE, revenue);
            params.put(EventParams.EVENT_PARAM_CURRENCY, currency);
            params.put(EventParams.EVENT_PARAM_AD_UNIT, adUnit);
            params.putAll(eventParams);
            params.putAll(clientInfos);
            params.put("segment", segments != null ? segments : "null");
            params.put(EventParams.EVENT_PARAM_AD_NETWORK, platform);
            params.put(EventParams.EVENT_PARAM_AD_MEDIATION, mediation != null ? mediation : "");
            params.put(EventParams.EVENT_PARAM_AD_PLACEMENT, placementId);
            params.put("session_show_ad_count", sessionCount);
            params.put("session_show_delta_time", sessionDeltaTime);
            adListener.logEvent(EventIDs.AD_IMPRESSION_REVENUE, EventType.EVENT_TYPE_AD_REVENUE, EventSrc.EVENT_SRC_SDK, params, true);
        }
    }

    private void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, boolean af) {
        adListener.logEvent(eventName, eventType, eventSrc, params, af);
    }

    public void onAdLoadStart(String adUnit, String platform, String segments) {
        if (sampledUser) {
            Map<String, Object> params = new HashMap<>();
            params.put(EventParams.EVENT_PARAM_FLOW_SEQ, "03");
            params.put(EventParams.EVENT_PARAM_AD_UNIT, adUnit);
            params.putAll(eventParams);
            params.put(EventParams.EVENT_PARAM_AD_NETWORK, platform);
            params.put("session_show_ad_count", sessionCount);
            params.put("session_show_delta_time", sessionDeltaTime);
            params.put("segment", segments != null ? segments : "null");
            logEvent(EventIDs.AD_REQUEST, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params, false);
        }
    }

    public MyAdListener resetEventParams(String platform) {
        eventParams.clear();
//        eventParams.put(EventParams.EVENT_PARAM_AD_NETWORK, platform);
        eventParams.put(EventParams.EVENT_PARAM_AD_FORMAT, adType.value);
        eventParams.put(EventParams.EVENT_PARAM_COUNTRY, Locale.getDefault().getCountry());
        return this;
    }
}