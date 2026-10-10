package com.ivy.base.notification;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.local.LocalStorage;

public class NotificationPermissionUtil {


    private static final int NOTIFICATION_REQUEST_CODE = 101;
    private static final String TAG = "NotificationPermission";

    private NotificationPermissionUtil() {
        // 私有构造，防止实例化
    }

    public static boolean isPermissionEnabled() {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            } else {
                return NotificationManagerCompat.from(activity).areNotificationsEnabled();
            }
        } else {
            ILog.w(TAG, "invalid activity for check");
            return false;
        }
    }

    /**
     * 判断当前权限状态
     * 0: 权限被彻底拒绝，需要跳转设置页面开启
     * 1: 权限已开启
     * 2: 权限状态待定，仍可通过系统接口请求
     */
    public static int loadNotificationPermissionState() {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                ILog.i(TAG, "notification permission granted");
                return 1;
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                    ILog.i(TAG, "should request permission by client");
                    return 2;
                } else {
                    boolean isFirstRequest = LocalStorage.Instance.decodeBoolean("is_first_notification", false);
                    if (!isFirstRequest) {
                        ILog.i(TAG, "first request! just require by client");
                        return 2;
                    }
                    return 0;
                }
            }
        } else {
            ILog.w(TAG, "invalid activity for check");
            return 1;
        }
    }

    /**
     * 仅弹出系统权限请求弹窗
     */
    public static void requestNotificationPermission() {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST_CODE);
                } else {
                    ILog.i(TAG, "permission already allowed");
                }
            } else {
                ILog.i(TAG, "permission auto allowed on system lower 23");
            }
        }
    }

    public static void autoRequestNotificationPermission() {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST_CODE);
                        } else {
                            boolean state = LocalStorage.Instance.decodeBoolean("is_first_notification", true);
                            if (state) {
                                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST_CODE);
                            } else {
                                openNotificationSettings();
                            }
                        }
                    }
                } else {
                    boolean enabled = NotificationManagerCompat.from(activity).areNotificationsEnabled();
                    if (!enabled) {
                        openNotificationSettings();
                    }
                }
            }
        }
    }

    public static void openNotificationSettings() {
        try {
            Activity activity = ActivityUtil.getInstance().getActivity();
            if (activity != null) {
                Intent intent = new Intent();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                    intent.putExtra("app_package", activity.getPackageName());
                    intent.putExtra("app_uid", activity.getApplicationInfo().uid);
                } else {
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                }
                activity.startActivity(intent);
            } else {
                ILog.i(TAG, "open permission settings error: activity invalid");
            }
        } catch (Throwable t) {
            ILog.e(TAG, "open permission settings error:" + t.getMessage());
        }
    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        try {
            Activity activity = ActivityUtil.getInstance().getActivity();
            if (activity != null) {
                if (requestCode == NOTIFICATION_REQUEST_CODE && permissions.length > 0) {
                    for (String permission : permissions) {
                        if (Manifest.permission.POST_NOTIFICATIONS.equals(permission)) {
                            boolean state = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS);
                            boolean preState = LocalStorage.Instance.decodeBoolean("is_first_notification", false);
                            if (state && !preState) {
                                LocalStorage.Instance.encodeBoolean("is_first_notification", true);
                            }
                        }
                    }
                }
            } else {
                ILog.i(TAG, "parse permission request result error: activity invalid!!!");
            }
        } catch (Exception e) {
            ILog.e(TAG, "parse permission request result error:" + e.getMessage());
        }
    }

    public static boolean isNotificationChannelClosed(String channelId) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager manager = (NotificationManager) App.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel channel = manager.getNotificationChannel(channelId);
                if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                    return true;
                }
            }
        } catch (Exception e) {
            ILog.e(TAG, "check channel state err:" + e.getMessage());
        }
        return false;
    }

    public static void openNotificationChannel(String channelId) {
        try {
            Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, App.getInstance().getPackageName());
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, channelId);
            Activity activity = ActivityUtil.getInstance().getActivity();
            if (activity != null) {
                activity.startActivity(intent);
            }
        } catch (Exception e) {
            ILog.e(TAG, "require open channel err:" + e.getMessage());
        }
    }

}
