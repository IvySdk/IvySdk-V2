package com.ivy.facebook;

import android.content.Context;
import android.os.Bundle;

import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.AppEventsConstants;
import com.facebook.appevents.AppEventsLogger;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.billing.PurchaseState;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;
import com.ivy.base.track.AbsTrack;
import com.ivy.base.track.EventParams;
import com.ivy.base.track.EventType;
import com.ivy.base.track.IConversationCallback;
import com.ivy.base.track.TrackPlatform;
import com.ivy.base.util.Util;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FacebookTrack extends AbsTrack implements IUnProguard {

    public static final String TAG = "Facebook";

    private AppEventsLogger appEventsLogger;
    private final Map<String, Object> userProperties = new HashMap<>();

    @Override
    public void setup(Context context, String appId, String config, boolean debug, IConversationCallback conversationCallback, Runnable readyListener) {
        super.setup(context, appId, config, debug, conversationCallback, readyListener);

        try {
            JSONObject json = new JSONObject(config);
            enableAdPing = json.optBoolean("enableAdPing", true);
            enablePurchasePing = json.optBoolean("enablePurchasePing", true);
        } catch (Exception ignored) {
        }

        if (debug) {
            FacebookSdk.setIsDebugEnabled(true);
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
        } else {
            FacebookSdk.setIsDebugEnabled(false);
        }

        appEventsLogger = AppEventsLogger.newLogger(context);
        enableStatus = true;
    }

    @Override
    public void setUserProperty(String key, Object value) {
        try {
            userProperties.put(key, value);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms) {
        if (!FacebookSdk.isInitialized()) {
            ILog.e(TAG, "log event:" + eventName + "  failed! facebook had not initialized");
            return;
        }
        Map<String, Object> fixedParams = new HashMap<>();
        if (params != null) {
            fixedParams.putAll(params);
        }
        fixedParams.put(EventParams.EVENT_PARAM_EVENT_SRC, eventSrc);
        fixedParams.put(EventParams.EVENT_PARAM_ROLE_ID, Util.roleId());
        try {
            fixedParams.putAll(userProperties);
        } catch (Exception ignored) {
        }

        switch (eventType) {
            case EventType.EVENT_TYPE_PURCHASE:
                logPurchase(fixedParams);
                break;
            case EventType.EVENT_TYPE_AD_REVENUE:
                logAdRevenue(fixedParams);
                break;
            default:
                logEvent(eventName, fixedParams);
                break;
        }
    }

    private void logEvent(String eventName, Map<String, Object> params) {
        if (enableStatus) {
            Bundle bundle = new Bundle();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    bundle.putString(entry.getKey(), (String) value);
                } else if (value instanceof Integer) {
                    bundle.putInt(entry.getKey(), (Integer) value);
                } else if (value instanceof Double) {
                    bundle.putDouble(entry.getKey(), (Double) value);
                } else if (value instanceof Long) {
                    bundle.putLong(entry.getKey(), (Long) value);
                } else if (value instanceof Boolean) {
                    bundle.putBoolean(entry.getKey(), (Boolean) value);
                } else {
                    bundle.putString(entry.getKey(), String.valueOf(value));
                }
            }
            TestManager.getInstance().addEvent(TAG, "事件:" + eventName, EventState.NORMAL, params);
            if (params.containsKey("mock")) return;
            appEventsLogger.logEvent(eventName, bundle);
        } else {
            ILog.w(TAG, "send " + eventName + " failed;\nnot initialized!!!");
        }
    }

    private void logAdRevenue(Map<String, Object> params) {
        if (enableStatus && enableAdPing) {
            double revenue = 0.0d;
            if (params.containsKey(EventParams.EVENT_PARAM_REVENUE)) {
                Object revenueObj = params.get(EventParams.EVENT_PARAM_REVENUE);
                revenue = Double.parseDouble(revenueObj.toString());
                Bundle bundle = new Bundle();
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        bundle.putString(entry.getKey(), (String) value);
                    } else if (value instanceof Double) {
                        bundle.putDouble(entry.getKey(), (Double) value);
                    } else if (value instanceof Integer) {
                        bundle.putInt(entry.getKey(), (Integer) value);
                    } else if (value instanceof Boolean) {
                        bundle.putBoolean(entry.getKey(), (Boolean) value);
                    } else {
                        bundle.putString(entry.getKey(), String.valueOf(value));
                    }
                }
                TestManager.getInstance().addEvent(TAG, "AdImpression", EventState.NORMAL, params);
                if (params.containsKey("mock")) return;
                String currency = params.get(EventParams.EVENT_PARAM_CURRENCY) instanceof String ? (String) params.get(EventParams.EVENT_PARAM_CURRENCY) : "USD";
                bundle.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currency);
                appEventsLogger.logEvent(AppEventsConstants.EVENT_NAME_AD_IMPRESSION, revenue, bundle);
            }
        } else {
            ILog.w(TAG, "send ad revenue event failed;\nenableStatus:" + enableStatus + ";enableAdPing:" + enableAdPing);
        }
    }

    private void logPurchase(Map<String, Object> params) {
        if (enableStatus && enablePurchasePing) {
            Double price = params.get("price_amount") instanceof Double ? (Double) params.get("price_amount") : null;
            if (price == null) return;

            String currency = params.get("currency") instanceof String ? (String) params.get("currency") : "USD";

            Object _state = params.get(EventParams.EVENT_PARAM_PAY_STATE);
            if (_state != null) {
                int state = (int) _state;
                if (PurchaseState.PAY_RESULT == state) {
                    TestManager.getInstance().addEvent(TAG, "fb_mobile_purchase", EventState.NORMAL, params);
                    if (debug) {
                        ILog.w(TAG, "debug mode! stop send purchase event");
                        return;
                    }
                    appEventsLogger.logPurchase(BigDecimal.valueOf(price), Currency.getInstance(currency));
                }
            }
            // PRE_ORDER and VERIFICATION are commented out in original Kotlin code
        } else {
            ILog.w(TAG, "send purchase event failed;\nenableStatus:" + enableStatus + ";enableAdPing:" + enableAdPing);
        }
    }
}
