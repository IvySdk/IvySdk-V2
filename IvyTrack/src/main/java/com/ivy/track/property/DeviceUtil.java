package com.ivy.track.property;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;


import com.ivy.base.local.LocalStorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;

public class DeviceUtil {

    public static String getHarmonyOSVersion() {
        String version = null;
        if (isHarmonyOS()) {
            version = getProp("hw_sc.build.platform.version", "");
            if (TextUtils.isEmpty(version)) {
                version = exec("getprop hw_sc.build.platform.version");
            }
        }
        return version;
    }

    private static String getProp(String property, String defaultValue) {
        try {
            Class<?> spClz = Class.forName("android.os.SystemProperties");
            Method method = spClz.getDeclaredMethod("get", String.class);
            String value = (String) method.invoke(spClz, property);
            if (TextUtils.isEmpty(value)) {
                return defaultValue;
            }
            return value;
        } catch (Throwable ignored) {
        }
        return defaultValue;
    }

    private static boolean isHarmonyOS() {
        try {
            Class<?> buildExClass = Class.forName("com.huawei.system.BuildEx");
            Object osBrand = buildExClass.getMethod("getOsBrand").invoke(buildExClass);
            return osBrand != null && "harmony".equalsIgnoreCase(osBrand.toString());
        } catch (Throwable e) {
            return false;
        }
    }

    private static String exec(String command) {
        InputStreamReader ir = null;
        BufferedReader input = null;
        try {
            Process process = Runtime.getRuntime().exec(command);
            ir = new InputStreamReader(process.getInputStream());
            input = new BufferedReader(ir);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = input.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (Throwable ignored) {
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable ignored) {
                }
            }
            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    public static String getCarrier(Context context) {
        HashMap<String, String> carrierMap = new HashMap<String, String>() {{
            put("46000", "中国移动");
            put("46002", "中国移动");
            put("46007", "中国移动");
            put("46008", "中国移动");
            put("46001", "中国联通");
            put("46006", "中国联通");
            put("46009", "中国联通");
            put("46003", "中国电信");
            put("46005", "中国电信");
            put("46011", "中国电信");
            put("46004", "中国卫通");
            put("46020", "中国铁通");
        }};

        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String simOperator = tm.getSimOperator();
            if (!TextUtils.isEmpty(simOperator) && carrierMap.containsKey(simOperator)) {
                return carrierMap.get(simOperator);
            }

            String simOperatorName = tm.getSimOperatorName();
            if (!TextUtils.isEmpty(simOperatorName)) {
                return simOperatorName;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @SuppressLint("HardwareIds")
    public static String getAndroidId(Context context) {
        String androidId = LocalStorage.Instance.decodeString("_ANDROID_ID_", "");
        try {
            if (TextUtils.isEmpty(androidId)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        androidId = Settings.Secure.getString(
                                context.getContentResolver(), Settings.Secure.ANDROID_ID);
                    } catch (SecurityException e) {
                        Log.e("device", "Error: Requires special permissions or targeting pre-Android 10.");
                    }
                } else {
                    androidId = Settings.Secure.getString(
                            context.getContentResolver(), Settings.Secure.ANDROID_ID);
                }
            }
            if (TextUtils.isEmpty(androidId)) {
                androidId = UUID.randomUUID().toString();
            }
        } catch (Exception ignored) {
        }
        LocalStorage.Instance.encodeString("_ANDROID_ID_", androidId);
        return androidId;
    }

    public static String getDeviceType(Context context) {
        try {
            int screenLayout = context.getResources().getConfiguration().screenLayout;
            int screenSize = screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
            return (screenSize < Configuration.SCREENLAYOUT_SIZE_LARGE) ? "Phone" : "Tablet";
        } catch (Exception ignored) {
        }
        return "Phone";
    }

    public static int[] getDeviceSize(Context context) {
        int[] size = new int[2];
        try {
            int screenWidth;
            int screenHeight;
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            int rotation = display.getRotation();
            Point point = new Point();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealSize(point);
                screenWidth = point.x;
                screenHeight = point.y;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                display.getSize(point);
                screenWidth = point.x;
                screenHeight = point.y;
            } else {
                screenWidth = display.getWidth();
                screenHeight = display.getHeight();
            }
            size[0] = screenWidth;
            size[1] = screenHeight;
        } catch (Exception e) {
            if (context.getResources() != null) {
                android.util.DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                size[0] = displayMetrics.widthPixels;
                size[1] = displayMetrics.heightPixels;
            }
        }
        return size;
    }

    private static int getNaturalWidth(int rotation, int width, int height) {
        return (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) ? width : height;
    }

    private static int getNaturalHeight(int rotation, int width, int height) {
        return (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) ? height : width;
    }
}