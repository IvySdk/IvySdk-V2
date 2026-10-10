package com.ivy.firebase;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ivy.base.App;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PendingEvent {
    public final String eventId;
    public final String eventType;
    public final String eventSrc;
    public final Map<String, Object> params;

    PendingEvent(String eventId, String eventType, String eventSrc, Map<String, Object> params) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventSrc = eventSrc;
        this.params = params;
    }
}


public class FirebaseTrack extends AbsTrack implements IUnProguard {

    public static final String TAG = "Firebase";

    private FirebaseAnalytics firebaseAnalytics = null;

    private List<PendingEvent> pendingEvents = new ArrayList<>();


    @Override
    public void setup(Context context, String appId, String config, boolean debug, IConversationCallback conversationCallback, Runnable readyListener) {
        super.setup(context, appId, config, debug, conversationCallback, readyListener);
        try {
            JSONObject obj = new JSONObject(config);
            enableAdPing = obj.optBoolean("enableAdPing", true);
            enablePurchasePing = obj.optBoolean("enablePurchasePing", true);
        } catch (Exception ignored) {
        }
        enableStatus = true;
    }

    private FirebaseAnalytics getFirebaseAnalytics() {
        if (FirebaseInit.isReady()) {
            if (firebaseAnalytics == null) {
                firebaseAnalytics = FirebaseAnalytics.getInstance(App.getInstance());
            }
            return firebaseAnalytics;
        }
        return null;
    }

    @Override
    public void setUserProperty(String key, Object value) {
        try {
            if ("customer_user_id".equals(key)) {
                if (value instanceof String) {
                    FirebaseAnalytics _firebaseAnalytics = getFirebaseAnalytics();
                    if (_firebaseAnalytics != null) {
                        _firebaseAnalytics.setUserProperty("cuid", (String) value);
                    }
                }
                return;
            }
            if ("firebase_userId".equals(key)) {
                if (value instanceof String) {
                    FirebaseAnalytics _firebaseAnalytics = getFirebaseAnalytics();
                    if (_firebaseAnalytics != null) {
                        _firebaseAnalytics.setUserId((String) value);
                    }
                }
                return;
            }
            // firebaseAnalytics.setUserProperty(key, String.valueOf(value));
        } catch (Exception e) {
            ILog.e(TAG, "set user property failed:" + key + ";" + value + "; reason:" + e.getMessage());
        }
    }


