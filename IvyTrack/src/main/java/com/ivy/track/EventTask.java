package com.ivy.track;

import com.ivy.base.track.TrackPlatform;

import java.util.List;
import java.util.Map;

public class EventTask {
    private String eventName;
    private String eventType;
    private String eventSrc;
    private Map<String, Object> params;
    private List<TrackPlatform> platforms;

    public EventTask(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms) {
        this.eventName = eventName;
        this.eventType = eventType;
        this.eventSrc = eventSrc;
        this.params = params;
        this.platforms = platforms;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventSrc() {
        return eventSrc;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public List<TrackPlatform> getPlatforms() {
        return platforms;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setEventSrc(String eventSrc) {
        this.eventSrc = eventSrc;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public void setPlatforms(List<TrackPlatform> platforms) {
        this.platforms = platforms;
    }
}
