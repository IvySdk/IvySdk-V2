package com.ivy.core.ab;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.ab.IAbManager;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;
import com.ivy.base.track.IHuoShan;
import com.ivy.base.util.Util;
import com.ivy.core.IvySdk;
import com.ivy.remote.IRCManager;
import com.ivy.remote.IvyRemoteConfig;
import com.ivy.track.IvyTrack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class IvyABManager extends IAbManager {

    public static final String TAG = "IvyAB";
    public static final String CACHED_AB_DATA_FILE = "ab.dt";

    private static IvyABManager instance;

    public static synchronized IvyABManager getInstance() {
        if (instance == null) {
            instance = new IvyABManager();
        }
        return instance;
    }

    private int abChannel = 0; // ab 渠道 0： IVY ； 1 ：火山(已移除)
    private JSONObject originIvyAbData = null; // 原始云端配置副本
    private JSONObject ivyAbData = null; // ivy ab 数据
    private JSONObject experimentAbData = null;
    private JSONObject defaultExperimentAbData = null;
    private Map<String, Object> abHeaderInfo = new ConcurrentHashMap<>();


    private AtomicBoolean hasInstallDataSet = new AtomicBoolean(false);
    private AtomicBoolean hasAlreadyData = new AtomicBoolean(false);

    private AdDataCallback adDataCallback = null;
    private long lastRefreshSuccessTime = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private IvyABManager() {
        loadCachedAbData();
    }

    private String abHitResults = "";

    public void setDefaultExperimentAbData(JSONObject data, JSONObject experimentAbData, String hitResults) {
        defaultExperimentAbData = data;
        this.experimentAbData = experimentAbData;
        this.abHitResults = hitResults;
        mergeAbData();
    }

    public String getAbHitResults() {
        return abHitResults;
    }

    private void mergeAbData() {
        try {
            JSONObject o;
            if (originIvyAbData != null) {
                o = new JSONObject(originIvyAbData.toString());
            } else {
                o = new JSONObject();
            }

            if (experimentAbData != null) {
                Iterator<String> it = experimentAbData.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    Object value = experimentAbData.opt(key);
                    if (value != null) {
                        o.put(key, value);
                    }
                }
            }
            ivyAbData = o;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadCachedAbData() {
        try {
            String file_path = App.getInstance().getCacheDir().getAbsolutePath() + File.separator + CACHED_AB_DATA_FILE;
            Util.loadCachedFileData(file_path, data -> {
                try {
                    if (data != null) {
                        originIvyAbData = new JSONObject(data);
                        mergeAbData();
                    } else {
                        ILog.i(TAG, "cached ab data does not exist");
                    }
                } catch (Exception e) {
                    ILog.i(TAG, "load cached ab data failed:" + e.getMessage());
                }
            });
        } catch (Exception e) {
            ILog.i(TAG, "load cached ab data failed:" + e.getMessage());
        }
    }

    private void cacheAbData() {
        try {
            if (originIvyAbData != null) {
                Util.cacheDataToFile(CACHED_AB_DATA_FILE, originIvyAbData.toString());
            } else {
                ILog.i(TAG, "cache ab data failed");
            }
        } catch (Exception e) {
            ILog.i(TAG, "cache ab data failed:" + e.getMessage());
        }
    }

    public void setAdDataListener(AdDataCallback callback) {
        this.adDataCallback = callback;
    }

    public int getAbChannel() {
        return abChannel;
    }

    public void enableAbTest(int channel, boolean debug) {
        abChannel = channel;
        ILog.i(TAG, "ab channel:" + channel);
    }

    public Map<String, Object> getHeaderInfo() {
        return abHeaderInfo;
    }

    @Override
    public void setHeaderInfo(String key, Object value) {
        abHeaderInfo.put(key, value);

    }

    @Override
    public void setUserInfo(String key, Object value) {

    }

    private void setInstallData(JSONObject json) {
        try {
            if (abChannel == 0) {
                Iterator<String> it = json.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    Object value = json.opt(key);
                    if (value != null) {
                        abHeaderInfo.put(key, value);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        hasInstallDataSet.set(true);
    }

    public boolean updateInstallData() {
        String cachedData = LocalStorage.Instance.decodeString("cache_install_data", null);
        if (cachedData != null) {
            try {
                JSONObject json = new JSONObject(cachedData);
                if (json.length() > 0) {
                    IvyABManager.getInstance().setHeaderInfo("country", Util.countryCode());
                    IvyABManager.getInstance().setInstallData(json);
                    return true;
                } else {
                    ILog.i(IvySdk.TAG, "cached install data is empty");
                    LocalStorage.Instance.removeValueForKey("cache_install_data");
                    return false;
                }
            } catch (Exception e) {
                ILog.i(IvySdk.TAG, "format cached install data failed:" + cachedData);
                return false;
            }
        }
        return false;
    }

    @Override
    public String getAbtestSSID() {
        String ssid = LocalStorage.Instance.decodeString("appsflyer_id", "");
        if (ssid.isEmpty()) {
            return Util.roleId();
        }
        return ssid;
    }

    // Load AB Test Configs
    public void loadAbTestConfigs(final String appId, final boolean debug, final LoadAbCallback callback) {
        boolean adDataReady = hasInstallDataSet.get();
        if (!adDataReady) {
            loadAdData(appId, debug);
            adDataReady = false;
        }
        loadABData(appId, debug, adDataReady, callback);
    }

    private void loadAdData(final String appId, final boolean debug) {
        if (hasAlreadyData.get())
            return;
        hasAlreadyData.set(true);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                int count = 1;
                while (count < 4) {
                    try {
                        Thread.sleep(count * 5000L);
                    } catch (InterruptedException e) {
                    }

                    boolean updated = updateInstallData();
                    ILog.i(TAG, "loop for waiting install data:" + updated + "; " + count);
                    if (updated || count == 3) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                loadABData(appId, debug, true, null);
                            }
                        });
                        count = 4;
                    } else {
                        count++;
                    }
                }
            }
        });
    }

    private void loadABData(final String appId, final boolean debug, final boolean loadAd,
                            final LoadAbCallback callback) {
        if (abChannel == 0) {
            if (originIvyAbData != null && SystemClock.elapsedRealtime() - lastRefreshSuccessTime < 10 * 60 * 1000) {
                if (callback != null)
                    callback.onLoaded(true);
                if (loadAd)
                    checkAdConfig();
                return;
            }

            IvyRemoteConfig.getInstance().loadAbData(appId, debug, abHeaderInfo, new IRCManager.AbDataCallback() {
                @Override
                public void onResult(JSONObject json) {
                    ILog.i(TAG, "ivy ab data loaded:" + json);
                    originIvyAbData = json;
                    if (callback != null)
                        callback.onLoaded(json != null);
                    if (loadAd) {
                        ILog.i(TAG, "load ad data from ivy ab");
                        checkAdConfig();
                    }
                    cacheAbData();
                    mergeAbData();
                }
            });

        } else {
            ILog.e(TAG, "unsupported ab channel:" + abChannel);
            if (callback != null)
                callback.onLoaded(false);
            if (loadAd)
                checkAdConfig();
        }
    }

    private void checkAdConfig() {
        // String adConfigStr;
        // switch (abChannel) {
        // case 0:
        // adConfigStr = huoShanAb != null ? huoShanAb.getAbConfigString("ads", "") :
        // "";
        // break;
        // case 1:
        // adConfigStr = ivyAbData != null ? ivyAbData.optString("ads") : "";
        // break;
        // default:
        // adConfigStr = GridManager.getInstance().getGridConfig("ads", "");
        // break;
        // }
        //
        // Pair<Boolean, JSONObject> pair = EncryptUtil.isValidJson(adConfigStr);
        // if (pair.first && pair.second != null) {
        // if (adDataCallback != null) {
        // adDataCallback.onAdData(pair.second);
        // }
        // } else {
        // new Thread(() -> {
        // Pair<Boolean, JSONObject> localPair = new Pair<>(false, null);
        // while (!localPair.first) {
        // String str = GridManager.getInstance().getGridConfig("ads", "");
        // if (str == null) str = "";
        // localPair = EncryptUtil.isValidJson(str);
        // if (localPair.second == null) {
        // localPair = new Pair<>(false, null);
        // try {
        // Thread.sleep(200);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // break;
        // }
        // }
        // }
        //
        // // 切换到主线程执行回调
        // Pair<Boolean, JSONObject> finalLocalPair = localPair;
        // new android.os.Handler(Looper.getMainLooper()).post(() -> {
        // if (adDataCallback != null) {
        // adDataCallback.onAdData(finalLocalPair.second);
        // }
        // });
        // }).start();
        // }
    }

    @Override
    public String getAllAbConfigs() {
        if (abChannel == 0) {
            return ivyAbData != null ? ivyAbData.toString() : "{}";
        } else {
            return "{}";
        }
    }

    @Override
    public String getAbConfigString(String key, String defaultValue) {
        if (key == null || key.isEmpty())
            return defaultValue;

        if (ivyAbData != null) {
            try {
                JSONObject json = ivyAbData.optJSONObject(key);
                String value = json.getString("val");
                String vid = json.getString("vid");
                IvyTrack.getInstance().logEvent("sdk_ab_exposure", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK,
                        Map.of("vid", vid));
                ILog.i(IvySdk.TAG, "getAbConfigString:key:" + key + "->" + value);
                return value;
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
        if (defaultExperimentAbData != null) {
            return defaultExperimentAbData.optString(key, defaultValue);
        }
        return defaultValue;
    }

    @Override
    public int getAbConfigInt(String key, int defaultValue) {
        if (key == null || key.isEmpty())
            return defaultValue;

        if (ivyAbData != null) {
            try {
                JSONObject json = ivyAbData.optJSONObject(key);
                int value = json.getInt("val");
                String vid = json.getString("vid");
                IvyTrack.getInstance().logEvent("sdk_ab_exposure", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK,
                        Map.of("vid", vid));
                ILog.i(IvySdk.TAG, "getAbConfigInt:key:" + key + "->" + value);
                return value;
            } catch (Exception ignored) {
            }
        }
        if (defaultExperimentAbData != null) {
            return defaultExperimentAbData.optInt(key, defaultValue);
        }
        return defaultValue;
    }

    @Override
    public long getAbConfigLong(String key, long defaultValue) {
        if (key == null || key.isEmpty())
            return defaultValue;
        if (ivyAbData != null) {
            try {
                JSONObject json = ivyAbData.optJSONObject(key);
                long value = json.getLong("val");
                String vid = json.getString("vid");
                IvyTrack.getInstance().logEvent("sdk_ab_exposure", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK,
                        Map.of("vid", vid));
                ILog.i(IvySdk.TAG, "getAbConfigLong:key:" + key + "->" + value);
                return value;
            } catch (Exception ignored) {
            }
        }
        if (defaultExperimentAbData != null) {
            return defaultExperimentAbData.optLong(key, defaultValue);
        }
        return defaultValue;
    }

    @Override
    public double getAbConfigDouble(String key, double defaultValue) {
        if (key == null || key.isEmpty())
            return defaultValue;
        if (ivyAbData != null) {
            try {
                JSONObject json = ivyAbData.optJSONObject(key);
                double value = json.getDouble("val");
                String vid = json.getString("vid");
                IvyTrack.getInstance().logEvent("sdk_ab_exposure", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK,
                        Map.of("vid", vid));
                ILog.i(IvySdk.TAG, "getAbConfigDouble:key:" + key + "->" + value);
                return value;
            } catch (Exception ignored) {
            }
        }
        if (defaultExperimentAbData != null) {
            return defaultExperimentAbData.optDouble(key, defaultValue);
        }
        return defaultValue;
    }

    @Override
    public boolean getAbConfigBoolean(String key, boolean defaultValue) {
        if (key == null || key.isEmpty())
            return defaultValue;

        if (ivyAbData != null) {
            try {
                JSONObject json = ivyAbData.optJSONObject(key);
                boolean value = json.getBoolean("val");
                String vid = json.getString("vid");
                IvyTrack.getInstance().logEvent("sdk_ab_exposure", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK,
                        Map.of("vid", vid));
                ILog.i(IvySdk.TAG, "getAbConfigBoolean:key:" + key + "->" + value);
                return value;
            } catch (Exception ignored) {
            }
        }

        if (defaultExperimentAbData != null) {
            return defaultExperimentAbData.optBoolean(key, defaultValue);
        }
        return defaultValue;
    }

    @Override
    public float getAbConfigFloat(String key, float defaultValue) {
        if (key == null || key.isEmpty())
            return defaultValue;
//        if (abChannel == 1) {
//            float value = huoShanAb != null ? huoShanAb.getAbConfigFloat(key, defaultValue) : defaultValue;
//            ILog.i(TAG, "getAbConfigFloat:key:" + key + "->" + value);
//            return value;
//        }
        // Kotlin 版本是通过 Double 转 Float
        return (float) getAbConfigDouble(key, defaultValue);
    }

    // Callback interfaces
    public interface LoadAbCallback {
        void onLoaded(boolean success);
    }

    public interface AdDataCallback {
        void onAdData(JSONObject data);
    }
}
