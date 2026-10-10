package com.ivy.appsflyer;

import android.text.TextUtils;

import com.appsflyer.AFInAppEventType;
import com.ivy.base.ILog;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.track.EventIDs;
import com.tencent.mmkv.MMKV;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class PendingEvent {
    final String id;
    final String name;
    final Map<String, Object> params;
    final long createdAt;
    int retryCount;
    long nextRetryAt;

    PendingEvent(String name, Map<String, Object> params) {
        this(UUID.randomUUID().toString(), name, params, System.currentTimeMillis(), 0, 0L);
    }

    PendingEvent(String id, String name, Map<String, Object> params, long createdAt,
                 int retryCount, long nextRetryAt) {
        this.id = id;
        this.name = name;
        this.params = params;
        this.createdAt = createdAt;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
    }
}

final class AppsflyerEventOutbox {
    private static final String STORE_ID = "af_event_outbox_v1";
    private static final String EVENT_KEY_PREFIX = "af_event:";
    private static final String EVENT_SEQUENCE_KEY = "af_event_sequence";
    private static final int MAX_PENDING_EVENTS = 10000;

    private final Map<String, PendingEvent> events = new LinkedHashMap<>();
    private MMKV store;
    private long eventSequence;

    synchronized void init() {
        try {
            LocalStorage.Instance.checkInit();
            store = MMKV.mmkvWithID(STORE_ID);
            eventSequence = store.decodeLong(EVENT_SEQUENCE_KEY, 0L);
            loadEvents();
            ILog.i(AppsflyerInit.TAG, "load persisted events:" + events.size());
        } catch (Throwable throwable) {
            ILog.e(AppsflyerInit.TAG, "init event outbox failed:" + throwable.getMessage());
            store = null;
        }
    }

    synchronized PendingEvent enqueue(String name, Map<String, Object> params, String protectedEventId) {
        if (EventIDs.SDK_FIRST_APP_OPEN.equals(name) && containsEvent(name)) {
            ILog.i(AppsflyerInit.TAG, "ignore duplicated pending first open event");
            return null;
        }

        PendingEvent event = new PendingEvent(name,
                params != null ? new HashMap<>(params) : new HashMap<>());
        event.params.put("event_seq", nextEventSequence());
        if (!makeRoom(event, protectedEventId)) {
            ILog.w(AppsflyerInit.TAG, "event outbox is full, drop event:" + name);
            return null;
        }
        event.params.put("ivy_event_id", event.id);
        event.params.put("client_event_time", event.createdAt);
        events.put(event.id, event);
        persist(event);
        return event;
    }

    synchronized PendingEvent next(long now) {
        PendingEvent selected = null;
        for (PendingEvent event : events.values()) {
            if (event.nextRetryAt > now) {
                continue;
            }
            if (selected == null || priority(event.name) > priority(selected.name)
                    || (priority(event.name) == priority(selected.name)
                    && event.createdAt < selected.createdAt)) {
                selected = event;
            }
        }
        return selected;
    }

    synchronized long earliestRetryAt(long now) {
        long earliest = Long.MAX_VALUE;
        for (PendingEvent event : events.values()) {
            if (event.nextRetryAt > now && event.nextRetryAt < earliest) {
                earliest = event.nextRetryAt;
            }
        }
        return earliest == Long.MAX_VALUE ? 0L : earliest;
    }

    synchronized PendingEvent remove(String eventId) {
        PendingEvent event = events.remove(eventId);
        if (store != null) {
            store.removeValueForKey(eventKey(eventId));
        }
        return event;
    }

    synchronized PendingEvent markForRetry(String eventId) {
        PendingEvent event = events.get(eventId);
        if (event == null) {
            return null;
        }
        event.retryCount++;
        event.nextRetryAt = System.currentTimeMillis() + retryDelay(event.retryCount);
        event.params.put("is_retry", true);
        persist(event);
        return event;
    }

