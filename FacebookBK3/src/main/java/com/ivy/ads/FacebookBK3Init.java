package com.ivy.ads;

import android.app.Activity;

import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.biddingkit.bridge.BiddingKit;
import com.ivy.base.IUnProguard;

import org.json.JSONObject;

import java.util.Map;

public class FacebookBK3Init implements IPlatformInit, IUnProguard {
    private boolean inited = false;
    private static String appId = "";
    private static String platformId = "";
    private static boolean debug = false;

    public static String getAppId() {
        return appId;
    }

    public static String getPlatformId() {
        return platformId;
    }

    public static boolean isDebug(){
        return debug;
    }

    @Override
    public void init(Activity activity, JSONObject config, boolean debug, InitCallback listener) {
        if (config != null) {
            appId = config.optString("appId", "");
            platformId = config.optString("platformId", appId);
            int globalTimeoutMs = config.optInt("timeoutMs", config.optInt("globalTimeoutMs", 0));
            if (globalTimeoutMs > 0) {
                BiddingKit.setGlobalTimeoutMs(globalTimeoutMs);
            }
        }

//        debug = false;
        FacebookBK3Init.debug = debug;
        BiddingKit.init(activity.getApplicationContext());
        BiddingKit.setDebugBuild(debug);
        AdSettings.setTestMode(debug);

        if (inited) {
            notifyInitialized(activity, listener);
            return;
        }
        inited = true;

        try {
            if (!AudienceNetworkAds.isInitialized(activity)) {
                AudienceNetworkAds.buildInitSettings(activity)
                        .withInitListener(new AudienceNetworkAds.InitListener() {
                            @Override
                            public void onInitialized(AudienceNetworkAds.InitResult result) {
                                notifyInitialized(activity, listener);
                            }
                        })
                        .initialize();
            } else {
                notifyInitialized(activity, listener);
            }
        } catch (Throwable ignored) {
            notifyInitialized(activity, listener);
        }
    }

    private void notifyInitialized(Activity activity, InitCallback listener) {
        if (listener != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onInitialized("", "facebook_bk3");
                }
            });
        }
    }

    @Override
    public void logEvent(String eventName, Map<String, Object> map) {

    }
}
