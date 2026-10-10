package com.ivy.firebase;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NotificationHelper {

    private static final String TAG = "FCM";

    private static final String CHANNEL_ID = "push_default";
    private static final String CHANNEL_NAME = "General Notifications";

    /**
     * 图片下载超时（ms）
     */
    private static final int IMAGE_TIMEOUT_MS = 3000;

    /**
     * 单线程池，避免并发撑爆
     */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private NotificationHelper() {
    }

    public static void show(Context context, String title, String body, Map<String, String> data) {
        ensureChannel(context);

        // Android 13+ 权限检查
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Notification permission not granted");
            return;
        }

        String imageUrl = data.get("image");
        String bigText = data.get("bigText");

        // 图片样式 → 异步
        if (imageUrl != null) {
            EXECUTOR.execute(() -> showBigPicture(context, title, body, data, imageUrl));
        } else if (!TextUtils.isEmpty(bigText)) {
            showBigText(context, title, body, bigText, data);
        } else {
            showNormal(context, title, body, data);
        }
    }

    // normal
    private static void showNormal(Context context, String title, String body, Map<String, String> data) {
        Notification notification = baseBuilder(context, title, body, data).build();
        notify(context, notification);
    }

    // BigText
    private static void showBigText(Context context, String title, String body, String bigText, Map<String, String> data) {
        Notification notification = baseBuilder(context, title, body, data).setStyle(new NotificationCompat.BigTextStyle().bigText(bigText)).build();
        notify(context, notification);
    }

    // BigPicture
    private static void showBigPicture(Context context, String title, String body, Map<String, String> data, String imageUrl) {
        Bitmap bitmap = downloadImage(imageUrl);
        NotificationCompat.Builder builder = baseBuilder(context, title, body, data);
        if (bitmap != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap));
        } else {
            // 图片失败 → 退化为 normal
        }
        notify(context, builder.build());
    }

    // 基础 Builder
    private static NotificationCompat.Builder baseBuilder(Context context, String title, String body, Map<String, String> data) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent == null) {
            intent = new Intent("android.intent.action.MAIN");
        }
        if (data != null){
            String data_str = new JSONObject(data).toString();
            intent.putExtra("payload", data_str);
        }

        intent.setPackage(context.getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.fcm_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
    }

    // 图片下载
    private static Bitmap downloadImage(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(IMAGE_TIMEOUT_MS);
            conn.setReadTimeout(IMAGE_TIMEOUT_MS);
            conn.setDoInput(true);
            conn.connect();

            if (conn.getResponseCode() != 200) return null;

            InputStream is = conn.getInputStream();
            return BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            Log.w(TAG, "Image download failed", e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // 通知发送
    private static void notify(Context context, Notification notification) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), notification);
    }

    // Channel
    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Activities");

        nm.createNotificationChannel(channel);
    }
}
