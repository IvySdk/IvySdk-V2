package com.ivy.firebase;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.firebase.IFirebase;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class FbRemoteConfig implements IUnProguard {

    private static final String TAG = "FireRemote";

    private FirebaseRemoteConfig remoteConfig;
    private boolean isDefaultDataSetted = false;

    public void setup(boolean debug, final IFirebase.RemoteConfigCallback callback) {
        remoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(debug ? 1 * 60 * 60 : 24 * 60 * 60).build();

        remoteConfig.setConfigSettingsAsync(configSettings);
        remoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (debug) {
                    Map<String, FirebaseRemoteConfigValue> map = remoteConfig.getAll();
                    Set<String> set = map.keySet();
                    Map<String, Object> result = new HashMap<>();
                    for (String key : set) {
                        try {
                            result.put(key, map.get(key).asString());
                        } catch (Exception ignored) {
                            ILog.w("TAG", "转换firebsae remoteconfig 值失败，key=" + key);
                        }
                    }
                    TestManager.getInstance().addEvent("Firebase", "RemoteConfig拉取成功", EventState.NORMAL, result);
                }
                if (callback != null) {
                    callback.onResult(true);
                }
            } else {
                TestManager.getInstance().addEvent("Firebase","RemoteConfig拉取失败", EventState.WARNING, "");
                if (callback != null) {
                    callback.onResult(false);
                }
            }
        });
    }

    public void setupDefaultRemoteData(String defaultData) {
        if (defaultData != null) {
            try {
                if (isDefaultDataSetted) return;
                isDefaultDataSetted = true;
                Map<String, Object> map = new HashMap<>();
                JSONObject json = new JSONObject(defaultData);
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = json.opt(key);
                    map.put(key, value != null ? value : "");
                }
                if (remoteConfig != null) {
                    remoteConfig.setDefaultsAsync(map);
                } else {
                    throw new IllegalAccessException("had not call init firebase remote config before use");
                }
            } catch (Exception e) {
                ILog.e(TAG, "setup default remote config data err:" + e.getMessage());
            }
        }
    }

    public String getRemoteConfigString(String key) {
        if (remoteConfig != null) {
            return remoteConfig.getString(key);
        }
        return FirebaseRemoteConfig.DEFAULT_VALUE_FOR_STRING;
    }

    public double getRemoteConfigDouble(String key) {
        if (remoteConfig != null) {
            return remoteConfig.getDouble(key);
        }
        return FirebaseRemoteConfig.DEFAULT_VALUE_FOR_DOUBLE;
    }

    public boolean getRemoteConfigBoolean(String key) {
        if (remoteConfig != null) {
            return remoteConfig.getBoolean(key);
        }
        return FirebaseRemoteConfig.DEFAULT_VALUE_FOR_BOOLEAN;
    }

    public long getRemoteConfigLong(String key) {
        if (remoteConfig != null) {
            return remoteConfig.getLong(key);
        }
        return FirebaseRemoteConfig.DEFAULT_VALUE_FOR_LONG;
    }

}
