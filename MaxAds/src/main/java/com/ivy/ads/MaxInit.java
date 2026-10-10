package com.ivy.ads;

import android.app.Activity;
import android.util.Log;

import com.adsurge.adn.ads.AdSurgePrivacyConfiguration;
import com.applovin.mediation.MaxSegmentCollection;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkInitializationConfiguration;
import com.applovin.sdk.AppLovinSdkSettings;
import com.ivy.ads.cmp.CMPUtil;
import com.ivy.base.App;
import com.ivy.base.IUnProguard;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class MaxInit implements IPlatformInit, IUnProguard {
    private static final String TAG = "MaxInit";
    public static ArrayList<String> selectiveAdUnitIds = new ArrayList<>();
    public boolean disableAxonEvents = true;

    @Override
    public void logEvent(String eventName, Map<String, Object> map) {
        try {
            if (disableAxonEvents) return;
            AppLovinSdk sdk = AppLovinSdk.getInstance(App.getInstance());
            if (sdk.isInitialized()) {
                if (map != null) {
                    sdk.getEventService().trackEvent(eventName, map);
                } else {
                    sdk.getEventService().trackEvent(eventName);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void init(Activity activity, JSONObject config, boolean debug, InitCallback listener) {
        CMPUtil.getInstance().init(activity, debug, ableLoadAds -> {
            AdSurgePrivacyConfiguration.setAgeRestrictedUser(false); // User is not below age restriction
            AdSurgePrivacyConfiguration.setDoNotSell(false); // Allow data to be used for advertising
            AdSurgePrivacyConfiguration.setUserConsent(true); // User has given consent
            configureXiaomiIfPresent(activity, config, debug);
            initializeMax(activity, config, debug, listener);
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
            Log.e(TAG, "Failed to configure Xiaomi Ads before MAX initialization", throwable);
        }
    }

    private void initializeMax(Activity activity, JSONObject config, boolean debug, InitCallback listener) {
        disableAxonEvents = config.optBoolean("disableAxonEvents", true);
        TimeUtil.getCurrentHourSinceInstall();

        AppLovinSdkSettings settings = AppLovinSdk.getInstance(activity).getSettings();
        if (debug) settings.setVerboseLogging(true);

//        String policy_url = config.optString("policy");
//        String terms_url = config.optString("terms");
//        if (!TextUtils.isEmpty(policy_url) && !TextUtils.isEmpty(terms_url)) {
//            settings.getTermsAndPrivacyPolicyFlowSettings().setEnabled(true);
//            settings.getTermsAndPrivacyPolicyFlowSettings().setPrivacyPolicyUri(Uri.parse(policy_url));
//            // Terms of Service URL is optional
//            settings.getTermsAndPrivacyPolicyFlowSettings().setTermsOfServiceUri(Uri.parse(terms_url));
//            // Showing Terms & Privacy Policy flow in GDPR region is optional (disabled by default)
//            settings.getTermsAndPrivacyPolicyFlowSettings().setShowTermsAndPrivacyPolicyAlertInGdpr(false);
//
//            if (debug) {
//                settings.getTermsAndPrivacyPolicyFlowSettings().setDebugUserGeography(AppLovinSdkConfiguration.ConsentFlowUserGeography.GDPR);
//                settings.setExtraParameter("google_test_device_hashed_id", "D8AF4EA57317FAF9D825B2BD1E570B17");
//            }
//        }

        AppLovinSdkInitializationConfiguration.Builder configBuilder = AppLovinSdkInitializationConfiguration.builder(config.optString("sdkKey", ""), activity);
        // 设置mediation provider
        configBuilder.setMediationProvider("max");
//        if (!AppLovinPrivacySettings.isUserConsentSet(activity.getApplicationContext())) {
//            AppLovinPrivacySettings.setHasUserConsent(true, activity.getApplicationContext());
//        }
//
//        if (!AppLovinPrivacySettings.isDoNotSellSet(activity.getApplicationContext())) {
//            AppLovinPrivacySettings.setDoNotSell(true, activity.getApplicationContext());
//        }

        // 设置segment数据
        MaxSegmentCollection segmentCollection = MaxSegmentManager.mSegmentCollection == null ? null : MaxSegmentManager.mSegmentCollection.build();
        if (segmentCollection != null) {
            configBuilder.setSegmentCollection(segmentCollection);
        }

        // 设置选择性初始化的广告单元ID
        if (!selectiveAdUnitIds.isEmpty()) {
            configBuilder.setAdUnitIds(selectiveAdUnitIds);
            if (config.optBoolean("disableB2B", false)) {
                StringBuilder sb = new StringBuilder();
                for (String id : selectiveAdUnitIds) {
                    sb.append(id).append(",");
                }
                AppLovinSdk.getInstance(activity).getSettings().setExtraParameter("disable_b2b_ad_unit_ids", sb.substring(0, sb.length() - 1));
            }
        }

        // 初始化SDK
        AppLovinSdk.getInstance(activity).initialize(configBuilder.build(), configuration -> {
            if (listener != null) {
                listener.onInitialized(configuration.getCountryCode(), "max");
            }
            if (debug) {
                AppLovinSdk.getInstance(activity).showMediationDebugger();
            }
        });
    }

}
