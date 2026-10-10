package com.ivy.core.Uac;

import com.ivy.base.local.LocalStorage;
import com.ivy.track.IvyTrack;

import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class UACTaskThreeDays extends AbsUACTask {

    @Override
    public void formatSrcData(JSONObject data) {
        if (data.has("t30")) {
            double day3Top30 = data.optDouble("t30");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_DAY3_TOP30, day3Top30);
        }
        if (data.has("t20")) {
            double day3Top20 = data.optDouble("t20");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_DAY3_TOP20, day3Top20);
        }
        if (data.has("t10")) {
            double day3Top10 = data.optDouble("t10");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_DAY3_TOP10, day3Top10);
        }
    }

    private boolean logUacConversionEvent(String tag, float totalRevenue, int hours) {
        String sendFlag = "AdLtv_day3_top" + tag + "_gen";
        boolean alreadySent = LocalStorage.Instance.decodeBoolean(sendFlag, false);
        if (alreadySent) return false;

        String eventName = "AdLtv_day3_top" + tag;
        Map<String, Object> params = new HashMap<>();
        params.put("total_revenue", totalRevenue);
        params.put("value", totalRevenue);
        params.put("currency", "USD");
        params.put("hours", hours);
        params.put("catalog", "day3");

        doLogUacConversionEvent(eventName, params);
        LocalStorage.Instance.encodeBoolean(sendFlag, true);
        return true;
    }

    @Override
    public void checkUacLtvConversion(float revenue, float totalRevenue) {
        long duration = System.currentTimeMillis() - IvyTrack.getInstance().getFirstOpenTime();
        if (duration <= 0) return;

        int hours = (int) (duration / 1000 / 60 / 60);
        if (hours > 72) return;

        double[] tops = new double[] {
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_DAY3_TOP30, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_DAY3_TOP20, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_DAY3_TOP10, 0.0)
        };

        String[] topTags = new String[] {"30", "20", "10"};
        boolean result;
        for (int i = 0; i < tops.length; i++) {
            if (tops[i] > 0 && totalRevenue >= tops[i]) {
                result = logUacConversionEvent(topTags[i], totalRevenue, hours);
                if (result) {
                    return; // 生成事件后立即返回
                }
            }
        }
    }
}
