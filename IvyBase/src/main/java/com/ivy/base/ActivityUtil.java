package com.ivy.base;

import android.app.Activity;
import android.os.Build;

import java.lang.ref.WeakReference;

public class ActivityUtil {

    private static volatile ActivityUtil instance;

    private WeakReference<Activity> activityInstance;
    private WeakReference<Activity> testActivityInstance;

    private ActivityUtil() {
        // 私有构造函数
    }

    public static ActivityUtil getInstance() {
        if (instance == null) {
            synchronized (ActivityUtil.class) {
                if (instance == null) {
                    instance = new ActivityUtil();
                }
            }
        }
        return instance;
    }

    public Activity getActivity() {
        if (testActivityInstance != null && testActivityInstance.get() != null) {
            return testActivityInstance.get();
        }
        return activityInstance != null ? activityInstance.get() : null;
    }

    public boolean isActivityValid() {
        Activity act = getActivity();
        if (act == null) return false;
        if (act.isFinishing()) return false;
        if (Build.VERSION.SDK_INT >= 17 && act.isDestroyed()) return false;
        return true;
    }

    public void setActivity(Activity activity) {
        this.activityInstance = new WeakReference<>(activity);
    }

    public void setTestActivity(Activity activity) {
        this.testActivityInstance = new WeakReference<>(activity);
    }
}
