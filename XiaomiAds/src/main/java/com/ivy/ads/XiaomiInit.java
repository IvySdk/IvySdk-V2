package com.ivy.ads;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import com.ivy.base.IUnProguard;
import com.zeus.gmc.sdk.mobileads.columbus.ad.AdGlobalSdk;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class XiaomiInit implements IPlatformInit, IUnProguard {
    private static final String TAG = "XiaomiInit";
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    @Override
    public void init(Activity activity, JSONObject config, boolean debug, InitCallback listener) {
        initializeColumbus(activity, config, debug, listener);
    }

    private void initializeColumbus(Activity activity, JSONObject config, boolean debug,
                                    InitCallback listener) {
        if (INITIALIZED.get()) {
            notifyInitialized(listener);
            return;
        }

        String appKey = config.optString("appId", "");
        String appSecret = config.optString("appSecret", "");
        String mediationName = config.optString("media", "");
        if (TextUtils.isEmpty(appKey) || TextUtils.isEmpty(appSecret)) {
            Log.e(TAG, "Missing platforms.xiaomi appKey or appSecret");
            notifyInitialized(listener);
            return;
        }

        try {
            XiaomiAds.configure(activity, config, debug);
            AdGlobalSdk.initialize(activity.getApplicationContext(), appKey, appSecret, mediationName);
            INITIALIZED.set(true);
        } catch (Throwable throwable) {
            Log.e(TAG, "Columbus initialization failed", throwable);
        }
        notifyInitialized(listener);
    }

    private void notifyInitialized(InitCallback listener) {
        if (listener != null) {
            listener.onInitialized("", "xiaomi");
        }
    }

    @Override
    public void logEvent(String eventName, Map<String, Object> map) {
    }
}
