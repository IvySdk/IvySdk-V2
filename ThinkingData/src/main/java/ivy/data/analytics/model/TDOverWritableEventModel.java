/*
 * Copyright (C) 2023 ThinkingData
 */
package ivy.data.analytics.model;
import org.json.JSONObject;
import ivy.data.analytics.TDOverWritableEvent;

/**
 *
 * @author liulongbing
 * @since 2023/7/20
 */
public class TDOverWritableEventModel extends TDOverWritableEvent {
    public TDOverWritableEventModel(String eventName, JSONObject properties, String eventId) {
        super(eventName, properties, eventId);
    }
}
