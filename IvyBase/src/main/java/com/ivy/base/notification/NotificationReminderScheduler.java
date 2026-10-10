package com.ivy.base.notification;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.ivy.base.ILog;

import java.util.concurrent.TimeUnit;

public class NotificationReminderScheduler {
    private static final int ALARM_REQUEST_CODE = 10011;
    private static final int JOB_ID = 10011;
    private static final String ALARM_ACTION = "com.notification.alarm.reminder";

    public static void startReminder(Context context) {
        try {
            startAlarmReminder(context);
            startPeriodicReminder(context);
            startScheduleJob(context);
        } catch (Exception e) {
            ILog.e("notification", "start notification task failed:" + e.getMessage());
        }
    }

    public static void startAlarmReminder(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction(ALARM_ACTION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
            long interval = 15 * 60 * 1000; // 15分钟
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, pendingIntent);
            } else {
                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, interval, pendingIntent);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void startScheduleJob(Context context) {
        try {
            @SuppressLint("JobSchedulerService")
            ComponentName serviceName = new ComponentName(context, NotifyJobService.class);
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceName)
                    .setPersisted(true) // 开机后依然存在
                    .setMinimumLatency(5 * 60 * 1000) // 最早5分钟后执行
                    .setOverrideDeadline(10 * 60 * 1000); // 最晚10分钟必须执行
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.cancel(JOB_ID);
            jobScheduler.schedule(builder.build());
        } catch (Throwable ignored) {
        }
    }

    public static void startOneTimeReminder(Context context, long delay_seconds) {
        try {
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(OneTimeReminderWorker.class).setInitialDelay(delay_seconds, TimeUnit.MILLISECONDS).build();
            WorkManager.getInstance(context).enqueue(request);
        } catch (Throwable ignored) {
        }
    }

    public static void startPeriodicReminder(Context context) {
        try {
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(PeriodicReminderWorker.class, 15, TimeUnit.MINUTES).setInitialDelay(10, TimeUnit.SECONDS).build();
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("notify_periodic_work", ExistingPeriodicWorkPolicy.KEEP, request);
        } catch (Throwable ignored) {
        }
    }


}
