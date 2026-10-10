//package com.ivy.core.ab;
//
//import android.content.Context;
//import org.json.JSONObject;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Consumer;
//
//public interface IAB {
//
//    void setUserInfo(String key, Object value);
//
//    void setHeaderInfo(String key, Object value);
//
//    String getSSID();
//
//    void init(Context context, String appId, String channel, String roleId, boolean debug);
//
//    void loadAbTestConfigs(Callback consumer);
//
//    String getAllAbConfigs();
//
//    String getAbConfigString(String key, String defaultValue);
//
//    int getAbConfigInt(String key, int defaultValue);
//
//    long getAbConfigLong(String key, long defaultValue);
//
//    double getAbConfigDouble(String key, double defaultValue);
//
//    boolean getAbConfigBoolean(String key, boolean defaultValue);
//
//    float getAbConfigFloat(String key, float defaultValue);
//
//    void logEvent(String event, String eventType, String eventSrc, Map<String, Object> params);
//
//    String getAbSdkVersion();
//
//    void setHeaderInfo(HashMap<String, Object> params);
//
//    void setUserInfo(JSONObject ps);
//
//    // 回调接口，用于 loadAbTestConfigs
//    interface Callback {
//        void onResult(boolean success);
//    }
//}
