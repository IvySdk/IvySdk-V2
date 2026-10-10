package com.ivy.base.track;

import android.os.Bundle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IEvent {

    void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms);


    default void logEvent(String eventName, String eventType, String eventSrc) {
        logEvent(eventName, eventType, eventSrc, null, null);
    }

    private Map<String, Object> bundle2Map(Bundle bundle) {
        Map<String, Object> map = new HashMap<>();
        try {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                map.put(key, value);
            }
        } catch (Exception ignore) {
        }
        return map;
    }

    default void logEvent(String eventName, Bundle bundle){
        logEvent(eventName, bundle.getString("event_type"), bundle.getString("event_src"), bundle2Map(bundle));
    }

    default void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params) {
        logEvent(eventName, eventType, eventSrc, params, null);
    }

}