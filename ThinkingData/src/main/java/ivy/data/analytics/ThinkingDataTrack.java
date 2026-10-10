package ivy.data.analytics;

import android.content.Context;

import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.billing.PurchaseState;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;
import com.ivy.base.track.AbsTrack;
import com.ivy.base.track.EventIDs;
import com.ivy.base.track.EventParams;
import com.ivy.base.track.EventType;
import com.ivy.base.track.IConversationCallback;
import com.ivy.base.track.TrackPlatform;
import com.ivy.base.util.Util;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThinkingDataTrack extends AbsTrack implements IUnProguard {

    public static final String TAG = "ThinkingData";

    @Override
    public void setup(Context context, String appId, String config, boolean debug, IConversationCallback conversationCallback, Runnable readyListener) {
        super.setup(context, appId, config, debug, conversationCallback, readyListener);

        try {
            JSONObject json = new JSONObject(config);
            enableAdPing = json.optBoolean("enableAdPing", true);
            enablePurchasePing = json.optBoolean("enablePurchasePing", true);

            String appKey = json.optString("app_key");
            String serverUrl = json.optString("server_url");

            if (appKey == null || appKey.isEmpty() || serverUrl == null || serverUrl.isEmpty()) {
                ILog.e(TAG, "init failed; appKey:" + appKey + "; server_url:" + serverUrl);
                return;
            }

            TDConfig tdConfig = TDConfig.getInstance(context, appKey, serverUrl);

            // NORMAL 模式:数据会存入缓存，并依据一定的缓存策略上报
            // Debug 模式:数据逐条上报，不建议线上使用
            tdConfig.setMode(debug ? TDConfig.TDMode.NORMAL : TDConfig.TDMode.NORMAL);

            if (debug) {
                TDAnalytics.enableLog(true);
            }

            TDAnalytics.init(tdConfig);

            enableStatus = true;

        } catch (Exception e) {
            ILog.e(TAG, "setup failed: " + e.getMessage());
        }
    }

    @Override
    public void setUserProperty(String key, Object value) {
        try {
            if ("distinct_id".equals(key) && value instanceof String) {
                TDAnalytics.setDistinctId((String) value);
            } else if ("login_id".equals(key) && value instanceof String) {
                TDAnalytics.login((String) value);
            } else if ("customer_user_id".equals(key)) {
                TDAnalytics.getSuperProperties().put("cuid", value);
            } else {
                TDAnalytics.getSuperProperties().put(key, value);
            }
        } catch (Exception e) {
            ILog.e(TAG, "set user property err:" + e.getMessage());
        }
    }

    @Override
    public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms) {
        Map<String, Object> fixedParams = new HashMap<>();
        if (params != null) fixedParams.putAll(params);

        fixedParams.put(EventParams.EVENT_PARAM_EVENT_SRC, eventSrc);
        fixedParams.put(EventParams.EVENT_PARAM_ROLE_ID, Util.roleId());

        switch (eventType) {
            case EventType.EVENT_TYPE_PURCHASE:
                logPurchase(fixedParams);
                break;
            case EventType.EVENT_TYPE_AD_REVENUE:
                logAdRevenue(fixedParams);
                break;
            default:
                logEvent(eventName, fixedParams);
        }
    }

    private void logEvent(String eventName, Map<String, Object> params) {
        try {
            if (enableStatus) {
                JSONObject jsonParams = mapToJSONObject(params);
//                JSONObject wrappedParams = new JSONObject();
//                wrappedParams.put("extra", jsonParams);
                TestManager.getInstance().addEvent("IVY", "事件:" + eventName, EventState.NORMAL, params);
                if (params.containsKey("mock")) return;
                TDAnalytics.track(eventName, jsonParams);
            } else {
                ILog.w(TAG, "send event:" + eventName + " failed; not initialized!!!");
            }
        } catch (Exception e) {
            ILog.e(TAG, "send event:" + eventName + " failed;" + e.getMessage());
        }
    }

    private void logAdRevenue(Map<String, Object> params) {
        try {
            if (!(enableStatus && enableAdPing)) {
                ILog.w(TAG, "send ad revenue event failed;\nenableStatus:" + enableStatus + ";enableAdPing:" + enableAdPing);
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
            }
            TestManager.getInstance().addEvent("IVY", "af_ad_revenue", EventState.NORMAL, params);
            if (params.containsKey("mock")) return;
            logEvent(EventIDs.AD_IMPRESSION_REVENUE, params);

        } catch (Exception e) {
            ILog.e(TAG, "send ad revenue event failed;" + e.getMessage());
        }
    }

    private void logPurchase(Map<String, Object> params) {

        if (!(enableStatus && enablePurchasePing)) {
            ILog.w(TAG, "send purchase event failed;\nenableStatus:" + enableStatus + ";enableAdPing:" + enableAdPing);
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
                TestManager.getInstance().addEvent("IVY", "af_purchase", EventState.NORMAL, params);
                if (debug) {
                    ILog.w(TAG, "debug mode! stop send purchase event");
                    return;
                }
                logEvent(EventIDs.AF_IAP_PURCHASED, params);
            }
        }
    }

    private JSONObject mapToJSONObject(Map<String, Object> params) {
        JSONObject json = new JSONObject();
        try {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("extra")) {
                    Object value = entry.getValue();
                    JSONObject _extra = null;
                    try {
                        Map<String, Object> __extra = (Map<String, Object>) value;
                        _extra = mapToJSONObject(__extra);
                    } catch (Exception ignored) {
                        ILog.e(TAG, "format extra params error");
                    }
                    if (_extra != null) {
                        json.put("extra", _extra);
                    }
                } else {
                    Object value = entry.getValue();
                    if (value instanceof Boolean) {
                        json.put(entry.getKey(), ((Boolean) value) ? 1 : 0);
                    } else {
                        json.put(entry.getKey(), value);
                    }
                }
            }
        } catch (Exception e) {
            ILog.e(TAG, "format event params err:" + e.getMessage());
        }
        return json;
    }
}
