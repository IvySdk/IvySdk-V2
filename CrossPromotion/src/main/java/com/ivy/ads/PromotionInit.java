package com.ivy.ads;

import android.app.Activity;
import android.util.Log;

import com.ivy.base.IUnProguard;
import com.ivy.crosspromotion.CrossPromotion;
import com.ivy.crosspromotion.CrossPromotionConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class PromotionInit implements IPlatformInit, IUnProguard {
    private static final String TAG = "CrossPromotion";

    @Override
    public void init(
            Activity activity,
            JSONObject config,
            boolean debug,
        InitCallback listener
    ) {
        try {
            JSONObject platformConfig = config == null ? new JSONObject() : config;
            JSONObject bundledConfig = loadBundledConfig(activity);
            String configUrl = platformConfig.optString("configUrl", "").trim();
            if (configUrl.isEmpty() && bundledConfig != null) {
                configUrl = bundledConfig.optString("configUrl", "").trim();
            }
            CrossPromotionConfig parsed = CrossPromotionConfig.fromJson(bundledConfig);
            Log.d(TAG, "PromotionInit: configUrl=" + configUrl + ", parsed ads count=" + parsed.getAds().size());
            CrossPromotion.getInstance().initialize(
                    activity,
                    parsed,
                    configUrl,
                    PromotionEventRouter.INSTANCE);
        } catch (Exception error) {
            Log.e(TAG, "Failed to initialize promotion config", error);
        }

        listener.onInitialized("", "promotion");
    }

    @Override
    public void logEvent(String eventName, Map<String, Object> map) {
        // Attribution is reported directly through AppsFlyer CrossPromotionHelper.
    }

    private JSONObject loadBundledConfig(Activity activity) {
        if (activity == null) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                activity.getAssets().open("crosspromotion.json"),
                StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            return new JSONObject(json.toString());
        } catch (Exception ignored) {
            return null;
        }
    }
}
