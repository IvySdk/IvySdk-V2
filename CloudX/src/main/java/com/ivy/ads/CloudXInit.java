package com.ivy.ads;

import android.app.Activity;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.ivy.ads.cmp.CMPUtil;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;

import org.json.JSONObject;

import java.util.Map;

import io.cloudx.sdk.CloudX;
import io.cloudx.sdk.CloudXError;
import io.cloudx.sdk.CloudXInitializationConfiguration;
import io.cloudx.sdk.CloudXInitializationListener;
import io.cloudx.sdk.CloudXLogLevel;
import io.cloudx.sdk.CloudXSdkConfiguration;

public class CloudXInit implements IPlatformInit, IUnProguard {

    public static final String TAG = "CloudX";

    @Override
    public void init(Activity activity, JSONObject config, boolean debug, InitCallback listener) {
        try {
            String appKey = config.getString("sdkKey");
            if (TextUtils.isEmpty(appKey)) {
                ILog.e(TAG, "invalid sdk key");
                return;
            }
            CMPUtil.getInstance().init(activity, debug, ableLoadAds -> {

//                // 设置哈希用户 ID 用于定向
//                CloudX.setHashedUserId("hashed-user-id");
//
//                // 设置自定义用户键值对
//                CloudX.setUserKeyValue("age", "25");
//                CloudX.setUserKeyValue("gender", "male");
//                CloudX.setUserKeyValue("location", "US");
//
//                // 设置自定义应用键值对
//                CloudX.setAppKeyValue("app_version", "1.0.0");
//                CloudX.setAppKeyValue("user_level", "premium");
//
//                // 清除所有自定义键值对
//                CloudX.clearAllKeyValues();

                CloudX.setMinLogLevel(debug ? CloudXLogLevel.VERBOSE : CloudXLogLevel.NONE);

                // 使用应用密钥初始化
                CloudXInitializationConfiguration configuration = CloudXInitializationConfiguration.builder(appKey).build();
                CloudX.initialize(configuration, new CloudXInitializationListener() {
                    @Override
                    public void onInitialized(@NonNull CloudXSdkConfiguration configuration) {
                        ILog.e(TAG, "CloudX SDK init success");
                        if (listener != null) listener.onInitialized("", "cloudx");
                    }

                    @Override
                    public void onInitializationFailed(@NonNull CloudXError cloudXError) {
                        ILog.e(TAG, "CloudX SDK init failed:" + cloudXError.getFormattedMessage());
                    }
                });
            });
        } catch (Exception e) {

        }

    }

    @Override
    public void logEvent(String eventName, Map<String, Object> map) {

    }

}
