package com.ivy.remote;

import org.json.JSONObject;

import java.util.Map;

public interface IRCManager {

    void loadLocalConfig(AppConfigCallback appConfigCallback);

    void loadRemoteData(String appId, boolean debug, JSONObject localConfig, Map<String, Object> headerInfo, RemoteConfigCallback remoteConfigCallback);

    void loadAbData(String appId, boolean debug, Map<String, Object> headerInfo, AbDataCallback callback);

    void loadUserInfo(String appId, UserInfoCallback callback);

    void setDefaultData(Map<String, Object> data);

    void clearCachedRemoteConfig();

    void clearCachedAppConfig();

    boolean has(String key);

    String getString(String key);

    boolean getBoolean(String key);

    long getLong(String key);

    double getDouble(String key);

    int getInt(String key);

    /**
     * Callback interfaces for compatibility with Java
     */
    interface AppConfigCallback {
        void onResult(JSONObject config);
    }

    interface RemoteConfigCallback {
        void onResult(boolean success, JSONObject result);
    }

    interface AbDataCallback {
        void onResult(JSONObject data);
    }

    interface UserInfoCallback {
        void onResult(String userInfo);
    }
}
