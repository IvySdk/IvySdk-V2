package com.ivy.base.notification;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;


@SuppressLint("SpecifyJobSchedulerIdRange")
public class NotifyJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.e("TAG", "Job schedule running...");
        NotificationTaskManager.getInstance().checkNotificationTasks(getApplicationContext());
        NotificationReminderScheduler.startReminder(getApplicationContext());
        return false; // 返回 true 表示还有后台任务
    }


    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}