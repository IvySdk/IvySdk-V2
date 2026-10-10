package com.ivy.remote;

import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.util.Util;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class RCManager implements IRCManager {

    public static final String KEY_LAST_REFRESH_TIME = "_rc_last_refresh_time_";
    public static final String KEY_FILE_CACHE_REMOTE_CONFIG = "rc.dat";

    private DataManager dataManager = new DataManager();
    private Map<String, Object> defaultData = new HashMap<>();
    private JSONObject remoteConfigData = null;

    public RCManager() {
        loadCacheRemoteData();
    }

    private void loadCacheRemoteData() {
        try {
            String file_path = App.getInstance().getCacheDir().getAbsolutePath() + File.separator + KEY_FILE_CACHE_REMOTE_CONFIG;
            Util.loadCachedFileData(file_path, cachedData -> {
                if (cachedData != null) {
                    try {
                        remoteConfigData = new JSONObject(cachedData);
                        ILog.i(IvyRemoteConfig.TAG, "load local cached remote data success:" + cachedData);
                    } catch (Exception e) {
                        ILog.i(IvyRemoteConfig.TAG, "load local cached remote data failed:" + e.getMessage());
                    }
                }
            });
        } catch (Exception ignored) {
            ILog.i(IvyRemoteConfig.TAG, "load local cached remote data failed");
        }
    }

    @Override
    public void loadLocalConfig(final AppConfigCallback appConfigCallback) {
        dataManager.loadLocalConfig(new AppConfigCallback() {
            @Override
            public void onResult(JSONObject config) {
                if (appConfigCallback != null) {
                    appConfigCallback.onResult(config);
                }
                if (remoteConfigData == null) {
                    remoteConfigData = config.optJSONObject("remoteConfig");
                }
            }
        });
    }

    @Override
    public void loadRemoteData(String appId, boolean debug, JSONObject localConfig, Map<String, Object> headerInfo, final RemoteConfigCallback remoteConfigCallback) {
        dataManager.loadRemoteData(appId, debug, localConfig, headerInfo, new RemoteConfigCallback() {
            @Override
            public void onResult(boolean state, JSONObject json) {
                if (remoteConfigCallback != null) {
                    remoteConfigCallback.onResult(state, json);
                }
                if (state && json != null) {
                    remoteConfigData = json;
                    ILog.i(IvyRemoteConfig.TAG, "loaded remote data:" + remoteConfigData);
                    Util.cacheDataToFile(KEY_FILE_CACHE_REMOTE_CONFIG, json.toString());
                }
            }
        });
    }

    @Override
    public void loadAbData(String appId, boolean debug, Map<String, Object> headerInfo, final AbDataCallback callback) {
        dataManager.loadAbData(appId, debug, headerInfo, new AbDataCallback() {
            @Override
            public void onResult(JSONObject json) {
                callback.onResult(json);
            }
        });
    }

    @Override
    public void loadUserInfo(String appId, final UserInfoCallback callback) {
        dataManager.loadUserTags(appId, new UserInfoCallback() {
            @Override
            public void onResult(String result) {
                if (callback != null) {
                    callback.onResult(result);
                }
            }
        });
    }

    @Override
    public void setDefaultData(Map<String, Object> data) {
        defaultData.putAll(data);
    }

    @Override
    public void clearCachedRemoteConfig() {
        remoteConfigData = null;
        Util.deleteCachedFile(KEY_FILE_CACHE_REMOTE_CONFIG);
    }

    @Override
    public void clearCachedAppConfig() {
        dataManager.clearCachedAppConfig();
    }

    @Override
    public boolean has(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        return (remoteConfigData != null && remoteConfigData.has(key)) || defaultData.containsKey(key);
    }

    @Override
    public String getString(String key) {
        String defaultValue = defaultData.get(key) instanceof String ? (String) defaultData.get(key) : "";
        return remoteConfigData != null ? remoteConfigData.optString(key, defaultValue) : defaultValue;
    }

    @Override
    public boolean getBoolean(String key) {
        boolean defaultValue = defaultData.get(key) instanceof Boolean ? (Boolean) defaultData.get(key) : false;
        return remoteConfigData != null ? remoteConfigData.optBoolean(key, defaultValue) : defaultValue;
    }

    @Override
    public long getLong(String key) {
        long defaultValue = defaultData.get(key) instanceof Long ? (Long) defaultData.get(key) : 0L;
        return remoteConfigData != null ? remoteConfigData.optLong(key, defaultValue) : defaultValue;
    }

    @Override
    public double getDouble(String key) {
        double defaultValue = defaultData.get(key) instanceof Double ? (Double) defaultData.get(key) : 0.0;
        return remoteConfigData != null ? remoteConfigData.optDouble(key, defaultValue) : defaultValue;
    }

    @Override
    public int getInt(String key) {
        ILog.i(IvyRemoteConfig.TAG, "getInt :" + key);
        ILog.i(IvyRemoteConfig.TAG, "defaultData :" + defaultData);
        ILog.i(IvyRemoteConfig.TAG, "remoteConfigData :" + remoteConfigData);
        int defaultValue = defaultData.get(key) instanceof Integer ? (Integer) defaultData.get(key) : 0;
        return remoteConfigData != null ? remoteConfigData.optInt(key, defaultValue) : defaultValue;
    }

}
