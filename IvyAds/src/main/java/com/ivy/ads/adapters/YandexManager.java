package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;

import com.ivy.base.util.Logger;
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.YandexAds;


public class YandexManager {
    private static final String TAG = "YandexManager";
    private static boolean initialized = false;
    private static final YandexManager INSTANCE = new YandexManager();

    public static synchronized YandexManager getInstance() {
        return INSTANCE;
    }

    private volatile boolean sdkInitialized = false;

    public synchronized void initialize(Context context, InitializationListener initializationListener) {
        if (!initialized) {
            try {
                YandexAds.initialize(context, () -> {
                    sdkInitialized = true;
                    Logger.debug(TAG, "Yandex SDK initialized");
                    if (initializationListener != null) {
                        initializationListener.onInitializationCompleted();
                    }
                });
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
            initialized = true;
        }
    }

    public boolean isSdkInitialized() {
        return sdkInitialized;
    }
}
