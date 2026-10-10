package com.ivy.crosspromotion;

public interface CrossPromotionListener {
    /** The startup server check completed. updated=false means HTTP 304 or same version. */
    default void onConfigChecked(String version, boolean updated) {
    }

    /** The server check failed; the current local or last-known-good config remains active. */
    default void onConfigCheckFailed(String reason) {
    }



    default void onImpression(String format, String adId) {
    }

    default void onClick(String format, String adId) {
    }

    default void onReward(String format, String adId) {
    }

    default void onClosed(String format, String adId) {
    }
}
