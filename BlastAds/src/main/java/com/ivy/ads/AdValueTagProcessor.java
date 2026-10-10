package com.ivy.ads;

import android.text.TextUtils;
import android.util.Log;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.track.ITrack;
import com.ivy.remote.IvyRemoteConfig;
import org.json.JSONObject;

public class AdValueTagProcessor {
    private ITrack track;
    private static final String TAG = "hoho";
    private static final String KEY_MAX_ECPM = "ad_max_ecpm";
    private static final String KEY_FINAL_TAG = "_sdk_ad_value_";

    private boolean isTrackingEnabled = false; // 对应原代码 f111955e
    private boolean hasTag = false;
    private int sessionLoadCount = 0;          // 对应原代码 f111956f (内存变量)

    public AdValueTagProcessor() {
        init();
    }

    private static AdValueTagProcessor instance;
    public static AdValueTagProcessor getInstance(){
        if (instance == null){
            instance = new AdValueTagProcessor();
        }
        return instance;
    }

    public void setTrack(ITrack track) {
        this.track = track;
        track.setUserProperty("ad_value", getAdValue());
    }

    public String getAdValue(){
        return LocalStorage.Instance.decodeString(KEY_FINAL_TAG, "unset");
    }

    private void init() {
        String savedTag = LocalStorage.Instance.decodeString(KEY_FINAL_TAG, "");
        float savedEcpm = LocalStorage.Instance.decodeFloat(KEY_MAX_ECPM, 0.0f);

        if (!TextUtils.isEmpty(savedTag)) {
            hasTag = true;
            // 已经有标签了，不追踪
            isTrackingEnabled = false;
        } else if (savedEcpm == 0.0f) {
            // 【关键点】只有记录为0，才允许开启追踪
            isTrackingEnabled = true;
            Log.d(TAG, "纯新用户，开启追踪");
        } else {
            // 虽然没标签，但ecpm已经有值了(上个session存的)，不开启追踪
            isTrackingEnabled = false;
            Log.d(TAG, "已有ecpm记录，不再追踪更新");

            // 但是，如果配置到了，还是要尝试定级一次（结账）
            trySegment(savedEcpm);
        }
    }

    public void onAdLoaded(double currentEcpm) {
        Log.d(TAG, "on ad loaded " + currentEcpm);
        if (!isTrackingEnabled) {
            if (!hasTag){
                trySegment(LocalStorage.Instance.decodeFloat(KEY_MAX_ECPM, 0.0f));
            }
            return;
        }

        // 内存计数
        sessionLoadCount++;

        // 对应原代码：if (i10 <= 3 && dC >= dU)
        if (sessionLoadCount <= 3) {
            float lastMax = LocalStorage.Instance.decodeFloat(KEY_MAX_ECPM, 0.0f);
            if (currentEcpm > lastMax) {
                LocalStorage.Instance.encodeFloat(KEY_MAX_ECPM, (float) currentEcpm, 0);
                Log.d(TAG, "Session内更新eCPM: " + currentEcpm);
                trySegment((float) currentEcpm);
            }
        }
    }

    private void trySegment(float ecpm) {
        String configJson = IvyRemoteConfig.getInstance().getString("ad_value");
        // 原代码逻辑：没有配置，直接 return，不产生标签
        if (TextUtils.isEmpty(configJson)) {
            Log.d(TAG, "not remote config value");
            return;
        }

        try {
            JSONObject o = new JSONObject(configJson);
            float high = (float) o.optDouble("high", 30.0);
            float med = (float) o.optDouble("med", 10.0);

            String tag = ecpm >= high ? "high" : (ecpm >= med ? "medium" : "low");
            LocalStorage.Instance.encodeString(KEY_FINAL_TAG, tag);
            track.setUserProperty("ad_value", tag);
            hasTag = true;
            Log.i(TAG, "定级成功: " + tag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}