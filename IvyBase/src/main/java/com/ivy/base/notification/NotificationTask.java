package com.ivy.base.notification;

public class NotificationTask {

    public String tag; // 任务唯一标记
    public String title; // 通知标题
    public String subtitle; // 通知副标题
    public String bigText; // 长文本
    public String smallIcon;//状态栏角标
    public String largeIcon; //通知栏内大图标
    public String bigPicture;//大图模式
    public long pushTime;//推送时间
    public long interval;//推送间隔，repeat为true时生效
    public boolean autoCancel; // 通知栏是否可关闭
    public String action; // 通知栏自定义数据
    public boolean repeat; // 循环任务标记
    public long nextTime; // 下次推送时间

    public NotificationTask() {
    }

    public NotificationTask(String tag, String title, String subtitle, String bigText, String smallIcon, String largeIcon, String bigPicture, long pushTime, long interval, boolean autoCancel, String action, boolean repeat) {
        this.tag = tag;
        this.title = title;
        this.subtitle = subtitle;
        this.bigText = bigText;
        this.smallIcon = smallIcon;
        this.largeIcon = largeIcon;
        this.bigPicture = bigPicture;
        this.pushTime = pushTime;
        this.interval = interval;
        this.autoCancel = autoCancel;
        this.action = action;
        this.repeat = repeat;
        this.nextTime = pushTime;
    }

}
