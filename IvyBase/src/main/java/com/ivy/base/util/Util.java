package com.ivy.base.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.track.IKeys;
import com.ivy.base.util.rx.Action;
import com.ivy.base.util.rx.Func;
import com.ivy.base.util.rx.Observer;
import com.ivy.base.util.rx.RxAndroid;
import com.ivy.base.util.rx.Schedulers;
import com.ivy.base.util.rx.SimpleRxJust;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Util {

    public static final byte[] ivBytes = new byte[]{0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30};

    private static String _keyHash = null;

    public static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static String cachedCountryCode = null;

    public static boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static String simCountryCode() {
        if (cachedCountryCode != null) {
            return cachedCountryCode;
        }
        try {
            TelephonyManager tm = (TelephonyManager) App.getInstance().getSystemService(Context.TELEPHONY_SERVICE);
            // 1. 获取当前挂载的网络运营商所在的国家（基站信息）
            // 只要有信号，漫游时会返回当地国家
            String networkCountry = tm.getNetworkCountryIso();
            if (networkCountry != null && !networkCountry.isEmpty()) {
                networkCountry = networkCountry.toUpperCase();
                cachedCountryCode = networkCountry;
                return networkCountry;
            }

            // 2. 获取 SIM 卡发行地的国家
            // 没信号也能拿到，但没插卡就拿不到
            String simCountry = tm.getSimCountryIso();
            if (simCountry != null && !simCountry.isEmpty()) {
                simCountry = simCountry.toUpperCase();
                cachedCountryCode = simCountry;
                return simCountry;
            }
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String countryCode() {
        String code = LocalStorage.Instance.decodeString("cache_country_code", "");
        if (!TextUtils.isEmpty(code)) {
            return code;
        }
        code = simCountryCode();
        if (!TextUtils.isEmpty(code)) {
            return code;
        }
        return Locale.getDefault().getCountry();
    }

    public static int screenWidth() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager wm = (WindowManager) App.getInstance().getSystemService(Context.WINDOW_SERVICE);
                Rect rect = wm.getCurrentWindowMetrics().getBounds();
                return rect.width();
            } else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                WindowManager wm = (WindowManager) App.getInstance().getSystemService(Context.WINDOW_SERVICE);
                wm.getDefaultDisplay().getMetrics(displayMetrics);
                return displayMetrics.widthPixels;
            }
        } catch (Exception e) {
            ILog.i("Util", "get screen width err:" + e.getMessage());
        }
        return 0;
    }

    public static int screenHeight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager wm = (WindowManager) App.getInstance().getSystemService(Context.WINDOW_SERVICE);
                Rect rect = wm.getCurrentWindowMetrics().getBounds();
                return rect.height();
            } else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                WindowManager wm = (WindowManager) App.getInstance().getSystemService(Context.WINDOW_SERVICE);
                wm.getDefaultDisplay().getMetrics(displayMetrics);
                return displayMetrics.heightPixels;
            }
        } catch (Exception e) {
            ILog.i("Util", "get screen height err:" + e.getMessage());
        }
        return 0;
    }

    private static long version_code = -1;

    public static long versionCode() {
        try {
            if (version_code < 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    version_code = App.getInstance().getPackageManager().getPackageInfo(App.getInstance().getPackageName(), PackageManager.GET_ACTIVITIES).getLongVersionCode();
                } else {
                    version_code = App.getInstance().getPackageManager().getPackageInfo(App.getInstance().getPackageName(), PackageManager.GET_ACTIVITIES).versionCode;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version_code;
    }

    private static String version_name = "0";

    public static String versionName() {
        try {
            if (version_name.equals("0")) {
                version_name = App.getInstance().getPackageManager().getPackageInfo(App.getInstance().getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version_name;
    }

    public static String packageName() {
        return App.getInstance().getPackageName();
    }

    public static String roleId() {
        String uuid = LocalStorage.Instance.decodeString("_ANDROID_*****_UUID_", null);
        if (uuid == null || uuid.trim().isEmpty()) {
            uuid = AdvertisingIdManager.getAdId();
            if (uuid == null || uuid.trim().isEmpty()) {
                uuid = UUID.randomUUID().toString();
            }
            LocalStorage.Instance.encodeString("_ANDROID_*****_UUID_", uuid);
        }
        return uuid;
    }

    public static String userId() {
        String cuid = LocalStorage.Instance.decodeString(IKeys.KEY_CUSTOM_USER_ID, null);
        if (cuid == null) {
            String ssid = LocalStorage.Instance.decodeString("appsflyer_id", "");
            if (ssid.isEmpty()) {
                return Util.roleId();
            } else {
                return ssid;
            }
        } else {
            return cuid;
        }
    }

    public static String UUID() {
        String uuid = LocalStorage.Instance.decodeString("_ANDROID_***_TEST_***_UUID_", null);
        if (uuid == null || uuid.trim().isEmpty()) {
            uuid = java.util.UUID.randomUUID().toString();
            LocalStorage.Instance.encodeString("_ANDROID_***_TEST_***_UUID_", uuid);
        }
        return uuid;
    }

    public static boolean isRoleIdAlreadySet() {
        String uuid = LocalStorage.Instance.decodeString("_ANDROID_*****_UUID_", null);
        return uuid != null && !uuid.trim().isEmpty();
    }

    public static boolean checkGooglePlayServices() {
        try {
            int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(App.getInstance());
            if (status == ConnectionResult.SUCCESS) {
                ILog.i("Util", "Google Play services support");
                return true;
            } else {
                ILog.w("Util", "Google Play services not supported !!!");
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public interface Callback {
        void onResult(String adId);
    }

    public static String getCachedAdvertisingId() {
        String adId = LocalStorage.Instance.decodeString("_ad_id_", null);
        return adId;
    }

    public static void loadAdvertisingId(Action<String> action) {
        new Thread(() -> {
            String adId = null;
            try {
                if (checkGooglePlayServices()) {
                    AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(App.getInstance());
                    if (!info.isLimitAdTrackingEnabled()) {
                        adId = info.getId();
                    }
                    if (TextUtils.isEmpty(adId) || adId.startsWith("00000000")) {
                        adId = null;
                    }
                }
                ILog.e("Util", " adId loaded:" + adId);
                if (adId != null) {
                    String finalAdId = adId;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        LocalStorage.Instance.encodeString("_ad_id_", finalAdId);
                    });
                    if (action != null) {
                        action.call(finalAdId);
                    }
                } else {
                    if (action != null) {
                        action.call(null);
                    }
                }
            } catch (Exception e) {
                ILog.e("Util", "load adId failed:" + e.getMessage());
                if (action != null) {
                    action.call(null);
                }
            }
        }).start();
    }

    public static void doLoadAdvertisingId(Action<String> action) {
        try {
            String adId = getCachedAdvertisingId();
            if (!TextUtils.isEmpty(adId)) {
                if (action != null) {
                    action.call(adId);
                }
                return;
            }

            loadAdvertisingId(s -> {
                if (TextUtils.isEmpty(s)) {
                    s = roleId();
                }
                if (action != null) {
                    action.call(s);
                }
            });

        } catch (Exception e) {
            if (action != null) {
                action.call(roleId());
            }
        }
    }

    public static String md5(String content) {
        try {
            byte[] bytes = MessageDigest.getInstance("md5").digest(content.getBytes());
            StringBuilder ret = new StringBuilder(bytes.length << 1);
            for (byte b : bytes) {
                ret.append(Character.forDigit((b >> 4) & 0xf, 16));
                ret.append(Character.forDigit(b & 0xf, 16));
            }
            return ret.toString();
        } catch (Exception e) {
            return content;
        }
    }

    public static String randomString(int length, boolean onlyWorld) {
        String src = "qwertyuioplmkjnhbgvfcdxsazAQWERTYUIOPLKJHGFDSZXCVBNM123456789";
        if (!onlyWorld) {
            src += "<>?[]!@#$%*{}=-+_";
        }
        StringBuilder builder = new StringBuilder();
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < length; i++) {
            int index = Math.abs(random.nextInt()) % src.length();
            builder.append(src.charAt(index));
        }
        return builder.toString();
    }

    private static String getSignatureHash(Signature signature) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(signature.toByteArray());
        return Base64.encodeToString(md.digest(), Base64.NO_WRAP).trim();
    }

    public static String getKeyStoreHash(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager pm = context.getPackageManager();
                android.content.pm.PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                if (packageInfo.signingInfo != null) {
                    if (packageInfo.signingInfo.hasMultipleSigners()) {
                        for (Signature signature : packageInfo.signingInfo.getApkContentsSigners()) {
                            return getSignatureHash(signature);
                        }
                    } else {
                        return getSignatureHash(packageInfo.signingInfo.getSigningCertificateHistory()[0]);
                    }
                }
            } else {
                PackageManager pm = context.getPackageManager();
                @SuppressWarnings("deprecation") Signature[] signatures = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
                if (signatures != null) {
                    for (Signature signature : signatures) {
                        return getSignatureHash(signature);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getKeyHash(Context context) {
        if (_keyHash == null) {
            _keyHash = getKeyStoreHash(context);
            if (_keyHash != null && _keyHash.length() < 32) {
                int padding = 32 - _keyHash.length();
                byte[] s = new byte[padding];
                System.arraycopy(ivBytes, 0, s, 0, padding);
                _keyHash += new String(s);
            }
        }
        return _keyHash;
    }

    public static String encodeParams(Context context, String data) {
        return encodeParams(context, data.getBytes());
    }

    public static String encodeParams(Context context, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(getKeyHash(context).getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(data);
            String result = Base64.encodeToString(encrypted, Base64.URL_SAFE | Base64.NO_WRAP).trim();
            ILog.i("Util", "encodeParams result: >>> " + result);
            return result;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decodeParams(Context context, String data) {
        try {
            return decodeParams(context, data.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String decodeParams(Context context, byte[] data) {
        try {
            byte[] keyHash = getKeyHash(context).getBytes("UTF-8");
            byte[] decoded = Base64.decode(data, Base64.URL_SAFE | Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(keyHash, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] plainText = cipher.doFinal(decoded);
            return new String(plainText);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decodeWithKey(String content, String key) {
        try {
            byte[] decoded = Base64.decode(content, Base64.URL_SAFE | Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] plainText = cipher.doFinal(decoded);
            return new String(plainText);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String base64encode(String input) {
        return Base64.encodeToString(input.getBytes(), Base64.DEFAULT);
    }

    public static String base64Decode(String input) {
        return new String(Base64.decode(input, Base64.DEFAULT));
    }

    public static float dp2Px(Context context, float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    public static long installTime() throws Exception {
        return App.getInstance().getPackageManager().getPackageInfo(App.getInstance().getPackageName(), 0).firstInstallTime;
    }

    public static long updateTime() throws Exception {
        return App.getInstance().getPackageManager().getPackageInfo(App.getInstance().getPackageName(), 0).lastUpdateTime;
    }

    public static long totalMemory() {
        try {
            ActivityManager manager = (ActivityManager) App.getInstance().getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            manager.getMemoryInfo(info);
            return info.totalMem;
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private static volatile long lastAvailableMemory = -1;
    private static volatile long lastAvailableMemoryTimestamp = -1;

    public static long availableMemory() {
        if (lastAvailableMemory <= 0) {
            try {
                ActivityManager manager = (ActivityManager) App.getInstance().getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
                manager.getMemoryInfo(info);
                lastAvailableMemory = info.availMem;
            } catch (Throwable ignored) {
            }
        } else {
            if (SystemClock.elapsedRealtime() - lastAvailableMemoryTimestamp > 5 * 1000) {
                fetchAvailableMemory();
                lastAvailableMemoryTimestamp = SystemClock.elapsedRealtime();
            }
        }
        return lastAvailableMemory;
    }

    private static void fetchAvailableMemory() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ActivityManager manager = (ActivityManager) App.getInstance().getSystemService(Context.ACTIVITY_SERVICE);
                    ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
                    manager.getMemoryInfo(info);
                    lastAvailableMemory = info.availMem;
                } catch (Throwable ignored) {
                }
            }
        });
    }

    public static long totalDiskSize() {
        try {
            StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
            return statFs.getTotalBytes();
        } catch (Exception ignored) {
        }
        return -1L;
    }

    public static long availableDiskSize() {
        try {
            StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
            return statFs.getAvailableBytes();
        } catch (Exception ignored) {
        }
        return -1L;
    }

    public static int byte2KB(long bytes) {
        return (int) (bytes / 1000.0f);
    }

    public static int byte2MB(long bytes) {
        return (int) (bytes / 1000.0f / 1000.0f);
    }

    public static float byte2GB(long bytes) {
        return bytes / 1000.0f / 1000.0f / 1000.0f;
    }

    public static boolean copyTxt(String txt) {
        try {
            ClipboardManager clipBoard = (ClipboardManager) App.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("label", txt);
            clipBoard.setPrimaryClip(clipData);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isNetworkConnected() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) App.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
            return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    public static Map<String, Object> formatAdClientInfo(String clientInfo) {
        if (clientInfo != null && !clientInfo.isEmpty()) {
            Map<String, Object> clientInfos = new HashMap<>();
            try {
                JSONObject json = new JSONObject(clientInfo);
                Iterator<String> iterator = json.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    Object value = json.get(key);
                    if (value instanceof Boolean) {
                        clientInfos.put(key, ((Boolean) value) ? 1 : 0);
                    } else {
                        clientInfos.put(key, value);
                    }
                }
                return clientInfos;
            } catch (Exception e) {
                ILog.e("Util", "format client info failed:" + e.getMessage());
            }
        }
        return null;
    }

    public static void openFacebookPage(String pageId) {
        try {
            String facebookScheme = "fb://page/" + pageId;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(facebookScheme));
            ActivityUtil.getInstance().getActivity().startActivity(intent);
        } catch (Exception e) {
            try {
                String facebookUrl = "https://www.facebook.com/" + pageId;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl));
                ActivityUtil.getInstance().getActivity().startActivity(intent);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void goLauncher() {
        try {
            Activity activity = ActivityUtil.getInstance().getActivity();
            if (activity != null) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean isNightMode() {
        try {
            int nightModeFlags = App.getInstance().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        } catch (Exception ignored) {
        }
        return false;
    }

    public static void loadCachedFileData(String file_path, Action<String> action) {
        new SimpleRxJust<>(file_path).map(new Func<String, String>() {
            @Override
            public String call(String value) throws Exception {
                File file = new File(file_path);
                if (!file.exists()) return null;
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    byte[] buffer = new byte[(int) file.length()];
                    int read = fis.read(buffer);
                    if (read > 0) {
                        return new String(buffer, 0, read, "UTF-8");
                    }
                } catch (Exception e) {
                    Log.w("Util", "read cached file:" + file_path + " failed:" + e.getMessage());
                } finally {
                    try {
                        if (fis != null) fis.close();
                    } catch (Exception ignored) {
                    }
                }
                return null;
            }
        }).subscribeOn(Schedulers.io()).observeOn(RxAndroid.main()).subscribe(new Observer<String>() {
            @Override
            public void onSuccess(String value) {
                if (action != null) {
                    action.call(value);
                }
            }

            @Override
            public void onError(Throwable e) {
                if (action != null) {
                    action.call(null);
                }
            }
        });
    }

    public static void cacheDataToFile(String file_path, String data) {
        if (data == null) return;
        new SimpleRxJust<>(new Pair<String, String>(file_path, data)).map(new Func<Pair<String, String>, Void>() {
            @Override
            public Void call(Pair<String, String> pair) throws Exception {
                FileOutputStream fos = null;
                try {
                    File file = new File(App.getInstance().getCacheDir(), pair.first);
                    fos = new FileOutputStream(file);
                    fos.write(pair.second.getBytes("UTF-8"));
                    fos.flush();
                } catch (Exception e) {
                    Log.w("Util", "cache file:" + pair.first + " failed:" + e.getMessage());
                } finally {
                    try {
                        if (fos != null) fos.close();
                    } catch (Exception ignored) {
                    }
                }
                return null;
            }
        }).subscribeOn(Schedulers.io()).observeOn(RxAndroid.main()).subscribe(new Observer<Void>() {
            @Override
            public void onSuccess(Void value) {

            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }

    public static void deleteCachedFile(String file_path) {
        new SimpleRxJust<>(file_path).map(new Func<String, Void>() {
            @Override
            public Void call(String s) throws Exception {
                try {
                    File file = new File(s);
                    if (file.exists()) file.delete();
                } catch (Exception e) {
                    Log.w("Util", "delete cached file:" + s + " failed:" + e.getMessage());
                }
                return null;
            }
        }).subscribeOn(Schedulers.io()).observeOn(RxAndroid.main()).subscribe(new Observer<Void>() {
            @Override
            public void onSuccess(Void value) {

            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }

    public static String loadStrFromJSONObject(JSONObject src, String key) {
        try {
            if (src.has(key)) {
                String str = src.getString(key);
                if ("null".equals(str)) {
                    return null;
                }
                return str;
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    public static <T> T createInstance(String className) {
        try {
            // 获取 Class 对象
            Class<?> clazz = Class.forName(className);

            // 获取无参构造函数并实例化
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            return (T) constructor.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            return token;
        }
    }

    public static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

}
