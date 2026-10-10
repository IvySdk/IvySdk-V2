package com.ivy.core.util;

import com.ivy.base.local.LocalStorage;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;
import com.ivy.base.track.IKeys;
import com.ivy.track.IvyTrack;

import java.util.HashMap;
import java.util.Map;

public class KwaiUtil {

    public void checkAdRevenue(double revenue) {
        double totalRevenue = LocalStorage.Instance.decodeDouble(IKeys.KEY_KWAI_TOTAL_AD_REVENUE, 0.0) + revenue;
        LocalStorage.Instance.encodeDouble(IKeys.KEY_KWAI_TOTAL_AD_REVENUE, totalRevenue);

        if (totalRevenue >= 0.05 && !LocalStorage.Instance.containsKey("cpm_total_0_05")) {
            LocalStorage.Instance.encodeBoolean("cpm_total_0_05", true);
            Map<String, Object> params = new HashMap<>();
            params.put("kwai_key_event_action_type", 2);
            params.put("kwai_key_event_action_value", 0.05);
            IvyTrack.getInstance().logEvent("cpm_total_0_05", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params);
        }

        if (totalRevenue >= 0.1 && !LocalStorage.Instance.containsKey("cpm_total_0_1")) {
            LocalStorage.Instance.encodeBoolean("cpm_total_0_1", true);
            Map<String, Object> params = new HashMap<>();
            params.put("kwai_key_event_action_type", 2);
            params.put("kwai_key_event_action_value", 0.1);
            IvyTrack.getInstance().logEvent("cpm_total_0_1", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params);
        }

        if (totalRevenue >= 0.05 && !LocalStorage.Instance.containsKey("revenue_total_day0_0_05")) {
            LocalStorage.Instance.encodeBoolean("revenue_total_day0_0_05", true);
            Map<String, Object> params = new HashMap<>();
            params.put("kwai_key_event_action_type", 4);
            params.put("kwai_key_event_action_value", 0.05);
            IvyTrack.getInstance().logEvent("revenue_total_day0_0_05", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params);
        }

        if (totalRevenue >= 0.1 && !LocalStorage.Instance.containsKey("revenue_total_day0_0_1")) {
            LocalStorage.Instance.encodeBoolean("revenue_total_day0_0_1", true);
            Map<String, Object> params = new HashMap<>();
            params.put("kwai_key_event_action_type", 4);
            params.put("kwai_key_event_action_value", 0.1);
            IvyTrack.getInstance().logEvent("revenue_total_day0_0_1", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params);
        }
    }

    public void checkAdEvents(String eventName, Map<String, Object> params) {
        int showTimes = LocalStorage.Instance.decodeInt(IKeys.KEY_KWAI_AD_SHOW_TIMES, 0) + 1;
        LocalStorage.Instance.encodeInt(IKeys.KEY_KWAI_AD_SHOW_TIMES, showTimes);

        if (showTimes == 1) {
            logAdShowEvent(1, 1);
        }
        if (showTimes == 3) {
            logAdShowEvent(1, 3);
        }
        if (showTimes == 5) {
            logAdShowEvent(1, 5);
        }
        if (showTimes == 10) {
            logAdShowEvent(1, 10);
        }
    }

    private void logAdShowEvent(int type, int times) {
        Map<String, Object> params = new HashMap<>();
        params.put("kwai_key_event_action_type", type);
        params.put("kwai_key_event_action_value", times);
        IvyTrack.getInstance().logEvent("ad_show" + times, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params);
    }
}
