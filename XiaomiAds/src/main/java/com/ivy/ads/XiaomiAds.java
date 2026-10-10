package com.ivy.ads;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

import com.zeus.gmc.sdk.mobileads.columbus.ad.AdGlobalSdk;

import org.json.JSONObject;

/**
 * Columbus privacy configuration shared by the standalone Xiaomi ad platform.
 */
public final class XiaomiAds {
    private static final String TAG = "XiaomiAds";
    private static final String IAB_GDPR_APPLIES = "IABTCF_gdprApplies";
    private static final String IAB_PURPOSE_CONSENTS = "IABTCF_PurposeConsents";

    private XiaomiAds() {
    }

    static void configure(Activity activity, JSONObject config, boolean debug) {
        boolean consent = resolveConsent(activity, config);
        AdGlobalSdk.setGDPRConsent(consent);
        AdGlobalSdk.setDebugOn(debug);
        AdGlobalSdk.setStaging(debug);
        Log.d(TAG, "Columbus GDPR consent configured to " + consent);
    }

    /** Called reflectively by AdMobAds and MaxAds before their SDK initialization. */
    public static void configureForMediation(Activity activity, JSONObject config, boolean debug) {
        configure(activity, config == null ? new JSONObject() : config, debug);
    }

    private static boolean resolveConsent(Activity activity, JSONObject config) {
        SharedPreferences preferences = activity.getSharedPreferences(
                activity.getPackageName() + "_preferences", Activity.MODE_PRIVATE);
        int gdprApplies = parseInt(preferences.getAll().get(IAB_GDPR_APPLIES), -1);

        // Only enforce consent check when explicitly in a GDPR-applicable region
        if (gdprApplies == 1) {
            String purposeConsents = preferences.getString(IAB_PURPOSE_CONSENTS, "");
            return hasPurposeConsent(purposeConsents, 1);
        }

        // Default to true for non-GDPR regions (0) and unconstrained/standard traffic (-1)
        return true;
    }

    private static int parseInt(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean hasPurposeConsent(String purposeConsents, int purpose) {
        int index = purpose - 1;
        return purposeConsents != null
                && purposeConsents.length() > index
                && purposeConsents.charAt(index) == '1';
    }
}
