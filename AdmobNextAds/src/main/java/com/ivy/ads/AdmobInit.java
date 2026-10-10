package com.ivy.ads;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.ivy.ads.cmp.CMPUtil;
import com.ivy.base.IUnProguard;

import org.json.JSONObject;

import java.util.Map;

public class AdmobInit implements IPlatformInit, IUnProguard {

    private boolean inited = false;

    @Override
    public void init(Activity activity, JSONObject config, boolean debug, InitCallback listener) {
        if (inited) {
            return;
        }
        inited = true;

        CMPUtil.getInstance().init(activity, debug, ableLoadAds -> {
            // 初始化SDK
            new Thread(
                () -> {
                    // Initialize GMA Next-Gen SDK on a background thread.
                    MobileAds.initialize(
                            activity,
                            // Sample AdMob app ID: ca-app-pub-3940256099942544~3347511713
                            new InitializationConfig.Builder(config.optString("appid")).build(),
                            initializationStatus -> {
                                // 配置请求参数
//                                RequestConfiguration requestConfiguration = new RequestConfiguration.Builder()
//                                        // 可以设置测试设备ID、标签年龄等
//                                        .build();
//
//                                MobileAds.setRequestConfiguration(requestConfiguration);
                                // Adapter initialization is complete.
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (listener != null) listener.onInitialized("", "admob");
                                    }
                                });
                            });
                }).start();
        });
    }

    @Override
    public void logEvent(String eventName, Map<String, Object> map) {

    }
}
