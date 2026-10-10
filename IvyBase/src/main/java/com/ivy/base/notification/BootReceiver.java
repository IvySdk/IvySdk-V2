package com.ivy.base.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                NotificationTaskManager.getInstance().checkNotificationTasks(context);
                NotificationTaskManager.getInstance().checkBootNotificationTasks(context);
                NotificationReminderScheduler.startReminder(context);
            }
        });
    }
}
