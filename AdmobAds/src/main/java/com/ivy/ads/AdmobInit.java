package com.ivy.ads;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.adsurge.adn.ads.AdSurgePrivacyConfiguration;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.ivy.ads.cmp.CMPUtil;
import com.ivy.base.IUnProguard;

import org.json.JSONObject;

import java.util.Map;

public class AdmobInit implements IPlatformInit, IUnProguard {

    private static final String TAG = "AdmobInit";
    private boolean inited = false;

    @Override
    public void init(Activity activity, JSONObject config, boolean debug, InitCallback listener) {
        // 配置请求参数
        RequestConfiguration requestConfiguration = new RequestConfiguration.Builder()
                // 可以设置测试设备ID、标签年龄等
                .build();

        MobileAds.setRequestConfiguration(requestConfiguration);

        if (inited) {
            return;
        }
        inited = true;

        CMPUtil.getInstance().init(activity, debug, ableLoadAds -> {
            AdSurgePrivacyConfiguration.setAgeRestrictedUser(false); // User is not below age restriction
            AdSurgePrivacyConfiguration.setDoNotSell(false); // Allow data to be used for advertising
            AdSurgePrivacyConfiguration.setUserConsent(true); // User has given consent
            configureXiaomiIfPresent(activity, config, debug);

            // 初始化SDK
            new Thread(() -> {
                try {
                    MobileAds.initialize(activity, new com.google.android.gms.ads.initialization.OnInitializationCompleteListener() {
                        @Override
                        public void onInitializationComplete(InitializationStatus initializationStatus) {
                            // AdMob不直接提供国家代码，可以通过其他方式获取或传递空值
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if (listener != null) listener.onInitialized("", "admob");
                                }
                            });
                        }
                    });
                } catch (Throwable ignored) {

                }
            }).start();

        });
    }

    private void configureXiaomiIfPresent(Activity activity, JSONObject config, boolean debug) {
        try {
            Class<?> xiaomiAds = Class.forName("com.ivy.ads.XiaomiAds");
            xiaomiAds.getMethod("configureForMediation", Activity.class, JSONObject.class,
                            boolean.class)
                    .invoke(null, activity, config, debug);
        } catch (ClassNotFoundException ignored) {
            // XiaomiAds mediation support is optional.
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to configure Xiaomi Ads before AdMob initialization", throwable);
        }
    }

    @Override
    public void logEvent(String eventName, Map<String, Object> map) {

    }
}