    private void loadEvents() {
        if (store == null) {
            return;
        }
        String[] keys = store.allKeys();
        if (keys == null) {
            return;
        }
        for (String key : keys) {
            if (key == null || !key.startsWith(EVENT_KEY_PREFIX)) {
                continue;
            }
            try {
                PendingEvent event = fromJson(store.decodeString(key, ""));
                if (event == null) {
                    store.removeValueForKey(key);
                    continue;
                }
                event.params.put("is_outbox_replay", true);
                events.put(event.id, event);
            } catch (Exception e) {
                ILog.e(AppsflyerInit.TAG, "load persisted event failed:" + e.getMessage());
                store.removeValueForKey(key);
            }
        }
    }

    private boolean persist(PendingEvent event) {
        if (store == null) {
            ILog.e(AppsflyerInit.TAG, "persist event failed: event store unavailable");
            return false;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("id", event.id);
            json.put("name", event.name);
            json.put("created_at", event.createdAt);
            json.put("retry_count", event.retryCount);
            json.put("next_retry_at", event.nextRetryAt);
            json.put("params", new JSONObject(event.params));
            boolean success = store.encode(eventKey(event.id), json.toString());
            if (!success) {
                ILog.e(AppsflyerInit.TAG, "persist event failed:" + event.name);
            }
            return success;
        } catch (Exception e) {
            ILog.e(AppsflyerInit.TAG, "persist event failed:" + e.getMessage());
            return false;
        }
    }

    private PendingEvent fromJson(String data) throws Exception {
        if (TextUtils.isEmpty(data)) {
            return null;
        }
        JSONObject json = new JSONObject(data);
        String id = json.optString("id");
        String name = json.optString("name");
        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(name)) {
            return null;
        }
        return new PendingEvent(id, name, jsonToMap(json.optJSONObject("params")),
                json.optLong("created_at", System.currentTimeMillis()),
                json.optInt("retry_count", 0), json.optLong("next_retry_at", 0L));
    }

    private Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<>();
        if (json == null) {
            return map;
        }
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.opt(key);
            if (value != null && value != JSONObject.NULL) {
                map.put(key, jsonToJavaValue(value));
            }
        }
        return map;
    }

    private Object jsonToJavaValue(Object value) {
        if (value instanceof JSONObject) {
            return jsonToMap((JSONObject) value);
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            ArrayList<Object> values = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                Object item = array.opt(i);
                if (item != null && item != JSONObject.NULL) {
                    values.add(jsonToJavaValue(item));
                }
            }
            return values;
        }
        return value;
    }

    private boolean containsEvent(String name) {
        for (PendingEvent event : events.values()) {
            if (name.equals(event.name)) {
                return true;
            }
        }
        return false;
    }

    private long nextEventSequence() {
        long next = ++eventSequence;
        if (store != null) {
            store.encode(EVENT_SEQUENCE_KEY, next);
        }
        return next;
    }

    private boolean makeRoom(PendingEvent incoming, String protectedEventId) {
        if (events.size() < MAX_PENDING_EVENTS) {
            return true;
        }
        PendingEvent victim = null;
        for (PendingEvent event : events.values()) {
            if (EventIDs.SDK_FIRST_APP_OPEN.equals(event.name) || event.id.equals(protectedEventId)) {
                continue;
            }
            if (victim == null || priority(event.name) < priority(victim.name)
                    || (priority(event.name) == priority(victim.name)
                    && event.createdAt < victim.createdAt)) {
                victim = event;
            }
        }
        if (victim == null || priority(incoming.name) < priority(victim.name)) {
            return false;
        }
        remove(victim.id);
        ILog.w(AppsflyerInit.TAG, "event outbox is full, evict event:" + victim.name);
        return true;
    }

    private int priority(String name) {
        if (AFInAppEventType.PURCHASE.equals(name)) {
            return 100;
        }
        if (EventIDs.SDK_FIRST_APP_OPEN.equals(name)) {
            return 90;
        }
        if (EventIDs.EVENT_APP_OPEN.equals(name)) {
            return 80;
        }
        if ("af_ad_revenue".equals(name)) {
            return 70;
        }
        return 10;
    }

    private long retryDelay(int retryCount) {
        if (retryCount <= 1) {
            return 30000L;
        }
        if (retryCount == 2) {
            return 2 * 60 * 1000L;
        }
        if (retryCount == 3) {
            return 10 * 60 * 1000L;
        }
        return 30 * 60 * 1000L;
    }

    private String eventKey(String eventId) {
        return EVENT_KEY_PREFIX + eventId;
    }
}
