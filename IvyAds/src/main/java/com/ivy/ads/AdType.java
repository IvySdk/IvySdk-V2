package com.ivy.ads;

// AdType 枚举示例（需要根据实际实现调整）
public enum AdType {
    INTERSTITIAL("inter"), REWARDED("video"), BANNER("banner");

    private final String value;

    AdType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
