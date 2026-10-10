//package com.ivy.core.ab;
//
//import android.content.Context;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//
//import com.bytedance.applog.AppLog;
//import com.bytedance.applog.IDataObserver;
//import com.bytedance.applog.InitConfig;
//import com.bytedance.applog.UriConfig;
//import com.ivy.base.billing.PurchaseState;
//import com.ivy.base.track.EventIDs;
//import com.ivy.base.track.EventParams;
//import com.ivy.base.track.EventType;
//import com.ivy.base.util.Util;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicInteger;
//
//public class Hsab implements IAB {
//
//    private final Map<Integer, Callback> callbacks = new HashMap<>();
//    private final AtomicInteger callbackIdx = new AtomicInteger(0);
//    private boolean debug = false;
//
//    private int setCallback(Callback cb) {
//        int idx = callbackIdx.incrementAndGet();
//        callbacks.put(idx, cb);
//        return idx;
//    }
//
//    @Override
//    public void setHeaderInfo(String key, Object value) {
//        AppLog.setHeaderInfo("hs_" + key, value);
//    }
//
//    @Override
//    public void setUserInfo(String key, Object value) {
//        JSONObject obj = new JSONObject();
//        try {
//            obj.put("hs_" + key, value);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        AppLog.profileSet(obj);
//    }
//
//    @Override
//    public void init(Context context, String appId, String channel, String roleId, boolean debug) {
//        this.debug = debug;
//        AppLog.setUserUniqueID(roleId);
//        InitConfig config = new InitConfig(appId, channel);
//
//        UriConfig uriConfig = UriConfig.createByDomain("https://gator.uba.ap-southeast-1.volces.com", null);
//        uriConfig.setAbUri("https://tab.ab.ap-southeast-1.volces.com" + UriConfig.PATH_AB);
//
//        config.setUriConfig(uriConfig);
//        config.setAutoStart(false);
//        config.setAbEnable(true);
//        config.setLogEnable(debug);
//        config.setLogger((s, throwable) -> {
//            Log.d("hsab", s);
//            if (throwable != null) throwable.printStackTrace();
//        });
//
//        AppLog.setEncryptAndCompress(!debug);
//        AppLog.init(context, config);
//        AppLog.getInstance().start();
//    }
//
//
//    @Override
//    public String getSSID() {
//        String ssid = AppLog.getInstance().getSsid();
//        Log.e("hsab", "ssid:" + ssid);
//        return ssid;
//    }
//
//    @Override
//    public void loadAbTestConfigs(Callback consumer) {
//        final int idx = setCallback(consumer);
//
//        AppLog.addDataObserver(new IDataObserver() {
//            @Override
//            public void onIdLoaded(String p0, String p1, String p2) {
//                if (debug) Log.e("hsab", "id loaded: " + p0 + ", " + p1 + ", " + p2);
//            }
//
//            @Override
//            public void onRemoteIdGet(boolean p0, String p1, String p2, String p3, String p4, String p5, String p6) {
//                if (debug)
//                    Log.e("hsab", "remote id get: " + p0 + ", " + p1 + ", " + p2 + ", " + p3 + ", " + p4 + ", " + p5 + ", " + p6);
//            }
//
//            @Override
//            public void onRemoteConfigGet(boolean p0, JSONObject p1) {
//                if (debug && p1 != null) {
//                    Log.e("hsab", "remote config: " + p1.toString());
//                }
//            }
//
//            @Override
//            public void onRemoteAbConfigGet(boolean p0, JSONObject p1) {
//                if (debug)
//                    Log.e("hsab", "remote ab config: " + p1.toString() + ", ab version " + AppLog.getAbSdkVersion());
//                new Handler(Looper.getMainLooper()).post(() -> {
//                    if (callbacks.containsKey(idx)) {
//                        callbacks.remove(idx).onResult(true);
//                    }
//                });
//                AppLog.removeDataObserver(this);
//            }
//
//            @Override
//            public void onAbVidsChange(String p0, String p1) {
//                if (debug) Log.e("hsab", "ab vids change: " + p0 + ", " + p1);
//            }
//        });
//
//        AppLog.pullAbTestConfigs();
//
//        // 超时回调 60s
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            if (callbacks.containsKey(idx)) {
//                callbacks.remove(idx).onResult(false);
//            }
//        }, 60000);
//    }
//
//    @Override
//    public String getAllAbConfigs() {
//        return AppLog.getInstance().getAllAbTestConfigs().toString();
//    }
//
//    @Override
//    public String getAbConfigString(String key, String defaultValue) {
//        Object value = AppLog.getInstance().getAbConfig(key, defaultValue);
//        return value != null ? value.toString() : defaultValue;
//    }
//
//    @Override
//    public int getAbConfigInt(String key, int defaultValue) {
//        Integer value = AppLog.getInstance().getAbConfig(key, defaultValue);
//        return value != null ? value : defaultValue;
//    }
//
//    @Override
//    public long getAbConfigLong(String key, long defaultValue) {
//        Long value = AppLog.getInstance().getAbConfig(key, defaultValue);
//        return value != null ? value : defaultValue;
//    }
//
//    @Override
//    public double getAbConfigDouble(String key, double defaultValue) {
//        Double value = AppLog.getInstance().getAbConfig(key, defaultValue);
//        return value != null ? value : defaultValue;
//    }
//
//    @Override
//    public boolean getAbConfigBoolean(String key, boolean defaultValue) {
//        Boolean value = AppLog.getInstance().getAbConfig(key, defaultValue);
//        return value != null ? value : defaultValue;
//    }
//
//    @Override
//    public float getAbConfigFloat(String key, float defaultValue) {
//        Float value = AppLog.getInstance().getAbConfig(key, defaultValue);
//        return value != null ? value : defaultValue;
//    }
//
//    @Override
//    public void logEvent(String event, String eventType, String eventSrc, Map<String, Object> params) {
//        Bundle b = new Bundle();
//        b.putString(EventParams.EVENT_PARAM_EVENT_SRC, eventSrc);
//        b.putString(EventParams.EVENT_PARAM_ROLE_ID, Util.roleId());
//
//        if (EventType.EVENT_TYPE_PURCHASE.equals(eventType)) {
//            logPurchase(b, params);
//        } else if (EventType.EVENT_TYPE_AD_REVENUE.equals(eventType)) {
//            logAdRevenue(b, params);
//        } else {
//            logEventInternal(event, b, params);
//        }
//    }
//
//    private void logPurchase(Bundle b, Map<String, Object> params) {
//        if (params == null) return;
//        Object state = params.get(EventParams.EVENT_PARAM_PAY_STATE);
//        if (state != null) {
//            int _state = (int) state;
//            if (PurchaseState.PRE_ORDER == _state) {
//                logEventInternal(EventIDs.IAP_PRE_ORDER, b, params);
//            } else if (PurchaseState.VERIFICATION == _state) {
//                logEventInternal(EventIDs.IAP_VERIFICATION, b, params);
//            } else if (PurchaseState.PAY_RESULT == _state) {
//                if (params.get("sdk_state") != null && params.get("sdk_state").equals(1)) {
//                    if (params.containsKey("price_amount"))
//                        params.put("af_revenue", params.get("price_amount"));
//                    if (params.containsKey("sku")) params.put("af_content_id", params.get("sku"));
//                    if (params.containsKey("type"))
//                        params.put("af_content_type", params.get("type"));
//                    if (!params.containsKey("currency")) params.put("currency", "USD");
//                    logEventInternal("af_purchase", b, params);
//                }
//            }
//        }
//    }
//
//    private void logAdRevenue(Bundle b, Map<String, Object> params) {
//        if (params == null) return;
//        if (params.containsKey(EventParams.EVENT_PARAM_CURRENCY))
//            params.put("af_currency", params.get(EventParams.EVENT_PARAM_CURRENCY));
//        if (params.containsKey(EventParams.EVENT_PARAM_REVENUE))
//            params.put("af_revenue", params.get(EventParams.EVENT_PARAM_REVENUE));
//        if (params.containsKey(EventParams.EVENT_PARAM_AD_MEDIATION))
//            params.put("mediationNetwork", params.get(EventParams.EVENT_PARAM_AD_MEDIATION));
//        if (params.containsKey(EventParams.EVENT_PARAM_AD_NETWORK))
//            params.put("monetizationNetwork", params.get(EventParams.EVENT_PARAM_AD_NETWORK));
//        logEventInternal("af_ad_revenue", b, params);
//    }
//
//    private void logEventInternal(String event, Bundle b, Map<String, Object> params) {
//        if (params != null) {
//            for (Map.Entry<String, Object> entry : params.entrySet()) {
//                Object v = entry.getValue();
//                String k = entry.getKey();
//                if (v instanceof Integer) b.putInt(k, (Integer) v);
//                else if (v instanceof String) b.putString(k, (String) v);
//                else if (v instanceof Boolean) b.putBoolean(k, (Boolean) v);
//                else if (v instanceof Double) b.putDouble(k, (Double) v);
//            }
//        }
//        AppLog.onEventV3(event, b);
//    }
//
//    @Override
//    public String getAbSdkVersion() {
//        return AppLog.getAbSdkVersion();
//    }
//
//    @Override
//    public void setHeaderInfo(HashMap<String, Object> params) {
//        HashMap<String, Object> map = new HashMap<>();
//        for (Map.Entry<String, Object> entry : params.entrySet()) {
//            map.put("hs_" + entry.getKey(), entry.getValue());
//        }
//        AppLog.setHeaderInfo(map);
//    }
//
//    @Override
//    public void setUserInfo(JSONObject ps) {
//        JSONObject json = new JSONObject();
//        try {
//            Iterator<String> iterator = ps.keys();
//            while (iterator.hasNext()) {
//                String key = iterator.next();
//                json.put("hs_" + key, ps.get(key));
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        AppLog.profileSet(json);
//    }
//
//}
