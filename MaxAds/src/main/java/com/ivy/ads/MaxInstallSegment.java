package com.ivy.ads;

import org.json.JSONObject;

public class MaxInstallSegment {
    public int hour;
    public String segment;

    public MaxInstallSegment(JSONObject jsonObject){
        this.hour = jsonObject.optInt("hour", 0);
        this.segment = jsonObject.optString("segment", "");
    }

    public void apply(BaseAd ad){
        // 获取当前安装时间（小时）
        int currentInstallHours = TimeUtil.getCurrentHourSinceInstall();

        // 如果当前安装时间小于等于配置的安装时间阈值，则应用分段规则
        if (currentInstallHours <= hour) {
            if (segment != null) {
                MaxSegmentManager.addSegment(segment);
                if (ad.segments != null) {
                    ad.segments = ad.segments + "," + this.segment;
                } else {
                    ad.segments = this.segment;
                }
            }
        }
    }
}