    private synchronized void addPendingEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params) {
        if (pendingEvents.size() > 100) return;
        Map<String, Object> copiedParams = params == null ? null : new HashMap<>(params);
        PendingEvent event = new PendingEvent(eventName, eventType, eventSrc, copiedParams);
        pendingEvents.add(event);
    }

    private synchronized List<PendingEvent> takePendingEvents() {
        if (pendingEvents.isEmpty()) {
            return Collections.emptyList();
        }
        List<PendingEvent> list = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return list;
    }

    private void flushPendingEvents() {
        if (pendingEvents.isEmpty()) return;
        List<PendingEvent> list = takePendingEvents();
        for (int i = 0; i < list.size(); i++) {
            try {
                PendingEvent event = list.get(i);
                logEvent(event.eventId, event.eventType, event.eventSrc, event.params, null);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms) {
        if (!FirebaseInit.isReady()) {
            addPendingEvent(eventName, eventType, eventSrc, params);
            return;
        }
        Map<String, Object> fixedParams = new HashMap<>();
        if (params != null) {
            fixedParams.putAll(params);
        }
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
                logEventInternal(eventName, fixedParams);
                break;
        }
        flushPendingEvents();
    }

    private void logEventInternal(String eventName, Map<String, Object> params) {
        if (enableStatus) {
            Bundle bundle = new Bundle();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                Object v = entry.getValue();
                if (v instanceof String) {
                    bundle.putString(entry.getKey(), (String) v);
                } else if (v instanceof Integer) {
                    bundle.putInt(entry.getKey(), (Integer) v);
                } else if (v instanceof Long) {
                    bundle.putLong(entry.getKey(), (Long) v);
                } else if (v instanceof Double) {
                    bundle.putDouble(entry.getKey(), (Double) v);
                } else if (v instanceof Float) {
                    bundle.putFloat(entry.getKey(), (Float) v);
                }
            }
            FirebaseAnalytics _firebaseAnalytics = getFirebaseAnalytics();
            if (_firebaseAnalytics != null) {
                ILog.i(TAG, eventName + " sent");
                TestManager.getInstance().addEvent("Firebase", "事件:" + eventName, EventState.NORMAL, bundle);
                if (params.containsKey("mock")) return;
                _firebaseAnalytics.logEvent(eventName, bundle);
            }
        } else {
            ILog.w(TAG, "send event:" + eventName + " failed; not initialized!!!");
        }
    }

    private void logAdRevenue(Map<String, Object> params) {
        if (enableStatus && enableAdPing) {
            Bundle bundle = new Bundle();

            if (params.containsKey(EventParams.EVENT_PARAM_REVENUE)) {
                Object it = params.get(EventParams.EVENT_PARAM_REVENUE);
                params.put(FirebaseAnalytics.Param.VALUE, it);
                double _revenue = Double.parseDouble(it.toString());
                bundle.putDouble(FirebaseAnalytics.Param.VALUE, _revenue);
                //  params.remove(EventParams.EVENT_PARAM_REVENUE);
            }

            logEventInternal(EventIDs.AD_IMPRESSION_REVENUE_FIREBASE, params);

            if (params.get(EventParams.EVENT_PARAM_AD_NETWORK) instanceof String) {
                bundle.putString(FirebaseAnalytics.Param.AD_PLATFORM, (String) params.get(EventParams.EVENT_PARAM_AD_NETWORK));
            }
            if (params.get(EventParams.EVENT_PARAM_AD_MEDIATION) instanceof String) {
                bundle.putString(FirebaseAnalytics.Param.AD_SOURCE, (String) params.get(EventParams.EVENT_PARAM_AD_MEDIATION));
            }
            if (params.get(EventParams.EVENT_PARAM_AD_FORMAT) instanceof String) {
                bundle.putString(FirebaseAnalytics.Param.AD_FORMAT, (String) params.get(EventParams.EVENT_PARAM_AD_FORMAT));
            }
            if (params.get(EventParams.EVENT_PARAM_AD_UNIT) instanceof String) {
                bundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, (String) params.get(EventParams.EVENT_PARAM_AD_UNIT));
            }
            if (params.get(EventParams.EVENT_PARAM_CURRENCY) instanceof String) {
                bundle.putString(FirebaseAnalytics.Param.CURRENCY, (String) params.get(EventParams.EVENT_PARAM_CURRENCY));
            }
            FirebaseAnalytics _firebaseAnalytics = getFirebaseAnalytics();
            if (_firebaseAnalytics != null) {
                TestManager.getInstance().addEvent("Firebase", "ad_impression", EventState.NORMAL, bundle);
                if (params.containsKey("mock")) return;
                _firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, bundle);
            }
        } else {
            ILog.w(TAG, "send ad revenue event failed;\nenableStatus:" + enableStatus + ";enableAdPing:" + enableAdPing);
        }
    }

    private void logPurchase(Map<String, Object> params) {
        if (enableStatus && enablePurchasePing) {
            if (params.containsKey("conf_price")) {
                Object obj_price = params.get("conf_price");
                if (obj_price == null) return;
                double price_value = Double.parseDouble(obj_price.toString());
                if (price_value > 150) return;
                params.put(FirebaseAnalytics.Param.VALUE, price_value);
                params.remove("conf_price");
                params.remove("currency");
                params.put(FirebaseAnalytics.Param.CURRENCY, "USD");

                params.remove("signature");
                params.remove("original_json");
                params.remove("purchase_token");
                params.remove("is_acknowledged");
                params.remove("is_auto_renewing");
                params.remove("purchase_state");

                Object _state = params.get(EventParams.EVENT_PARAM_PAY_STATE);
                if (_state != null) {
                    int state = (int) _state;
                    if (PurchaseState.PRE_ORDER == state) {
                        logEventInternal(EventIDs.IAP_PRE_ORDER, params);
                    } else if (PurchaseState.VERIFICATION == state) {
                        logEventInternal(EventIDs.IAP_VERIFICATION, params);
                    } else if (PurchaseState.PAY_RESULT == state) {
                        TestManager.getInstance().addEvent("Firebase", "sdk_iap_purchased", EventState.NORMAL, params);
                        if (debug) {
                            ILog.w(TAG, "debug mode! stop send purchase event");
                            return;
                        }
                        logEventInternal("iap_user_payment", params);
                        logEventInternal(EventIDs.IAP_PURCHASED, params);
                    }
                }
            }
        } else {
            ILog.w(TAG, "send purchase event failed;\nenableStatus:" + enableStatus + ";enableAdPing:" + enableAdPing);
        }
    }
}
