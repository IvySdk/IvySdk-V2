package com.ivy.base.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.ivy.base.local.LocalStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdvertisingIdManager {

    public interface Callback {
        void onResult(String id);
    }

    private static final String KEY_AD_ID = "_ad_id_";

    private static volatile String memoryCache;
    private static final AtomicBoolean isLoading = new AtomicBoolean(false);
    private static final List<Callback> pendingCallbacks = new ArrayList<>();
    private static AtomicBoolean hasFinished = new AtomicBoolean(false);
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final long TIMEOUT_MS = 5000;

    private AdvertisingIdManager() {
    }

    public static void get(Context context, Callback callback) {

        // 内存缓存
        if (!TextUtils.isEmpty(memoryCache)) {
            postResult(callback, memoryCache);
            return;
        }

        // 本地缓存
        String local = LocalStorage.Instance.decodeString(KEY_AD_ID, null);
        if (!TextUtils.isEmpty(local)) {
            memoryCache = local;
            postResult(callback, local);
            return;
        }

        boolean shouldStart = false;

        synchronized (pendingCallbacks) {

            if (!TextUtils.isEmpty(memoryCache)) {
                postResult(callback, memoryCache);
                return;
            }

            if (callback != null) {
                pendingCallbacks.add(callback);
            }

            if (!isLoading.getAndSet(true)) {
                shouldStart = true;
            }
        }

        // 在锁外启动线程（避免阻塞）
        if (shouldStart) {
            executor.execute(() -> loadInternal(context.getApplicationContext()));
        }
    }

    // =========================
    // 内部加载逻辑
    // =========================
    private static void loadInternal(Context context) {
        String result = null;
        hasFinished.set(false);

        // 超时兜底 Runnable
        Runnable timeoutRunnable = () -> {
            if (!hasFinished.get()) {
                String fallbackId = Util.roleId();
                dispatchResult(fallbackId);
            }
        };
        mainHandler.removeCallbacksAndMessages(null);
        // 超时兜底，5秒后执行
        mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);
        try {
            AdvertisingIdClient.Info info =
                    AdvertisingIdClient.getAdvertisingIdInfo(context);

            if (info != null && !info.isLimitAdTrackingEnabled()) {
                String adId = info.getId();

                if (!TextUtils.isEmpty(adId) && !adId.startsWith("00000000")) {
                    result = adId;
                }
            }

        } catch (Exception ignored) {
        }
        // fallback
        if (TextUtils.isEmpty(result)) {
            result = Util.roleId();
        }
//
//        // 缓存
//        memoryCache = result;
//        LocalStorage.Instance.encodeString(KEY_AD_ID, result);
        // 分发结果
        dispatchResult(result);
        mainHandler.removeCallbacks(timeoutRunnable);
    }

    private static void dispatchResult(String id) {
        if (!hasFinished.getAndSet(true)) {

            memoryCache = id;
            LocalStorage.Instance.encodeString(KEY_AD_ID, id);
            List<Callback> callbacksCopy;
            synchronized (pendingCallbacks) {
                callbacksCopy = new ArrayList<>(pendingCallbacks);
                pendingCallbacks.clear();
                isLoading.set(false);
            }

            for (Callback cb : callbacksCopy) {
                postResult(cb, id);
            }
        }
    }

    private static void postResult(Callback callback, String id) {
        if (callback == null) return;
        mainHandler.post(() -> callback.onResult(id));
    }

//    public static void clear() {
//        memoryCache = null;
//        LocalStorage.Instance.encodeString(KEY_AD_ID, null);
//    }

    public static String getAdId() {
        String result = memoryCache;
        if (TextUtils.isEmpty(result)) {
            result = LocalStorage.Instance.decodeString(KEY_AD_ID, null);
        }
        return result;
    }

}