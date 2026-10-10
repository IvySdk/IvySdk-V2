package com.ivy.firebase;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.notification.NotificationReminderScheduler;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseCloudMessageService extends FirebaseMessagingService {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private boolean isAppInForeground() {
        try {
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses == null) return false;

            final String packageName = getPackageName();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void handleDataPayload(Map<String, String> data) {
        try {
            ILog.d("FCM", "received message payload:" + data);
        } catch (Exception ignored) {

        }
        if (data == null) return;
        if (data.size() <= 0) return;
        // 1. 直接上报
        try {
            String uuid = UUID.randomUUID().toString();
            String data_str = new JSONObject(data).toString();
            SharedPreferences sp = getApplication().getSharedPreferences("fcm_push_data", Context.MODE_PRIVATE);
            sp.edit().putString(uuid, data_str).apply();

            String pre = sp.getString("push_ids", "");
            sp.edit().putString("push_ids", pre + "," + uuid).apply();
        } catch (Exception ignored) {
        }

        // 2. 保存上报记录，补充部分由fcm自动展示的通知上报丢失上报

    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        ILog.d("FCM", "received message");
        // 1. 始终处理 Data 数据逻辑，上报通知到达事件
        Map<String, String> data = message.getData();
        handleDataPayload(data);

        // 2. 处理通知展示
        if (message.getNotification() != null) {
            // 如果应用在前台，FCM 不会自弹通知，我们需要手动弹
            if (isAppInForeground()) {
                String title = message.getNotification().getTitle();
                String body = message.getNotification().getBody();
                NotificationHelper.show(getApplicationContext(), title, body, data);
            } else {
                // 如果应用在后台，由fcm接管自动弹出
                NotificationReminderScheduler.startReminder(App.getInstance());
            }
        }

    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // need send to server?
        try {
            ILog.d("firebase", "fb_push_token:" + token);
            if (!TextUtils.isEmpty(token)) {
                App.setInstance(getApplication());
                LocalStorage.Instance.checkInit();
                if (!TextUtils.isEmpty(token)) {
                    LocalStorage.Instance.encodeString("fb_push_token", token);
                }
            }
        } catch (Exception e) {

        }
    }
}
