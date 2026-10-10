package com.ivy.ads;

import android.app.Activity;


import com.ivy.base.IUnProguard;
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.YandexAds;


import org.json.JSONObject;

import java.util.Map;


public class YandexInit implements IPlatformInit, IUnProguard {

    @Override
    public void init(Activity activity, JSONObject config, boolean debug, InitCallback listener) {
        try {
            YandexAds.initialize(activity, new InitializationListener() {
                @Override
                public void onInitializationCompleted() {
                    listener.onInitialized("", "yandex");
                }
            });
        } catch (Throwable ex) {
            ex.printStackTrace();
            listener.onInitialized("", "yandex");
        }
    }

    @Override
    public void logEvent(String eventName, Map<String, Object> map) {

    }
}