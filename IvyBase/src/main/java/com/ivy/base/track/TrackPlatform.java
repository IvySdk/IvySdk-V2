package com.ivy.base.track;

public enum TrackPlatform {
    NONE("none"),
    FACEBOOK("facebook"),
    FIREBASE("firebase"),
    APPSFLYER("appsflyer"),
    THINKING_DATA("thinkingData"),
    PANGLE("pangle"),
    AXON("axon");

    private final String value;

    TrackPlatform(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}