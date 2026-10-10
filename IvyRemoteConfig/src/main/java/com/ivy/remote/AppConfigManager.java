package com.ivy.remote;

import android.os.Build;
import android.util.Pair;

import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;
import com.ivy.base.track.IKeys;
import com.ivy.base.util.BrotliDecompressor;
import com.ivy.base.util.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppConfigManager {

    public static final String TAG = "AppConfig";

    public void loadLocalConfig(final IRCManager.AppConfigCallback callback) {
        try {
            long lastCachedVersionCode = LocalStorage.Instance.decodeLong("gridDataVersion", 0L);
            long currentVersionCode = Util.versionCode();
            String gridData = null;

            if (lastCachedVersionCode == currentVersionCode) {
                ILog.i(TAG, "read cache grid data");
                gridData = loadCacheConfigFile();
            }
//            gridData = null;
            if (gridData != null) {
                Pair<Boolean, JSONObject> pair = EncryptUtil.isValidJson(gridData);
                if (pair.first && pair.second != null) {
                    ILog.i(TAG, "grid data loaded from cache");
                    TestManager.getInstance().addEvent("IVY", "使用历史缓存 default.json", EventState.NORMAL, "");
                    final JSONObject result = pair.second;
                    callback.onResult(result);
                    return;
                }
            }

            String countryConfigFileName = loadCountryConfig();
            if (countryConfigFileName != null) {
                String countryData = loadAssets(countryConfigFileName);
                if (countryData != null) {
                    countryData = EncryptUtil.decrypt(countryData);
                    countryData = BrotliDecompressor.decompress(DataManager.ENABLE_BROTLI, countryData);
                    if (countryData != null && !countryData.isEmpty()) {
                        ILog.i(TAG, "country data loaded");
                        gridData = countryData;
                    } else {
                        ILog.i(TAG, "country data load failed");
                        TestManager.getInstance().addEvent("IVY", "指定国家default.json读取失败", EventState.ERROR, "");
                    }
                } else {
                    ILog.w(TAG, "had not found country config file:" + countryConfigFileName);
                    TestManager.getInstance().addEvent("IVY", "指定国家default.json读取失败", EventState.ERROR, "");
                }
            } else {
                ILog.w(TAG, "unable find country config");
                TestManager.getInstance().addEvent("IVY", "指定国家default.json读取失败", EventState.ERROR, "");
            }

            if (gridData == null) {
                ILog.i(TAG, "no grid data found! load default");
                JSONObject defaultConfig = loadDefaultConfig();
                if (defaultConfig != null) {
                    gridData = defaultConfig.toString();
                }
            }

            if (gridData != null) {
                Pair<Boolean, JSONObject> pair = EncryptUtil.isValidJson(gridData);
                if (pair.first && pair.second != null) {
                    final JSONObject result = pair.second;
                    callback.onResult(result);
                } else {
                    ILog.i(TAG, "invalid grid data");
                    TestManager.getInstance().addEvent("IVY", "default.json 格式错误", EventState.ERROR, "");
                }
            } else {
                ILog.i(TAG, "no grid data found!!!");
                TestManager.getInstance().addEvent("IVY", "default.json 读取失败", EventState.ERROR, "");
                callback.onResult(new JSONObject());
            }

        } catch (Exception e) {
            ILog.e(TAG, "load grid data err:" + e.getMessage());
            ILog.e(TAG, "load default");
            JSONObject defaultConfig = loadDefaultConfig();
            if (defaultConfig != null) {
                final JSONObject result = defaultConfig;
                callback.onResult(result);
            } else {
                callback.onResult(new JSONObject());
            }
        }
    }

    public void clearCachedData() {
        try {
            File file = new File(App.getInstance().getCacheDir(), "grid");
            if (file.exists()) file.delete();
        } catch (Exception e) {
            ILog.w(TAG, "read cached grid failed:" + e.getMessage());
        }
    }

    private String loadCountryConfig() {
        try {
            String fileName = "sdk_cg_" + Util.packageName();
            String encryptFileName = Util.md5(fileName);
            ILog.i(TAG, "cg file name:" + encryptFileName);
            String deData = loadAssets(encryptFileName);
            ILog.i(TAG, "cg file encrypted content:" + deData);
            if (deData != null) {
                deData = EncryptUtil.decrypt(deData);
                deData = BrotliDecompressor.decompress(DataManager.ENABLE_BROTLI, deData);
                ILog.i(TAG, "cg file decrypted content:" + deData);
                if (deData != null) {
                    JSONArray dataArray = new JSONArray(deData);
                    if (dataArray.length() == 0) {
                        ILog.w(TAG, "invalid country config");
                        return null;
                    }

                    long firstOpenTime = LocalStorage.Instance.decodeLong(IKeys.KEY_FIRST_OPEN_TIME, System.currentTimeMillis());
                    long livingDays = (System.currentTimeMillis() - firstOpenTime) / (24 * 60 * 60 * 1000) + 1;
                    long appVersion = Util.versionCode();
                    int osVersion = Build.VERSION.SDK_INT;
                    String country = Util.countryCode().toLowerCase();
                    float mbMemory = Util.byte2GB(Util.totalMemory());
                    String deviceBrand = Build.BRAND.toLowerCase(Locale.ENGLISH);
                    String afCampaignId = LocalStorage.Instance.decodeString(IKeys.KEY_AF_CAMPAIGN_ID, "").toLowerCase();
                    String afMediaSource = LocalStorage.Instance.decodeString(IKeys.KEY_AF_MEDIA_SOURCE, "").toLowerCase();

                    String currentConfigFileName = null;
                    int validCondition = 0;
                    ILog.i(TAG, "app info:\n livingDays:" + livingDays + "\n appVersion:" + appVersion + "\n osVersion:" + osVersion + "\n country:" + country + "\n afCampaignId:" + afCampaignId + "\nafMediaSource:" + afMediaSource);

                    for (int index = 0; index < dataArray.length(); index++) {
                        JSONObject item = dataArray.optJSONObject(index);
                        int currentValidCondition = 0;
                        if (item.has("minAppVersion")) {
                            if (appVersion >= item.optInt("minAppVersion", 0)) {
                                currentValidCondition++;
                            } else {
                                continue;
                            }
                        }
                        if (item.has("minOSVersion")) {
                            if (osVersion >= item.optInt("minOSVersion", 0)) {
                                currentValidCondition++;
                            } else {
                                continue;
                            }
                        }

                        if (item.has("countries")) {
                            JSONArray countriesArray = item.optJSONArray("countries");
                            List<String> countries = new ArrayList<>();
                            if (countriesArray != null) {
                                for (int i = 0; i < countriesArray.length(); i++) {
                                    countries.add(countriesArray.optString(i, "").toLowerCase());
                                }
                            }
                            if (!countries.isEmpty()) {
                                if (countries.contains(country)) {
                                    currentValidCondition++;
                                } else {
                                    continue;
                                }
                            }
                        }

                        if (item.has("OSRams")) {
                            JSONArray osRamArray = item.optJSONArray("OSRams");
                            boolean ramState = true;
                            if (osRamArray != null && osRamArray.length() == 2) {
                                long minRam = osRamArray.optLong(0, 0);
                                long maxRam = osRamArray.optLong(1, 0);
                                ramState = mbMemory >= minRam && mbMemory < maxRam;
                            }
                            if (ramState) {
                                currentValidCondition++;
                            } else {
                                continue;
                            }
                        }

                        if (!afMediaSource.isEmpty() && item.has("mediaSources")) {
                            JSONArray mediaSourcesArray = item.optJSONArray("mediaSources");
                            List<String> mediaSources = new ArrayList<>();
                            if (mediaSourcesArray != null) {
                                for (int i = 0; i < mediaSourcesArray.length(); i++) {
                                    mediaSources.add(mediaSourcesArray.optString(i, ""));
                                }
                            }
                            if (!mediaSources.isEmpty()) {
                                if (mediaSources.contains(afMediaSource)) {
                                    currentValidCondition++;
                                } else {
                                    continue;
                                }
                            }
                        }

                        if (!afCampaignId.isEmpty() && item.has("campaignIds")) {
                            JSONArray campaignIdsArray = item.optJSONArray("campaignIds");
                            List<String> campaignIds = new ArrayList<>();
                            if (campaignIdsArray != null) {
                                for (int i = 0; i < campaignIdsArray.length(); i++) {
                                    campaignIds.add(campaignIdsArray.optString(i, "").toLowerCase());
                                }
                            }
                            if (!campaignIds.isEmpty()) {
                                if (campaignIds.contains(afCampaignId)) {
                                    currentValidCondition++;
                                } else {
                                    continue;
                                }
                            }
                        }

                        if (item.has("livingDays")) {
                            JSONArray livingDaysArray = item.optJSONArray("livingDays");
                            boolean livingDayState = true;
                            if (livingDaysArray != null) {
                                for (int i = 0; i < livingDaysArray.length(); i++) {
                                    long itemValue = livingDaysArray.optLong(i);
                                    livingDayState = itemValue == livingDays;
                                    if (livingDayState) {
                                        break;
                                    }
                                }
                                if (livingDayState) {
                                    currentValidCondition++;
                                }
                            }

                        }

                        String currentFile = item.optString("configFileName", "");
                        if (!currentFile.isEmpty() && currentValidCondition >= validCondition) {
                            currentConfigFileName = currentFile;
                        }
                    }
                    ILog.d(TAG, "found country config file name: " + currentConfigFileName);
                    return currentConfigFileName;
                }
            }
        } catch (Exception e) {
            ILog.w(TAG, "no country config found:" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private String loadAssets(String fileName) {
        try {
            byte[] buffer = new byte[App.getInstance().getAssets().open(fileName).available()];
            App.getInstance().getAssets().open(fileName).read(buffer);
            return new String(buffer);
        } catch (Exception e) {
            ILog.e(TAG, "read assets " + fileName + " failed;" + e.getMessage());
        }
        return null;
    }

    private String loadCacheConfigFile() {
        try {
            File file = new File(App.getInstance().getCacheDir(), "grid");
            byte[] buffer = new byte[(int) file.length()];
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            fis.read(buffer);
            fis.close();
            return new String(buffer);
        } catch (Exception e) {
            ILog.w(TAG, "read cached grid failed:" + e.getMessage());
        }
        return null;
    }

    public void cacheConfig(String data) {
        if (data == null) return;
        try {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(new File(App.getInstance().getCacheDir(), "grid"));
            fos.write(data.getBytes());
            fos.close();
            LocalStorage.Instance.encodeLong("gridDataVersion", Util.versionCode());
        } catch (Exception e) {
            ILog.w(TAG, "cache grid failed:" + e.getMessage());
        }
    }

    private JSONObject loadDefaultConfig() {
        try {
            String data = loadAssets("default.json");
            return new JSONObject(data);
        } catch (Exception e) {
            ILog.w(TAG, "load default config failed:" + e.getMessage());
        }
        return null;
    }


}
