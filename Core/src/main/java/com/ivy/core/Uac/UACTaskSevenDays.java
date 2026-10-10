package com.ivy.core.Uac;

import com.ivy.base.local.LocalStorage;
import com.ivy.track.IvyTrack;

import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class UACTaskSevenDays extends AbsUACTask {

    @Override
    public void formatSrcData(JSONObject data) {
        if (data.has("t10")) {
            double top10 = data.optDouble("t10");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_DAY7_TOP10, top10);
        }
        if (data.has("t20")) {
            double top20 = data.optDouble("t20");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_DAY7_TOP20, top20);
        }
        if (data.has("t30")) {
            double top30 = data.optDouble("t30");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_DAY7_TOP30, top30);
        }
        if (data.has("t40")) {
            double top40 = data.optDouble("t40");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_DAY7_TOP40, top40);
        }
        if (data.has("t50")) {
            double top50 = data.optDouble("t50");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_DAY7_TOP50, top50);
        }
        if (data.has("t60")) {
            double top60 = data.optDouble("t60");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_DAY7_TOP60, top60);
        }
        if (data.has("t70")) {
            double top70 = data.optDouble("t70");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_DAY7_TOP70, top70);
        }
        if (data.has("t80")) {
            double top80 = data.optDouble("t80");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_DAY7_TOP80, top80);
        }
        if (data.has("t90")) {
            double top90 = data.optDouble("t90");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_DAY7_TOP90, top90);
        }
    }

    private boolean logUacConversionEvent(String tag, float totalRevenue, int hours) {
        String sendFlag = "AdLtv_day7_top" + tag + "_gen";
        boolean alreadySent = LocalStorage.Instance.decodeBoolean(sendFlag, false);
        if (alreadySent) return false;

        String eventName = "AdLtv_day7_top" + tag;
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
        if (hours > 168) return;

        double[] tops = new double[] {
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_DAY7_TOP90, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_DAY7_TOP80, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_DAY7_TOP70, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_DAY7_TOP60, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_DAY7_TOP50, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_DAY7_TOP40, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_DAY7_TOP30, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_DAY7_TOP20, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_DAY7_TOP10, 0.0)
        };

        String[] topTags = new String[] {"90", "80", "70", "60", "50", "40", "30", "20", "10"};
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
