package com.ivy.base.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.ivy.base.ILog;
import com.ivy.base.R;
import com.ivy.base.util.Util;

import java.io.InputStream;
import java.util.Random;

public class LocalNotification {


    private static final String DEFAULT_CHANNEL_ID = "custom_notification";
    private static final String DEFAULT_CHANNEL_NAME = "Custom Notification";



    public static void show(Context context, NotificationTask task) {
        try {
            if (context == null) return;
            if (task == null) return;
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID, DEFAULT_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                manager.createNotificationChannel(channel);
            }

            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Util.isValidString(task.action)) {
                    intent.putExtra("notification_action", task.action);
                }
                intent.setPackage(context.getPackageName());
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setWhen(System.currentTimeMillis()).setAutoCancel(task.autoCancel).setPriority(NotificationCompat.PRIORITY_DEFAULT);
            if (Util.isValidString(task.title)) {
                builder.setContentTitle(task.title);
            }
            if (Util.isValidString(task.subtitle)) {
                builder.setContentText(task.subtitle);
            }
            AssetManager assetManager = context.getAssets();
            Bitmap largeIcon = null;
            if (Util.isValidString(task.largeIcon)) {
                try (InputStream input = assetManager.open(task.largeIcon)) {
                    largeIcon = BitmapFactory.decodeStream(input);
                    builder.setLargeIcon(largeIcon);
                } catch (Exception e) {
                    ILog.e("notification", "read large icon err:" + e.getMessage());
                }
            }

            if (intent != null) {
                PendingIntent pendingIntent;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    pendingIntent = PendingIntent.getActivity(context, 101, intent, PendingIntent.FLAG_IMMUTABLE);
                } else {
                    pendingIntent = PendingIntent.getActivity(context, 101, intent, 0);
                }
                builder.setContentIntent(pendingIntent);
            }

            if (Util.isValidString(task.smallIcon)) {
                try (InputStream input = assetManager.open(task.smallIcon)) {
                    Bitmap small = BitmapFactory.decodeStream(input);
                    builder.setSmallIcon(IconCompat.createWithBitmap(small));
                } catch (Exception e) {
                    ILog.e("notification", "read small icon err:" + e.getMessage());
                }
            } else {
                builder.setSmallIcon(R.drawable.icon_notification); // 默认图标
            }

            if (Util.isValidString(task.bigPicture)) {
                try (InputStream input = assetManager.open(task.bigPicture)) {
                    Bitmap bigP = BitmapFactory.decodeStream(input);
                    builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bigP).bigLargeIcon(largeIcon));
                } catch (Exception e) {
                    ILog.e("notification", "read big picture err:" + e.getMessage());
                }

            } else if (Util.isValidString(task.bigText)) {
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(task.bigText).setSummaryText(task.subtitle));
            }

            int itemId = new Random().nextInt(100);
            manager.notify(itemId, builder.build());

        } catch (Exception e) {
            ILog.e("LocalNotificationUtil", "show notification err:" + e.getMessage());
        }
    }

}
