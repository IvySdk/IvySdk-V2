package com.ivy.base.util;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.Display;

import androidx.core.content.FileProvider;

import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.activities.WebViewActivity;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class IvyUtil {

    public static final String TAG = "IvyUtil";

    private IvyUtil() {
        // 工具类不允许实例化
    }

    public static void systemShareText(Activity activity, String txt) {
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, txt);
            intent.setType("text/plain");
            activity.startActivity(intent);
        } catch (Exception e) {
            ILog.e(TAG, "system share txt failed:" + e.getMessage());
        }
    }

    public static void systemShareImage(String title, String imagePath) {
        try {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Uri uri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    uri = FileProvider.getUriForFile(App.getInstance(), App.getInstance().getPackageName() + ".fileprovider", imgFile);
                } else {
                    uri = Uri.fromFile(imgFile);
                }

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.setType("image/*");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent shareChooser = Intent.createChooser(shareIntent, title);
                if (shareIntent.resolveActivity(App.getInstance().getPackageManager()) != null) {
                    Activity activity = ActivityUtil.getInstance().getActivity();
                    if (activity != null) {
                        activity.startActivity(shareChooser);
                    }
                } else {
                    throw new Exception("unable open share activity");
                }
            } else {
                throw new IllegalArgumentException("invalid img path");
            }
        } catch (Exception e) {
            ILog.e(TAG, "system share img failed:" + e.getMessage());
        }
    }

    public static void openUrlWithBrowser(String url) {
        if (url == null) {
            ILog.e(TAG, "invalid url to open with browser");
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            PackageManager pm = App.getInstance().getPackageManager();
            if (pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                Activity activity = ActivityUtil.getInstance().getActivity();
                if (activity != null) {
                    activity.startActivity(intent);
                } else {
                    ILog.e(TAG, "invalid activity to open browser");
                }
            }
        } catch (Exception e) {
            ILog.e(TAG, "open browser for " + url + " failed;" + e.getMessage());
        }
    }

    public static void showWebView(String title, String url) {
        try {
            if (TextUtils.isEmpty(url)){
                ILog.e(TAG, "invalid url to open webview");
                return;
            }
            Activity activity = ActivityUtil.getInstance().getActivity();
            if (activity != null) {
                Intent intent = new Intent(activity, WebViewActivity.class);
                if (TextUtils.isEmpty(title)) {
                    intent.putExtra("title", title);
                }
                intent.putExtra("url", url);
                activity.startActivity(intent);
            } else {
                ILog.e(TAG, "invalid activity to open webview");
            }
        } catch (Exception e) {
            openUrlWithBrowser(url);
        }
    }

    public static JSONObject loadDeviceInfoJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("model", Build.MODEL);
            json.put("device", Build.DEVICE);
            json.put("device_type", getDeviceType(App.getInstance()));
            json.put("device_model", Build.BRAND + "::" + Build.MODEL);
            json.put("update_dimension_table", 1);
            json.put("brand", Build.BRAND);
            json.put("board", Build.BOARD);
            json.put("android_api_version", Build.VERSION.SDK_INT);
            json.put("android_version", Build.VERSION.RELEASE);
            json.put("manufacturer", Build.MANUFACTURER);
            json.put("abi", Build.SUPPORTED_ABIS[0]);
            json.put("cores", Runtime.getRuntime().availableProcessors());

            DisplayManager displayManager = (DisplayManager) App.getInstance().getSystemService(Context.DISPLAY_SERVICE);
            if (displayManager != null) {
                Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
                if (display != null) {
                    json.put("refresh_rate", display.getRefreshRate());
                }
            }

            android.util.DisplayMetrics displayMetrics = App.getInstance().getResources().getDisplayMetrics();
            json.put("density", displayMetrics.density);
            json.put("density_dpi", displayMetrics.densityDpi);
            json.put("width_pixels", displayMetrics.widthPixels);
            json.put("height_pixels", displayMetrics.heightPixels);
            json.put("ram", Util.byte2GB(Util.totalMemory()));
            json.put("disk", Util.byte2GB(Util.totalDiskSize()));
            return json;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, Object> loadDeviceInfo() {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("model", Build.MODEL);
            map.put("device", Build.DEVICE);
            map.put("device_type", getDeviceType(App.getInstance()));
            map.put("device_model", Build.MANUFACTURER + "_" + Build.MODEL);
            map.put("update_dimension_table", 1);
            map.put("brand", Build.BRAND);
            map.put("board", Build.BOARD);
            map.put("android_api_version", Build.VERSION.SDK_INT);
            map.put("android_version", Build.VERSION.RELEASE);
            map.put("manufacturer", Build.MANUFACTURER);
            map.put("abi", Build.SUPPORTED_ABIS[0]);
            map.put("cores", Runtime.getRuntime().availableProcessors());

            DisplayManager displayManager = (DisplayManager) App.getInstance().getSystemService(Context.DISPLAY_SERVICE);
            if (displayManager != null) {
                Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
                if (display != null) {
                    map.put("refresh_rate", display.getRefreshRate());
                }
            }

            android.util.DisplayMetrics displayMetrics = App.getInstance().getResources().getDisplayMetrics();
            map.put("density", displayMetrics.density);
            map.put("density_dpi", displayMetrics.densityDpi);
            map.put("width_pixels", displayMetrics.widthPixels);
            map.put("height_pixels", displayMetrics.heightPixels);
            map.put("ram", Util.byte2GB(Util.totalMemory()));
            map.put("disk", Util.byte2GB(Util.totalDiskSize()));
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getDeviceType(Context context) {
        try {
            int screenLayout = context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
            if (screenLayout < Configuration.SCREENLAYOUT_SIZE_LARGE) {
                return "Phone";
            } else {
                return "Tablet";
            }
        } catch (Exception e) {
            return "Phone";
        }
    }
}
