package com.ivy.base.notification;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class OneTimeReminderWorker extends Worker {

    public OneTimeReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.e("TAG", "one time received:");
        Context context = getApplicationContext();
        NotificationTaskManager.getInstance().checkNotificationTasks(context);
        NotificationReminderScheduler.startReminder(context);
        return Result.success();
    }

}
