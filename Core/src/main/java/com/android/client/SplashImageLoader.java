package com.android.client;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.ivy.base.ILog;

public class SplashImageLoader {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 当前 splash View
    private static volatile View currentSplashView;
    private static volatile View fastCoverView;

    // 防止快速 remove/show 冲突
    private static volatile boolean isRemoving = false;

    private static int getWindowBackgroundColor(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
//        return typedValue.data;

        return typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT
                ? typedValue.data
                : -1;
    }

    /**
     * 加载并显示 Splash
     */
    public static void loadAndAddSplash(final Context context, final String layoutName, final ViewGroup parentView) {
        if (context == null || parentView == null || layoutName == null) {
            return;
        }
        try {
            // 如果已有 splash
            removeSplashImmediate(parentView);
            isRemoving = false;

            int backColor = getWindowBackgroundColor(context);
            // 增加瞬时占位view, 防止防止xml加载前短时间的空窗期
            View cover = new View(context);
            try {
                if (backColor == -1){
                    cover.setBackgroundColor(Color.WHITE);
                } else {
                    cover.setBackgroundColor(backColor);
                }
            } catch (Exception ignored) {
            }
            cover.setClickable(true);
            cover.setFocusable(true);
            cover.setAlpha(1f);
            fastCoverView = cover;
            parentView.addView(cover, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            // 加载xml
            int layoutId = context.getResources().getIdentifier(layoutName, "layout", context.getPackageName());
            if (layoutId == 0) {
                return;
            }
            View splashView = LayoutInflater.from(context).inflate(layoutId, parentView, false);
            splashView.setClickable(true);
            splashView.setClickable(true);
            try {
                if (backColor == -1){
                    splashView.setBackgroundColor(Color.WHITE);
                } else {
                    splashView.setBackgroundColor(backColor);
                }
            } catch (Exception ignored) {
            }
            currentSplashView = splashView;
            parentView.addView(splashView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            splashView.setAlpha(0f);
            splashView.setScaleX(1.02f);
            splashView.setScaleY(1.02f);

            ImageView logo = splashView.findViewWithTag("logo");
            if (logo != null) {
                logo.animate().cancel();
                logo.setAlpha(0f);
                logo.setScaleX(0.92f);
                logo.setScaleY(0.92f);
                boolean isLandscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;//.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) logo.getLayoutParams();
                params.matchConstraintPercentWidth = isLandscape ? 0.25f : 0.7f;
                logo.setLayoutParams(params);
            }

            splashView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(250).start();
            final View targetCover = cover;
            mainHandler.postDelayed(() -> {
                try {
                    if (fastCoverView != null && fastCoverView == targetCover) {
                        if (fastCoverView.getParent() == parentView) {
                            parentView.removeView(fastCoverView);
                        }
                        fastCoverView = null;
                    }
                } catch (Exception ignored) {
                }
            }, 5);
            if (logo != null) {
                logo.animate().cancel();
                logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(450).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 渐隐移除 Splash
     */
    public static void removeSplash(final ViewGroup parentView) {
        mainHandler.post(() -> {
            try {
                if (isRemoving) {
                    ILog.d("unity", "call hide splash: isRemoving");
                    return;
                }
                try {
                    if (fastCoverView != null) {
                        if (fastCoverView.getParent() == parentView) {
                            parentView.removeView(fastCoverView);
                        }
                        fastCoverView = null;
                    }
                } catch (Exception ignored) {
                }
                final View splashView = currentSplashView;
                if (splashView == null) {
                    ILog.d("unity", "call hide splash: splashView is null");
                    return;
                }
                if (splashView.getParent() != parentView) {
                    currentSplashView = null;
                    ILog.d("unity", "call hide splash: splashView parent error");
                    return;
                }
                isRemoving = true;
                splashView.animate().cancel();
                splashView.animate().alpha(0f).setDuration(220).withEndAction(() -> {
                    try {
                        if (splashView.getParent() == parentView) {
                            parentView.removeView(splashView);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (currentSplashView == splashView) {
                        currentSplashView = null;
                    }
                    isRemoving = false;
                    ILog.d("unity", "call hide splash: splashView remove animation end");
                }).start();
            } catch (Exception e) {
                ILog.d("unity", "call hide splash: splashView remove error:" + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 立即移除（内部使用）
     */
    private static void removeSplashImmediate(final ViewGroup parentView) {
        try {
            if (fastCoverView != null) {
                if (fastCoverView.getParent() == parentView) {
                    parentView.removeView(fastCoverView);
                }
                fastCoverView = null;
            }
        } catch (Exception ignored) {
        }
        try {
            if (currentSplashView != null) {
                currentSplashView.animate().cancel();
                if (currentSplashView.getParent() == parentView) {
                    parentView.removeView(currentSplashView);
                }
                currentSplashView = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        isRemoving = false;
    }
}