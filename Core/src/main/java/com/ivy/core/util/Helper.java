package com.ivy.core.util;

import android.net.Uri;

import com.ivy.base.ILog;
import com.ivy.base.grid.GridManager;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.track.IKeys;
import com.ivy.base.util.Util;
import com.ivy.core.IvySdk;
import com.ivy.track.property.PropertyUtil;

import java.util.Locale;

public class Helper {

    public static final int CONFIG_KEY_APP_ID = 1;
    public static final int CONFIG_KEY_LEADER_BOARD_URL = 2;
    public static final int CONFIG_KEY_API_VERSION = 3;
    public static final int CONFIG_KEY_SCREEN_WIDTH = 4;
    public static final int CONFIG_KEY_SCREEN_HEIGHT = 5;
    public static final int CONFIG_KEY_LANGUAGE = 6;
    public static final int CONFIG_KEY_COUNTRY = 7;
    public static final int CONFIG_KEY_VERSION_CODE = 8;
    public static final int CONFIG_KEY_VERSION_NAME = 9;
    public static final int CONFIG_KEY_PACKAGE_NAME = 10;
    public static final int CONFIG_KEY_UUID = 11;
    public static final int CONFIG_KEY_MEDIA_SOURCE = 12;
    public static final int CONFIG_KEY_NETWORK_TYPE = 13;
    public static final int SDK_CONFIG_KEY_JSON_VERSION = 21;
    public static final int SDK_KEY_NETWORK_TYPE = 22;

    public static String getConfig(int key) {
        try {
            switch (key) {
                case CONFIG_KEY_APP_ID:
                    return IvySdk.getInstance().getAppId();
                case CONFIG_KEY_LEADER_BOARD_URL:
                    return GridManager.getInstance().getGridConfig("leader_board_url", "");
                case CONFIG_KEY_API_VERSION:
                    return GridManager.getInstance().getGridConfig("v_api", "26");
                case CONFIG_KEY_SCREEN_WIDTH:
                    return Integer.toString(Util.screenWidth());
                case CONFIG_KEY_SCREEN_HEIGHT:
                    return Integer.toString(Util.screenHeight());
                case CONFIG_KEY_LANGUAGE:
                    return Locale.getDefault().getLanguage();
                case CONFIG_KEY_COUNTRY:
                    return Util.countryCode();
                case CONFIG_KEY_VERSION_CODE:
                    return Long.toString(Util.versionCode());
                case CONFIG_KEY_VERSION_NAME:
                    return Util.versionName();
                case CONFIG_KEY_PACKAGE_NAME:
                    return Util.packageName();
                case CONFIG_KEY_UUID:
                    return Util.roleId();
                case CONFIG_KEY_MEDIA_SOURCE:
                    return LocalStorage.Instance.decodeString(IKeys.KEY_AF_MEDIA_SOURCE, "");
                case CONFIG_KEY_NETWORK_TYPE:
                case SDK_KEY_NETWORK_TYPE:
                    return PropertyUtil.getInstance().getNetworkType();
                case SDK_CONFIG_KEY_JSON_VERSION:
                    String domain = GridManager.getInstance().getGridConfig("domain", "");
                    if (domain != null) {
                        Uri uri = Uri.parse(domain);
                        String vApi = uri.getQueryParameter("v_api");
                        return vApi != null ? vApi : "";
                    } else {
                        return "";
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            ILog.e(IvySdk.TAG, "get app config err:" + e.getMessage());
        }
        return "";
    }
}

