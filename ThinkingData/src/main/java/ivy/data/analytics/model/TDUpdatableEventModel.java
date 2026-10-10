/*
 * Copyright (C) 2023 ThinkingData
 */
package ivy.data.analytics.model;
import org.json.JSONObject;
import ivy.data.analytics.TDUpdatableEvent;
/**
 *
 * @author liulongbing
 * @since 2023/7/20
 */
public class TDUpdatableEventModel extends TDUpdatableEvent {
    public TDUpdatableEventModel(String eventName, JSONObject properties, String eventId) {
        super(eventName, properties, eventId);
    }
}
