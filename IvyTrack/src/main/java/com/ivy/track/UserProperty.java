package com.ivy.track;

import com.ivy.base.track.TrackPlatform;

public class UserProperty {
    private String key;
    private Object value;
    private TrackPlatform platform;

    public UserProperty(String key, Object value, TrackPlatform platform) {
        this.key = key;
        this.value = value;
        this.platform = platform;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public TrackPlatform getPlatform() {
        return platform;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setPlatform(TrackPlatform platform) {
        this.platform = platform;
    }
}
