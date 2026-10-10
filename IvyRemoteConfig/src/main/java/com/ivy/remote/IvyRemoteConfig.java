package com.ivy.remote;

import org.json.JSONObject;

import java.util.Map;

public class IvyRemoteConfig implements IRCManager {

    public static final String  TAG = "IvyRemoteConfig";

    private RCManager manager = new RCManager();

    // 单例实现
    private static volatile IvyRemoteConfig instance;

    private IvyRemoteConfig() {
        // 私有构造
    }

    public static IvyRemoteConfig getInstance() {
        if (instance == null) {
            synchronized (IvyRemoteConfig.class) {
                if (instance == null) {
                    instance = new IvyRemoteConfig();
                }
            }
        }
        return instance;
    }

    @Override
    public void loadLocalConfig(AppConfigCallback appConfigCallback) {
        manager.loadLocalConfig(appConfigCallback);
    }

    @Override
    public void loadRemoteData(String appId, boolean debug, JSONObject localConfig, Map<String, Object> headerInfo, RemoteConfigCallback remoteConfigCallback) {
        manager.loadRemoteData(appId, debug, localConfig, headerInfo, remoteConfigCallback);
    }

    @Override
    public void loadAbData(String appId, boolean debug, Map<String, Object> headerInfo, AbDataCallback callback) {
        manager.loadAbData(appId, debug, headerInfo, callback);
    }

    @Override
    public void loadUserInfo(String appId, UserInfoCallback callback) {
        manager.loadUserInfo(appId, callback);
    }

    @Override
    public void setDefaultData(Map<String, Object> data) {
        manager.setDefaultData(data);
    }

    @Override
    public void clearCachedRemoteConfig() {
        manager.clearCachedRemoteConfig();
    }

    @Override
    public void clearCachedAppConfig() {
        manager.clearCachedAppConfig();
    }

    @Override
    public boolean has(String key) {
        return manager.has(key);
    }

    @Override
    public String getString(String key) {
        return manager.getString(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return manager.getBoolean(key);
    }

    @Override
    public long getLong(String key) {
        return manager.getLong(key);
    }

    @Override
    public double getDouble(String key) {
        return manager.getDouble(key);
    }

    @Override
    public int getInt(String key) {
        return manager.getInt(key);
    }
}