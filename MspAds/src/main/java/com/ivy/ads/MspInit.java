package com.ivy.ads;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ivy.base.IUnProguard;
import com.particles.msp.api.MSPInitListener;
import com.particles.msp.api.MSPInitStatus;
import com.particles.msp.api.MSPInitializationParameters;
import com.particles.prebidadapter.MSP;

import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

public class MspInit implements IPlatformInit, IUnProguard {
    @Override
    public void init(Activity activity, JSONObject config, boolean debug, InitCallback listener) {
        final String apiKey = config.optString("apiKey", "");
        final String prebidUrl = config.optString("hostUrl", "https://msp.newsbreak.com/openrtb2/auction");
        final int orgId = config.optInt("orgId", 0);
        final int appId = config.optInt("appId", 0);
        MSP.INSTANCE.init(activity, new MSPInitializationParameters() {

            @NonNull
            @Override
            public String getPrebidAPIKey() {
                return apiKey;
            }

            @NonNull
            @Override
            public String getPrebidHostUrl() {
                return prebidUrl;
            }

            @Override
            public int getOrgId() {
                return orgId;
            }

            @Override
            public int getAppId() {
                return appId;
            }

            @NonNull
            @Override
            public Map<String, Object> getParameters() {
                return Collections.emptyMap();
            }

            @Override
            public boolean hasUserConsent() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                // 获取 TCF 定义的 Purpose 1 (基础广告权限)
                String purposeConsent = prefs.getString("IABTCF_PurposeConsents", "");
                if (!purposeConsent.isEmpty()) {
                    // 第一位如果是 '1'，代表同意了基础权限
                    return purposeConsent.charAt(0) == '1';
                }
                return true;
            }

            @Override
            public boolean isAgeRestrictedUser() {
                return false;
            }

            @Override
            public boolean isDoNotSell() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                String uspString = prefs.getString("IABUSPrivacy_String", "");
                // 字符串第三位是 'Y' 表示用户 Opt-Out (DoNotSell = true)
                if (uspString.length() >= 3) {
                    return uspString.charAt(2) == 'Y';
                }
                return false;
            }

            @NonNull
            @Override
            public String getConsentString() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                // 获取完整的 TC String
                return prefs.getString("IABTCF_TCString", "");
            }

            @Override
            public boolean isInTestMode() {
                return debug;
            }
        }, new MSPInitListener() {
            @Override
            public void onComplete(@NonNull MSPInitStatus mspInitStatus, @NonNull String s) {
                Log.d("hoho", "msp init " + mspInitStatus.getMessage() + " s " + s);
                listener.onInitialized("", "msp");
            }
        }, false);
    }

    @Override
    public void logEvent(String eventName, Map<String, Object> map) {

    }
}
