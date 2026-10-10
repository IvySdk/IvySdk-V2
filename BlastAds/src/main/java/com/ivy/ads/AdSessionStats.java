package com.ivy.ads;

import com.ivy.base.ad.AdType;
import com.ivy.base.local.LocalStorage;

/** Owns the existing per-session revenue counters used by MAX segmentation. */
final class AdSessionStats {
    private AdSessionStats() { }

    static void recordRevenue(BaseAd adConfig) {
        try {
            String adType = adConfig.getAdType().getValue();

            // Get current session ID
            String sessionCountKey = "ivy_session_count";
            int currentSession = LocalStorage.Instance.decodeInt(sessionCountKey, 0);

            // Current session's accumulated eCPM value and count
            String sessionEcpmKey = "ivy_session_" + currentSession + "_ecpm_" + adType;
            String sessionCounterKey = "ivy_session_" + currentSession + "_counter_" + adType;

            // Update current session's accumulated eCPM value and count
            float currentEcpm = LocalStorage.Instance.decodeFloat(sessionEcpmKey, 0.0f);
            int currentCount = LocalStorage.Instance.decodeInt(sessionCounterKey, 0);

            float newEcpm = currentEcpm + (float) (adConfig.getPrice() * 1000.0);
            int newCount = currentCount + 1;

            LocalStorage.Instance.encodeFloat(sessionEcpmKey, newEcpm, 0);
            LocalStorage.Instance.encodeInt(sessionCounterKey, newCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void startSession(int keepEcpmSessions) {
        String sessionCountKey = "ivy_session_count";
        int currentSession = LocalStorage.Instance.decodeInt(sessionCountKey, 0);
        // Increment session count
        LocalStorage.Instance.encodeInt(sessionCountKey, currentSession + 1);

        int cleanCount = keepEcpmSessions;
        if (currentSession > cleanCount) {
            // Clean old session data, but keep the most recent cleanCount records

            // Calculate the earliest session ID to keep
            int keepFromSession = currentSession - cleanCount;

            // Clean session data for each ad type
            String[] adTypes = {AdType.INTERSTITIAL.getValue(), AdType.REWARDED.getValue(), AdType.BANNER.getValue()};
            for (String adType : adTypes) {
                String sessionEcpmKey = "ivy_session_" + keepFromSession + "_ecpm_" + adType;
                String sessionCounterKey = "ivy_session_" + keepFromSession + "_counter_" + adType;

                // Delete old session data
                LocalStorage.Instance.removeValueForKey(sessionEcpmKey);
                LocalStorage.Instance.removeValueForKey(sessionCounterKey);
            }
        }
    }
}
