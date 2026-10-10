package com.ivy.ads.configuration;

import android.content.Context;

import androidx.annotation.NonNull;

import com.ivy.base.util.Logger;
import com.ivy.base.grid.GridManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationParser {
    private static final String TAG = ConfigurationParser.class.getCanonicalName();
    private final Context mApplicationContext;

    private static Map<Class, BaseConfig> configCache = new HashMap<>();
    private String adConfig;

    public ConfigurationParser(Context applicationContext, String adConfig) {
        this.mApplicationContext = applicationContext;
        this.adConfig = adConfig;
    }

    public static void clearCache() {
        configCache.clear();
    }

    public static void clearPromoteConfigCache() {
        if (configCache.containsKey(PromoteConfig.class)) {
            configCache.remove(PromoteConfig.class);
        }
    }

    @NonNull
    public <T extends BaseConfig> T getConfig(Class<T> configClass) {
        T config = null;
        try {
            if (configCache.containsKey(configClass)) {
                return (T) (configCache.get(configClass));
            }

            config = configClass.newInstance();
            config.fillFromJson(new JSONObject(adConfig));

            configCache.put(configClass, config);
        } catch (InstantiationException ex) {
            Logger.error(TAG, "Error occurred while trying to get grid JSON ", ex);
        } catch (IllegalAccessException ex2) {
            Logger.error(TAG, "Error occurred while trying to get grid JSON ", ex2);
        } catch (JSONException ex3) {
            Logger.error(TAG, "Error occurred while trying to get grid JSON ", ex3);
        }
        return config;
    }
}
