package com.ivy.base.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AlarmReceiver extends BroadcastReceiver {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("TAG", "alarm received");
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                NotificationTaskManager.getInstance().checkNotificationTasks(context);
                NotificationReminderScheduler.startPeriodicReminder(context);
                NotificationReminderScheduler.startScheduleJob(context);
            }
        });
    }
}
