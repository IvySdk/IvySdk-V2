package com.ivy.base.util;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class InsetsHelper {

    public interface OnInsetsCallback {
        void onInsets(int statusBarHeight, int navigationBarHeight, int gestureBarHeight, int cutoutHeight, boolean gestureNavigation, boolean hasCutout);
    }

    private static int lastStatusBarHeight = -1;
    private static int lastNavigationBarHeight = -1;
    private static int lastCutoutHeight = -1;
    private static int lastGestureBarHeight = -1;
    private static Boolean lastGestureNavigation = null; // 改为包装类，更精准记录状态改变
    private static Boolean lastCutout = null; // 改为包装类，更精准记录状态改变

    //状态栏高度
    public static int getStatusBarHeight() {
        return lastStatusBarHeight;
    }

    // 老式底部三键导航高度
    public static int getNavigationBarHeight() {
        return lastNavigationBarHeight;
    }

    //手势导航条高度
    public static int getGestureBarHeight() {
        return lastGestureBarHeight;
    }

    //是否存在手势导航条
    public static boolean hasGestureNavigation() {
        if (lastGestureNavigation != null) {
            return lastGestureNavigation;
        }
        return false;
    }

    public static boolean hasCutout() {
        if (lastCutout != null) {
            return lastCutout;
        }
        return false;
    }

    public static int getCutoutHeight() {
        return lastCutoutHeight;
    }

    public static void loadInsets(@NonNull Activity activity, @NonNull OnInsetsCallback callback) {
        //主动加载window信息
        try {
            // 重置状态
            lastStatusBarHeight = -1;
            lastNavigationBarHeight = -1;
            lastGestureBarHeight = -1;
            lastCutoutHeight = -1;
            lastGestureNavigation = null;
            lastCutout = null;
            View decorView = activity.getWindow().getDecorView();
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decorView);

            loadInsetsTask(activity, decorView, insets, callback);
        } catch (Exception ignored) {
        }
    }

    public static void observeInsets(@NonNull Activity activity, @NonNull OnInsetsCallback callback) {
        final View decorView = activity.getWindow().getDecorView();

        // 重置状态
        lastStatusBarHeight = -1;
        lastNavigationBarHeight = -1;
        lastGestureBarHeight = -1;
        lastCutoutHeight = -1;
        lastGestureNavigation = null;
        lastCutout = null;
//        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(activity.getWindow().getDecorView());
        ViewCompat.setOnApplyWindowInsetsListener(decorView, (v, insets) -> {
            loadInsetsTask(activity, decorView, insets, callback);
            return insets;
        });

        if (decorView.isAttachedToWindow()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                decorView.requestApplyInsets();
            } else {
                ViewCompat.requestApplyInsets(decorView);
            }
        }
    }

    private static void loadInsetsTask(@NonNull Activity activity, @NonNull View decorView, @NonNull WindowInsetsCompat insets, @NonNull OnInsetsCallback callback) {
        Insets statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
        Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

        int statusBarHeight = statusInsets.top;
        int navigationBarHeight = navInsets.bottom;

        // 1. 拦截死循环导致的清零包
        if (statusBarHeight == 0 && navigationBarHeight == 0 && lastStatusBarHeight > 0) {
            return;
        }

        // 2. 获取官方手势高度
        int gestureBarHeight = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Insets gestureInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
            gestureBarHeight = gestureInsets.bottom;
        }

        // 3. 官方刘海屏获取
        int cutoutHeight = 0;
        boolean hasCutout = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowInsets platformInsets = insets.toWindowInsets();
            if (platformInsets != null) {
                DisplayCutout cutout = platformInsets.getDisplayCutout();
                if (cutout != null) {
                    hasCutout = true;
                    cutoutHeight = Math.max(cutout.getSafeInsetTop(), statusBarHeight);
                }
            }
        }

        // 4. 【核心修改：前置特征盘查】
        // 不再单纯依赖 navigationBarHeight == 0。
        // 先通过 Settings 强行去读底层硬件开关，如果读到了手势开启，那不管系统现在返回的 navHeight 是多少，一律强行断定为手势导航。
        boolean gestureNavigation = checkGestureNavigationAbsolute(activity, navigationBarHeight, gestureBarHeight);

        // 5. 如果判定是全面屏手势，将处于过渡期的假 navigationBarHeight 强行校准为 0，防止外部布局被顶起
        if (gestureNavigation) {
            navigationBarHeight = 0;
        }

        // 6. 【数据防抖拦截】
        // 只有当任何一个核心数值真的变了，才向外通知
        if (statusBarHeight == lastStatusBarHeight && navigationBarHeight == lastNavigationBarHeight && cutoutHeight == lastCutoutHeight && lastGestureNavigation != null && gestureNavigation == lastGestureNavigation) {
            return;
        }

        // 更新缓存记录
        lastStatusBarHeight = statusBarHeight;
        lastNavigationBarHeight = navigationBarHeight;
        lastCutoutHeight = cutoutHeight;
        lastGestureNavigation = gestureNavigation;
        lastGestureBarHeight = gestureBarHeight;
        lastCutout = hasCutout;

        // 7. 安全回调给外部
        callback.onInsets(statusBarHeight, navigationBarHeight, gestureBarHeight, cutoutHeight, gestureNavigation, hasCutout);
    }

    private static boolean checkGestureNavigationAbsolute(Context context, int navHeight, int gestureHeight) {
        // 优先盘查各家手机的硬开关设置
        try {
            // 小米 MIUI / HyperOS
            if (Settings.Global.getInt(context.getContentResolver(), "force_fsg_nav", 0) != 0)
                return true;
            // 华为 EMUI / HarmonyOS
            if (Settings.Global.getInt(context.getContentResolver(), "navigationbar_is_min", 0) != 0)
                return true;
            // VIVO Funtouch / OriginOS
            if (Settings.Secure.getInt(context.getContentResolver(), "navigation_gesture_on", 0) != 0)
                return true;
            // OPPO ColorOS
            if (Settings.Secure.getInt(context.getContentResolver(), "hide_navigationbar_enable", 0) != 0)
                return true;
        } catch (Exception ignored) {
        }

        // 三星 Android 9 旧机型特征补丁
        if (Build.BRAND.toLowerCase().contains("samsung") && Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            float density = context.getResources().getDisplayMetrics().density;
            float navHeightDp = navHeight / (density == 0 ? 1 : density);
            if (navHeight > 0 && navHeightDp < 30) {
                return true;
            }
        }

        // 官方标准 Android 10+ 判定：如果硬件设置没读到，走官方标准
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 情况 A：最标准的全面屏手势，导航栏彻底隐藏（navHeight 完美等于 0）
            if (navHeight == 0 && gestureHeight > 0) {
                return true;
            }

            // 情况 B：
            // 当 navHeight > 0，但它和手势触发高度【完全相等】或者【高度差小于 5 像素】时，
            // 100% 可以断定这就是全面屏手势下的小横条，而不是传统的三大金刚键！
            if (navHeight > 0 && gestureHeight > 0 && Math.abs(navHeight - gestureHeight) <= 5) {
                return true;
            }
        }

        // 4. 物理全面屏长宽比兜底（保持原样，防 onCreate 时速黑洞）
        try {
            android.util.DisplayMetrics dm = context.getResources().getDisplayMetrics();
            int max = Math.max(dm.widthPixels, dm.heightPixels);
            int min = Math.min(dm.widthPixels, dm.heightPixels);
            float aspectRatio = (float) max / min;
            if (aspectRatio > 1.85f && navHeight > (max * 0.05f)) {
                return true;
            }
        } catch (Exception ignored) {
        }

        // 终极兜底
        return navHeight == 0;
    }
}