package com.ivy.base.util;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class ScreenLayoutController {

    /**
     * 自由控制沉浸式与全屏行为
     *
     * @param activity      当前Activity
     * @param hideStatusBar 是否隐藏状态栏
     * @param hideNavBar    是否隐藏导航栏
     * @param extendToNotch 是否把画面延伸到刘海/状态栏区域 (穿透到最顶端)
     */
    public static void configureScreen(Activity activity, boolean hideStatusBar, boolean hideNavBar, boolean extendToNotch) {
        if (activity == null) return;

        Window window = activity.getWindow();

        WindowCompat.setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = window.getAttributes();
            if (extendToNotch) {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            } else {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
            }
            window.setAttributes(lp);
        }

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        int typesToHide = 0;
        if (hideStatusBar) {
            typesToHide |= WindowInsetsCompat.Type.statusBars();
        }
        if (hideNavBar) {
            typesToHide |= WindowInsetsCompat.Type.navigationBars();
        }
        if (typesToHide != 0) {
            controller.hide(typesToHide);
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars());
        }
    }
}