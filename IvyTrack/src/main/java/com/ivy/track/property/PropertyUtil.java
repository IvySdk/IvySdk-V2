package com.ivy.track.property;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.text.TextUtils;

import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.Version;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.track.EventParams;
import com.ivy.base.util.AdvertisingIdManager;
import com.ivy.base.util.Util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class PropertyUtil {
    private static volatile PropertyUtil instance;
    private final FPSUtil fpsUtil = new FPSUtil();
    private final NetworkUtil networkUtil = new NetworkUtil();
    private final Map<String, Object> statics = loadStaticProperties();

    private PropertyUtil() {
        fpsUtil.startListenFPS();
        networkUtil.startListenNetworkState(App.getInstance());
    }

    public static PropertyUtil getInstance() {
        if (instance == null) {
            synchronized (PropertyUtil.class) {
                if (instance == null) {
                    instance = new PropertyUtil();
                }
            }
        }
        return instance;
    }

    public Map<String, Object> loadProperties() {
        Map<String, Object> bundle = new HashMap<>();
        bundle.putAll(statics);
        Map<String, Object> dynamic = loadDynamicProperties();
        bundle.putAll(dynamic);
        return bundle;
    }

    private Map<String, Object> loadStaticProperties() {
        Map<String, Object> bundle = new HashMap<>();
        bundle.put(Properties.KEY_ANDROID_API_VERSION, Build.VERSION.SDK_INT);
        bundle.put(EventParams.EVENT_PARAM_ROLE_ID, Util.roleId());
        bundle.put(Properties.KEY_LIB_VERSION, Version.SDK_VERSION_CODE);

        String androidUuid = LocalStorage.Instance.decodeString("_Android_uuid_", null);
        if (androidUuid == null || androidUuid.isEmpty()) {
            androidUuid = UUID.randomUUID().toString();
            LocalStorage.Instance.encodeString("_Android_uuid_", androidUuid);
        }
        bundle.put(Properties.KEY_ANDROID_UUID, androidUuid);

        String osVersion = DeviceUtil.getHarmonyOSVersion();
        if (osVersion == null || osVersion.isEmpty()) {
            bundle.put(Properties.KEY_ANDROID_VERSION, Build.VERSION.RELEASE);
            bundle.put(Properties.KEY_OS_VERSION, AndroidVersionUtils.getAndroidMajorVersion());
            bundle.put(Properties.KEY_OS, "android");
        } else {
            bundle.put(Properties.KEY_ANDROID_VERSION, osVersion);
            bundle.put(Properties.KEY_OS_VERSION, osVersion);
            bundle.put(Properties.KEY_OS, "harmonyos");
        }

        try {
            PackageInfo packageInfo = App.getInstance().getPackageManager().getPackageInfo(App.getInstance().getPackageName(), 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bundle.put(Properties.KEY_APP_VERSION_CODE, packageInfo.getLongVersionCode());
            } else {
                bundle.put(Properties.KEY_APP_VERSION_CODE, packageInfo.versionCode);
            }

            bundle.put(Properties.KEY_APP_VERSION, Util.versionName());
            bundle.put(Properties.KEY_DEVICE, Build.DEVICE);
            bundle.put(Properties.KEY_MODEL, Build.MODEL);

            String carrier = DeviceUtil.getCarrier(ActivityUtil.getInstance().getActivity());
            bundle.put(Properties.KEY_CARRIER, carrier != null ? carrier : "");

            bundle.put(Properties.KEY_COUNTRY, Locale.getDefault().getDisplayCountry(Locale.ENGLISH));
            bundle.put(Properties.KEY_COUNTRY_CODE, Util.countryCode());
            bundle.put(Properties.KEY_SDK_COUNTRY_CODE, Util.countryCode());
            bundle.put(Properties.KEY_SIM_COUNTRY_CODE, Util.simCountryCode());

            String androidId = DeviceUtil.getAndroidId(ActivityUtil.getInstance().getActivity());
            bundle.put(Properties.KEY_DEVICE_ID, androidId != null ? androidId : "");

            bundle.put(Properties.KEY_DEVICE_MODEL, Build.BRAND + "::" + Build.MODEL);
            bundle.put(Properties.KEY_SDK_DEVICE_MODEL, Build.BRAND + "::" + Build.MODEL);

            TimeUtil installTimeUtil = new TimeUtil(packageInfo.firstInstallTime);
            bundle.put(Properties.KEY_INSTALL_TIME, installTimeUtil.getUtcTimeStr());
            bundle.put(Properties.KEY_INSTALL_TIME_IT, packageInfo.firstInstallTime);

            TimeUtil updateTimeUtil = new TimeUtil(packageInfo.lastUpdateTime);
            bundle.put(Properties.KEY_UPDATED_TIME, updateTimeUtil.getUtcTimeStr());
            bundle.put(Properties.KEY_SYSTEM_LANGUAGE, Locale.getDefault().getLanguage());
            bundle.put(Properties.KEY_UPDATED_TIME_UT, packageInfo.lastUpdateTime);

            bundle.put(Properties.KEY_DISK, Util.byte2GB(Util.totalDiskSize()));
            bundle.put(Properties.KEY_RAM, Util.byte2GB(Util.totalMemory()));
            bundle.put(Properties.KEY_BUNDLE_ID, packageInfo.packageName);

            bundle.put(Properties.KEY_SIMULATOR, new EmulatorDetector().isEmulator() ? "1" : "0");

            bundle.put(Properties.KEY_ZONE_OFFSET, getTimezoneOffset(System.currentTimeMillis(), TimeZone.getDefault()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bundle;
    }

    private Map<String, Object> loadDynamicProperties() {
        Map<String, Object> bundle = new HashMap<>();
        try {
            String gaId = AdvertisingIdManager.getAdId();
            if (gaId != null) {
                bundle.put(EventParams.EVENT_PARAM_GA_ID, gaId);
            }
        } catch (Exception ignored) {
        }
        String afid = LocalStorage.Instance.decodeString("appsflyer_id", null);
        if (afid != null) {
            bundle.put("appsflyer_id", afid);
        }

        bundle.put(Properties.KEY_TIME, com.ivy.base.time.TimeUtil.getInstance().getFormattedUTCTime("yyyy-MM-dd"));
        bundle.put(Properties.KEY_TIME_LOCAL, com.ivy.base.time.TimeUtil.getInstance().getFormattedLocalTime("yyyy-MM-dd"));
        bundle.put(Properties.KEY_TIME_ST, com.ivy.base.time.TimeUtil.getInstance().getTimestamp());
        bundle.put(Properties.KEY_TIME_EVENT_TIME, com.ivy.base.time.TimeUtil.getInstance().getFormattedUTCTime("yyyy-MM-dd HH:mm:ss.SSS"));

        bundle.put(Properties.KEY_AVAILABLE_DISK, Util.byte2GB(Util.availableDiskSize()));
        bundle.put(Properties.KEY_AVAILABLE_RAM, Util.byte2GB(Util.availableMemory()));
        bundle.put(Properties.KEY_FPS, fpsUtil.getFPS());
        bundle.put(Properties.KEY_NETWORK_TYPE, getNetworkType());
        try {
            String fb_push_token = LocalStorage.Instance.decodeString("fb_push_token", "");
            if (!TextUtils.isEmpty(fb_push_token)) {
                bundle.put(Properties.KEY_FB_PUSH_TOKEN, fb_push_token);
            }
        } catch (Exception e) {

        }

        return bundle;
    }

    private double getTimezoneOffset(long time, TimeZone timeZone) {
        TimeZone tz = timeZone != null ? timeZone : TimeZone.getDefault();
        return tz.getOffset(time) / (1000.0 * 60 * 60);
    }

    public String getNetworkType() {
        return ActivityUtil.getInstance().getActivity() != null ? networkUtil.getNetworkType(ActivityUtil.getInstance().getActivity()) : "NULL";
    }


}