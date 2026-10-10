package com.ivy.ads;

import android.content.Context;
import android.util.Log;

import com.ivy.base.track.AbsTrack;
import com.ivy.base.track.EventParams;
import com.ivy.base.track.IConversationCallback;
import com.ivy.base.track.TrackPlatform;
import com.ivy.base.util.Util;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.base.TTAdRevenueEvent;
import com.tiktok.appevents.base.TTBaseEvent;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class PangleTrack extends AbsTrack {
    @Override
    public void setup(Context context, String appId, String config, boolean debug, IConversationCallback conversationCallback, Runnable readyListener) {
        super.setup(context, appId, config, debug, conversationCallback, readyListener);
        try {
            JSONObject c = new JSONObject(config);
            if (c.has("secret")) {
                TikTokBusinessSdk.TTConfig tt = new TikTokBusinessSdk.TTConfig(context, c.optString("secret"))
                        .setAppId(context.getPackageName())
                        .setTTAppId(c.optString("ttappid"));
                if (debug){
                    tt.openDebugMode();
                    tt.setLogLevel(TikTokBusinessSdk.LogLevel.DEBUG);
                } else {
                    tt.setLogLevel(TikTokBusinessSdk.LogLevel.NONE);
                }
                TikTokBusinessSdk.initializeSdk(tt, new TikTokBusinessSdk.TTInitCallback() {
                    @Override
                    public void success() {
                        // SDK initialized successfully
                        Log.i("hoho", "tt init success");
                    }

                    @Override
                    public void fail(int code, String msg) {
                        // SDK initialisation failed
                        Log.i("hoho", "tt init failed with " + code + " msg " + msg);
                    }
                });
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms) {
        if ("af_ad_revenue".equals(eventName) && params != null){
            try {
                String platform = (String) params.get(EventParams.EVENT_PARAM_AD_NETWORK);
                JSONObject adRevenueJson = new JSONObject();
                adRevenueJson.put("ad_unit_id", params.get(EventParams.EVENT_PARAM_AD_UNIT));
                adRevenueJson.put("ad_format", params.get(EventParams.EVENT_PARAM_AD_FORMAT));
                if ("admob".equals(platform)) {
                    // 公共收益事件已将 AdMob 的微单位转为普通金额，这里还原为文档要求的微单位。
                    double revenue = ((Number) params.get(EventParams.EVENT_PARAM_REVENUE)).doubleValue();
                    adRevenueJson.put("value", Math.round(revenue * 1_000_000.0));
                    adRevenueJson.put("currency_code", params.get(EventParams.EVENT_PARAM_CURRENCY));
                    adRevenueJson.put("ad_source_name", params.get(EventParams.EVENT_PARAM_AD_MEDIATION));
                    adRevenueJson.put("device_ad_mediation_platform", "admob_sdk");
                } else {
                    adRevenueJson.put("revenue", params.get(EventParams.EVENT_PARAM_REVENUE));
                    adRevenueJson.put("country_code", Util.countryCode());
                    adRevenueJson.put("network_name", params.get(EventParams.EVENT_PARAM_AD_MEDIATION));
                    adRevenueJson.put("placement", params.get(EventParams.EVENT_PARAM_AD_PLACEMENT));
                    adRevenueJson.put("device_ad_mediation_platform", "max".equals(platform) ? "applovin_max_sdk" : platform);
                    if (!"max".equals(platform)) {
                        adRevenueJson.put("currency", params.get(EventParams.EVENT_PARAM_CURRENCY));
                    }
                }
                TTBaseEvent adRevenueInfo = TTAdRevenueEvent.newBuilder(adRevenueJson).build();
                TikTokBusinessSdk.trackTTEvent(adRevenueInfo);
            } catch (Exception e) {
                Log.e("hoho", "logEvent failed: " + e.getMessage());
            }
        }
    }

    @Override
    public void setUserProperty(String key, Object value) {

    }
}
