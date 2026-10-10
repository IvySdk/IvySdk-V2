package com.ivy.core.Uac;

import com.ivy.base.ILog;
import com.ivy.base.http.HttpUtil;
import com.ivy.base.local.LocalStorage;
import com.ivy.core.util.Helper;

import org.json.JSONObject;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class UAC {

    private final String appId;
    private final String apiUrl;

    private AbsUACTask oneDayTask;
    private AbsUACTask threeDayTask;
    private AbsUACTask sevenDayTask;

    private static final String TAG = "UAC";
    private static final String KEY_UAC_UPDATE_TIME = "KEY_UAC_UPDATE_TIME";

    public UAC(String appId, String apiUrl) {
        this.appId = appId;
        this.apiUrl = apiUrl;
    }

    public void checkAndUpdateUacTop() {
        long lastCheckTs = LocalStorage.Instance.decodeLong(KEY_UAC_UPDATE_TIME, 0L);
        if (System.currentTimeMillis() - lastCheckTs < 2 * 3600 * 1000L) {
            return;
        }
        try {
            String pkgName = Helper.getConfig(Helper.CONFIG_KEY_PACKAGE_NAME);
            Request request = new Request.Builder()
                    .url(apiUrl + "?appId=" + appId + "&packageName=" + pkgName)
                    .get()
                    .build();

            HttpUtil.getInstance().getOkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    ILog.i(TAG, "checkAndUpdateUacTop err:" + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        if (response.body() == null) {
                            throw new IllegalArgumentException("invalid response data");
                        }
                        String data = response.body().string();
                        JSONObject result = new JSONObject(data);
                        String status = result.getString("status");
                        if (!"success".equals(status)) {
                            ILog.i(TAG, "checkAndUpdateUacTop result status not success");
                            return;
                        }

                        JSONObject obj1 = result.optJSONObject("data");
                        if (obj1 != null) {
                            oneDayTask = new UACTaskOneDay();
                            oneDayTask.formatSrcData(obj1);
                            LocalStorage.Instance.encodeLong(KEY_UAC_UPDATE_TIME, System.currentTimeMillis());
                        }

                        JSONObject obj3 = result.optJSONObject("day3_data");
                        if (obj3 != null) {
                            threeDayTask = new UACTaskThreeDays();
                            threeDayTask.formatSrcData(obj3);
                        }

                        JSONObject obj7 = result.optJSONObject("day7_data");
                        if (obj7 != null) {
                            sevenDayTask = new UACTaskSevenDays();
                            sevenDayTask.formatSrcData(obj7);
                        }

                    } catch (Exception e) {
                        ILog.i(TAG, "checkAndUpdateUacTop result parse err:" + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            ILog.e("okhttp", "check uac config failed:" + e.getMessage());
        }
    }

    public void checkUacLtvConversion(float revenue, float totalRevenue) {
        if (oneDayTask != null) {
            oneDayTask.checkUacLtvConversion(revenue, totalRevenue);
        }
        if (threeDayTask != null) {
            threeDayTask.checkUacLtvConversion(revenue, totalRevenue);
        }
        if (sevenDayTask != null) {
            sevenDayTask.checkUacLtvConversion(revenue, totalRevenue);
        }
    }
}
