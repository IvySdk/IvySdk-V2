package com.ivy.remote;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;
import com.ivy.base.track.IKeys;
import com.ivy.base.util.BrotliDecompressor;
import com.ivy.base.util.CheckConfigHelper;
import com.ivy.base.util.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataManager {

    public static final String KEY_SERVER_URL = "https://gameconfig.ivymobile.com/api/external/v1/distribution/remote_config/edata";
    public static final String KEY_SERVER_URL_DEBUG = "http://43.130.59.186:8888/api/external/v1/distribution/remote_config/edata";
    public static final String KEY_AB_SERVER_URL = "https://gameconfig.ivymobile.com/api/external/v1/distribution/ab_config/edata";
    public static final String KEY_AB_SERVER_URL_DEBUG = "http://43.130.59.186:8888/api/external/v1/distribution/ab_config/edata";
    public static final String KEY_GET_USER_INFO_URL = "https://gameconfig.ivymobile.com/api/external/v1/distribution/user/info";
//    public static final long DEFAULT_TIME_WAITING_REMOTE_CONFIG = 10 * 1000L;
    public static final boolean ENABLE_BROTLI = false;
    public static final String KEY_LAST_REFRESH_TIME = "_rc_last_refresh_time_";
    public static final String KEY_CACHE_USER_TAGS = "sdk_cache_user_tags";

    private final AppConfigManager appConfig = new AppConfigManager();
    private String currentVAPI = "";

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void clearCachedAppConfig() {
        appConfig.clearCachedData();
    }

    public void loadLocalConfig(final IRCManager.AppConfigCallback callback) {
        appConfig.loadLocalConfig(callback);
    }

    public void loadRemoteData(final String appId, final boolean debug, final JSONObject localConfig, final Map<String, Object> headerInfo, final IRCManager.RemoteConfigCallback remoteConfigCallback) {
        final String vApi = localConfig.optString("v_api");
        currentVAPI = vApi;

//        long timeWaitingForRemoteData = localConfig.optLong("timeWaitingForRemoteData", DEFAULT_TIME_WAITING_REMOTE_CONFIG);
//        if (timeWaitingForRemoteData <= 0)
//            timeWaitingForRemoteData = DEFAULT_TIME_WAITING_REMOTE_CONFIG;
        ILog.i(IvyRemoteConfig.TAG, "start load remote data");
        executor.submit(() -> {
            try {
                JSONObject jsonObject = doLoadRemoteData(appId, vApi, debug, headerInfo);
                if (jsonObject == null) {
                    mainHandler.post(() -> TestManager.getInstance().addEvent("IVY", "RemoteConfig拉取失败", EventState.WARNING, "返回空内容"));
                    throw new Exception("no remote data loaded, use local config");
                }
                // 解析 default_json
                if (jsonObject.has("default_json")) {
                    JSONObject defaultJson;
                    try {
                        defaultJson = jsonObject.getJSONObject("default_json");
                        ILog.i(IvyRemoteConfig.TAG, "loaded default json :" + defaultJson);
                    } catch (Exception e) {
                        ILog.e(IvyRemoteConfig.TAG, "get default json err:" + e.getMessage());
                        defaultJson = localConfig;
                    }
                    appConfig.cacheConfig(defaultJson.toString());
                }

                // 解析 _IVYSDK_CONFIG
                if (jsonObject.has("_IVYSDK_CONFIG")) {
                    try {
                        JSONObject configJson = jsonObject.getJSONObject("_IVYSDK_CONFIG");
                        ILog.i(IvyRemoteConfig.TAG, "_IVYSDK_CONFIG:" + configJson);

                        try {
                            String country = configJson.getString("country");
                            LocalStorage.Instance.encodeString("cache_country_code", country);
                        } catch (Exception ignored) {
                        }

                        try {
                            JSONObject adJson = new JSONObject();
                            String[] adKeys = {"media_source", "campaign_id", "campaign_name", "adset_name", "adset_id", "media_channel", "siteid", "ad_type", "ad_name", "ad_id"};
                            for (String key : adKeys) {
                                String value = Util.loadStrFromJSONObject(configJson, key);
                                if (value != null) adJson.put(key, value);
                            }
                            LocalStorage.Instance.encodeString("cache_install_data", adJson.toString());
                        } catch (Exception ignored) {
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (jsonObject.length() > 0) {
                    LocalStorage.Instance.encodeLong(KEY_LAST_REFRESH_TIME, System.currentTimeMillis());
                    try {
                        jsonObject.remove("default_json");
                    } catch (Exception ignored) {
                    }
                    mainHandler.post(() -> {
                        remoteConfigCallback.onResult(true, jsonObject);
                        TestManager.getInstance().addEvent("IVY", "RemoteConfig拉取成功", EventState.NORMAL, jsonObject.toString());
                    });

                } else {
                    mainHandler.post(() -> {
                        remoteConfigCallback.onResult(false, localConfig);
                        TestManager.getInstance().addEvent("IVY", "RemoteConfig拉取失败", EventState.WARNING, "返回空内容");
                    });
                }
            } catch (Exception e) {
                ILog.e(IvyRemoteConfig.TAG, "load remote data failed, use local config");
                mainHandler.post(() -> {
                    remoteConfigCallback.onResult(false, localConfig);
                    TestManager.getInstance().addEvent("IVY", "RemoteConfig拉取失败", EventState.WARNING, e.getMessage());
                });
            }
        });
    }

    public void loadAbData(final String appId, final boolean debug, final Map<String, Object> headerInfo, final IRCManager.AbDataCallback callback) {
        JSONObject body = formatPostBody(appId, debug, currentVAPI, headerInfo);
        executor.submit(() -> {
            String url = debug ? KEY_AB_SERVER_URL_DEBUG : KEY_AB_SERVER_URL;
            String resp = RemoteDataTask.post(url, body);
            if (resp != null) {
                ILog.i(IvyRemoteConfig.TAG, "ab server data:");
                try {
                    String srcData = new JSONObject(resp).getString("data");
                    String decryptData = EncryptUtil.decrypt(srcData);
                    String decompressData = BrotliDecompressor.decompress(ENABLE_BROTLI, decryptData);
                    ILog.i(IvyRemoteConfig.TAG, decompressData);
                    JSONObject resultJson = null;
                    if (decompressData != null) {
                        Pair<Boolean, JSONObject> result = EncryptUtil.isValidJson(decompressData);
                        if (result.first && result.second != null) resultJson = result.second;
                    }
                    JSONObject finalResultJson = resultJson;
                    mainHandler.post(() -> callback.onResult(finalResultJson));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onResult(null));
                }
            } else {
                mainHandler.post(() -> callback.onResult(null));
            }
        });
    }

    private JSONObject doLoadRemoteData(String appId, String vApi, boolean debug, Map<String, Object> headerInfo) {
        JSONObject body = formatPostBody(appId, debug, vApi, headerInfo);
        String resp = RemoteDataTask.post(debug ? KEY_SERVER_URL_DEBUG : KEY_SERVER_URL, body);
        if (resp != null) {
            try {
                String srcData = new JSONObject(resp).getString("data");
                String decryptData = EncryptUtil.decrypt(srcData);
                String decompressData = BrotliDecompressor.decompress(ENABLE_BROTLI, decryptData);
                Pair<Boolean, JSONObject> result = EncryptUtil.isValidJson(decompressData);
                if (result.first && result.second != null) return result.second;
            } catch (Exception e) {
                ILog.e(IvyRemoteConfig.TAG, "doLoadRemoteData error:" + e.getMessage());
            }
        }
        return null;
    }

    public void loadUserTags(final String appId, final IRCManager.UserInfoCallback callback) {
        executor.submit(() -> {
            JSONObject data = doLoadUserTags(appId);
            if (data != null) {
                ILog.d("REMOTE", data.toString());
                LocalStorage.Instance.encodeString(KEY_CACHE_USER_TAGS, data.toString());
                callback.onResult(data.optString("country_code"));
            } else {
                callback.onResult(null);
            }
        });
    }

    private JSONObject doLoadUserTags(String appId){
        String url = KEY_GET_USER_INFO_URL + "?app_id=" + appId + "&user_id=" + Util.userId();
        try {
            String data = RemoteDataTask.loadData(url);
            if (data != null) {
                JSONObject js = new JSONObject(data);
                if (js.optInt("code", -1) == 0){
                    return js.getJSONObject("data");
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String doLoadUserInfo(String appId) {
        String url = KEY_GET_USER_INFO_URL + "?app_id=" + appId + "&user_id=" + Util.roleId() + "&user_property=country_code";
        String data = RemoteDataTask.loadData(url);
        if (data != null) {
            try {
                JSONObject js = new JSONObject(data);
                if (js.optInt("code", -1) == 0)
                    return js.getJSONObject("data").getString("country_code");
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private JSONObject formatPostBody(String appId, boolean debug, String vApi, Map<String, Object> headerInfo) {
        JSONObject json = new JSONObject();
        try {
            json.put("app_id", appId);
            json.put("data_version", vApi);
            json.put("user_id", Util.roleId());

            String cuid = LocalStorage.Instance.decodeString(IKeys.KEY_CUSTOM_USER_ID, null);
            if (cuid != null) json.put("cuid", cuid);

            json.put("brotli", ENABLE_BROTLI ? 1 : 0);
            json.put("brotli_debug", 0);
            json.put("app_version", Util.versionCode());
            json.put("os_version", Build.VERSION.SDK_INT);
            json.put("country", Util.countryCode());
            json.put("os_ram", Util.byte2GB(Util.totalMemory()));
            json.put("dname", Build.DEVICE);
            json.put("modelname", Build.MODEL);
            json.put("language", Locale.getDefault().getLanguage());
            json.put("network", CheckConfigHelper.getNetworkType(App.getInstance()));
            json.put("timezone", TimeZone.getDefault().getDisplayName(false, TimeZone.LONG));
            json.put("timestamp", String.valueOf(System.currentTimeMillis()));

            String afid = LocalStorage.Instance.decodeString(IKeys.KEY_AF_ID, "");
            json.put("afid", afid);
            json.put("media_source", LocalStorage.Instance.decodeString(IKeys.KEY_AF_MEDIA_SOURCE, ""));
            json.put("campaign", LocalStorage.Instance.decodeString(IKeys.KEY_AF_CAMPAIGN, ""));
            json.put("campaign_id", LocalStorage.Instance.decodeString(IKeys.KEY_AF_CAMPAIGN_ID, ""));
            json.put("is_new_user", LocalStorage.Instance.decodeBoolean(IKeys.KEY_APP_NEW_USER, false) ? "1" : "0");
            json.put("bundleId", Util.packageName());
            json.put("opt", CheckConfigHelper.checkConfigB(App.getInstance()));

            String installDataStr = LocalStorage.Instance.decodeString("cache_install_data", null);
            if (installDataStr != null) {
                JSONObject installData = new JSONObject(installDataStr);
                JSONArray keys = installData.names();
                if (keys != null) {
                    for (int i = 0; i < keys.length(); i++) {
                        String key = keys.getString(i);
                        json.put(key, installData.opt(key));
                    }
                }
            }

            if (headerInfo != null) {
                for (Map.Entry<String, Object> entry : headerInfo.entrySet()) {
                    json.put(entry.getKey(), entry.getValue());
                }
            }

            if (debug) {
                String keyStoreHash = Util.getKeyStoreHash(App.getInstance());
                if (keyStoreHash != null) json.put("security_key", keyStoreHash);
                //   json.put("test_uuid", Util.roleId());
            }

        } catch (Exception e) {
            ILog.e(IvyRemoteConfig.TAG, "formatPostBody error:" + e.getMessage());
        }
        return json;
    }


}
