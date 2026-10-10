package com.ivy.core.Uac;

import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;
import com.ivy.track.IvyTrack;

import org.json.JSONObject;
import java.util.Map;

abstract class AbsUACTask {

    public abstract void formatSrcData(JSONObject data);

    public abstract void checkUacLtvConversion(float revenue, float totalRevenue);

    protected void doLogUacConversionEvent(String eventName, Map<String, Object> params) {
        IvyTrack.getInstance().logEvent(
                eventName,
                EventType.EVENT_TYPE_COMMON,
                EventSrc.EVENT_SRC_SDK,
                params
        );
    }
}
