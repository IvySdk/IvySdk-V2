package com.ivy.base.grid;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.ivy.base.ILog;

import org.json.JSONObject;

public class GridManager implements IGridQuery {

    public static final String TAG = "GridManager";

    private static GridManager instance;

    private MutableLiveData<JSONObject> liveGrid = new MutableLiveData<>(null);

    public interface AdProvidersCallback {
        void setupAdProviders(String str, boolean z);
    }

    private GridManager() {
    }

    public JSONObject getGridData(){
        return liveGrid.getValue();
    }

    public static GridManager getInstance() {
        if (instance == null) {
            synchronized (GridManager.class) {
                if (instance == null) {
                    instance = new GridManager();
                }
            }
        }
        return instance;
    }

    public abstract static class IGrid {
        // 统计
        public void onTrackConfig(String data) {}

        // 广告
        public void onAdConfig(String data) {}

        // 客服
        public void onCustomerServiceConfig(String data) {}

        // 推送
        public void onPushConfig(String data) {}

        // 计费
        public void onPayment(String data, String enjoy_pay_data) {}


        // 自有 remote config
        public void onGridRemoteConfig(String data) {}

        // 游戏服务
        public void onGameServices(String data) {}

        // 其它，自取
        public void onData() {}
    }

    public void setData(JSONObject json) {
        ILog.i(TAG, "setup grid data:\n" + json);
        liveGrid.setValue(json);
    }

    public void addListener(Observer<JSONObject> observer) {
        liveGrid.observeForever(observer);
    }

    public void removeListener(Observer<JSONObject> observer) {
        liveGrid.removeObserver(observer);
    }

    public void parseConfig(IGrid callback) {
        JSONObject gridData = liveGrid.getValue();
        if (callback != null) {
            // 统计
            callback.onTrackConfig(gridData != null ? gridData.optString("track") : null);
            // 自定义远程配置
            callback.onGridRemoteConfig(gridData != null ? gridData.optString("remoteConfig") : null);
            // 客服
            callback.onCustomerServiceConfig(gridData != null ? gridData.optString("helper") : null);
            // 推送
            callback.onPushConfig(gridData != null ? gridData.optString("push") : null);
            // 广告
            callback.onAdConfig(gridData != null ? gridData.optString("oldads") : null);
            // 计费
            String google_billing_data = gridData != null ? gridData.optString("payment") : null;
            String enjoy_pay_data = gridData != null ? gridData.optString("enjoy_pay") : null;
            callback.onPayment(google_billing_data, enjoy_pay_data);

            // 基础服务
            callback.onGameServices(gridData != null ? gridData.optString("gameServices") : null);
            // 其它
            callback.onData();
        }
    }

    /**
     * 获取第一层数据值
     */
    @Override
    public String getGridConfig(String key, String defaultValue) {
        JSONObject json = liveGrid.getValue();
        return json != null ? json.optString(key, defaultValue) : defaultValue;
    }

    @Override
    public int getGridConfig(String key, int defaultValue) {
        JSONObject json = liveGrid.getValue();
        return json != null ? json.optInt(key, defaultValue) : defaultValue;
    }

    @Override
    public double getGridConfig(String key, double defaultValue) {
        JSONObject json = liveGrid.getValue();
        return json != null ? json.optDouble(key, defaultValue) : defaultValue;
    }

    @Override
    public boolean getGridConfig(String key, boolean defaultValue) {
        JSONObject json = liveGrid.getValue();
        return json != null ? json.optBoolean(key, defaultValue) : defaultValue;
    }

    @Override
    public long getGridConfig(String key, long defaultValue) {
        JSONObject json = liveGrid.getValue();
        return json != null ? json.optLong(key, defaultValue) : defaultValue;
    }
}
