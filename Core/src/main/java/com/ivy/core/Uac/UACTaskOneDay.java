package com.ivy.core.Uac;

import com.ivy.base.local.LocalStorage;

import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.HashMap;
import java.util.Map;

public class UACTaskOneDay extends AbsUACTask {

    private String todayKey;

    public UACTaskOneDay() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        todayKey = simpleDateFormat.format(date);
    }

    @Override
    public void formatSrcData(JSONObject data) {
        if (data.has("t10")) {
            double top10 = data.optDouble("t10");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_TOP10, top10);
        }
        if (data.has("t20")) {
            double top20 = data.optDouble("t20");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_TOP20, top20);
        }
        if (data.has("t30")) {
            double top30 = data.optDouble("t30");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_TOP30, top30);
        }
        if (data.has("t40")) {
            double top40 = data.optDouble("t40");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_TOP40, top40);
        }
        if (data.has("t50")) {
            double top50 = data.optDouble("t50");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_TOP50, top50);
        }
        if (data.has("t60")) {
            double top60 = data.optDouble("t60");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_TOP60, top60);
        }
        if (data.has("t70")) {
            double top70 = data.optDouble("t70");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_TOP70, top70);
        }
        if (data.has("t80")) {
            double top80 = data.optDouble("t80");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_TOP80, top80);
        }
        if (data.has("t90")) {
            double top90 = data.optDouble("t90");
            LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_TOP90, top90);
        }
    }

    private boolean logUacConversionEvent(int top, double dayRevenue, double totalRevenue) {
        String sendFlagKey = UACUtil.KEY_UAC_PREFIX_TODAY + todayKey + top + "_gen";
        boolean alreadySent = LocalStorage.Instance.decodeBoolean(sendFlagKey, false);
        if (alreadySent) {
            return false;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("total_revenue", totalRevenue);
        params.put("value", dayRevenue);
        params.put("currency", "USD");
        params.put("day_revenue", dayRevenue);
        params.put("label", todayKey);

        String eventName = UACUtil.PREFIX_EVENT_UAC + top;
        doLogUacConversionEvent(eventName, params);
        LocalStorage.Instance.encodeBoolean(sendFlagKey, true);
        return true;
    }

    @Override
    public void checkUacLtvConversion(float revenue, float totalRevenue) {
        double todayRevenue = LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_PREFIX_TODAY + todayKey, 0.0) + revenue;
        LocalStorage.Instance.encodeDouble(UACUtil.KEY_UAC_PREFIX_TODAY + todayKey, todayRevenue);

        double[] tops = new double[] {
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_TOP90, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_TOP80, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_TOP70, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_TOP60, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_TOP50, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_TOP40, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_TOP30, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_TOP20, 0.0),
                LocalStorage.Instance.decodeDouble(UACUtil.KEY_UAC_TOP10, 0.0)
        };

        int[] topValues = new int[] {90, 80, 70, 60, 50, 40, 30, 20, 10};
        boolean result;
        for (int i = 0; i < tops.length; i++) {
            if (tops[i] > 0 && todayRevenue >= tops[i]) {
                result = logUacConversionEvent(topValues[i], todayRevenue, totalRevenue);
                if (result) {
                    return;
                }
            }
        }
    }
}
