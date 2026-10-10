package com.ivy.base.notification;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


public class PeriodicReminderWorker extends Worker {
    public PeriodicReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.e("TAG", "Periodic received");

        Context context = getApplicationContext();
        NotificationTaskManager.getInstance().checkNotificationTasks(context);
        NotificationReminderScheduler.startAlarmReminder(context);
        NotificationReminderScheduler.startScheduleJob(context);
        return Result.success();
    }

}
