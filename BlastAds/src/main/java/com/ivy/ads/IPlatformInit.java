package com.ivy.ads;

import android.app.Activity;

import org.json.JSONObject;

import java.util.Map;

/**
 * 平台初始化接口
 * 所有广告平台的初始化类都应该实现这个接口
 */
public interface IPlatformInit {
    /**
     * 初始化平台 SDK
     *
     * @param activity Activity 上下文
     */
    void init(Activity activity, JSONObject config, boolean debug, InitCallback listener);


    void logEvent(String eventName, Map<String, Object> map);

}
