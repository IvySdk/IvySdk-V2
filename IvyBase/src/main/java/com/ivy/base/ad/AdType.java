package com.ivy.base.ad;

/**
 * Ad type enumeration
 */
public enum AdType {
    BANNER("banner"),
    INTERSTITIAL("interstitial"),
    REWARDED("rewarded"),
    NATIVE("native"),
    SPLASH("splash");

    public final String value;

    AdType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
