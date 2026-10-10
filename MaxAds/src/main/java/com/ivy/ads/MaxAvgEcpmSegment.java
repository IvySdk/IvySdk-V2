package com.ivy.ads;

import com.applovin.mediation.MaxSegment;
import com.ivy.base.ad.AdType;
import com.ivy.base.local.LocalStorage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MaxAvgEcpmSegment {
    private int lastNSession;
    private int tag;
    private float[] ecpm;
    private int[] index;

    public MaxAvgEcpmSegment(JSONObject jsonObject){
        this.lastNSession = jsonObject.optInt("lastNSession", 0);
        this.tag = jsonObject.optInt("tag", 0);

        // 解析ecpm数组
        if (jsonObject.has("ecpm")) {
            JSONArray ecpmArray = jsonObject.optJSONArray("ecpm");
            this.ecpm = new float[ecpmArray.length()];
            for (int i = 0; i < ecpmArray.length(); i++) {
                this.ecpm[i] = (float) ecpmArray.optDouble(i, 0);
            }
        }

        // 解析index数组
        if (jsonObject.has("index")) {
            JSONArray indexArray = jsonObject.optJSONArray("index");
            this.index = new int[indexArray.length()];
            for (int i = 0; i < indexArray.length(); i++) {
                this.index[i] = indexArray.optInt(i, 0);
            }
        }
    }

    private float calculateAvgEcpmForLastNSessions(AdType adType) {
        try {
            // 获取当前会话ID
            String sessionCountKey = "ivy_session_count";
            int currentSession = LocalStorage.Instance.decodeInt(sessionCountKey, 0);

            // 遍历最近N次会话
            float totalEcpm = 0f;
            int totalCount = 0;

            // 使用配置的N值
            int n = lastNSession;
            for (int i = 0; i < n; i++) {
                int session = currentSession - i - 1;
                if (session < 0) break;

                String sessionEcpmKey = "ivy_session_" + session + "_ecpm_" + adType.getValue();
                String sessionCounterKey = "ivy_session_" + session + "_counter_" + adType.getValue();

                float sessionEcpm = LocalStorage.Instance.decodeFloat(sessionEcpmKey, 0f);
                int sessionCount = LocalStorage.Instance.decodeInt(sessionCounterKey, 0);

                if (sessionCount > 0) {
                    totalEcpm += sessionEcpm;
                    totalCount += sessionCount;
                }
            }

            // 如果有有效数据，计算平均值
            return totalCount > 0 ? totalEcpm / totalCount : 0f;
        } catch (Exception e) {
            e.printStackTrace();
            return 0f;
        }
    }

    public void apply(BaseAd ad){
        float avgEcpm = calculateAvgEcpmForLastNSessions(ad.adType);
        if (avgEcpm == 0f) return;

        // 查找对应的分段索引
        int segmentIndex = -1;
        for (int i = 0; i < ecpm.length; i++) {
            if (avgEcpm < ecpm[i]) {
                segmentIndex = i;
                break;
            }
        }

        // 如果找到了有效的分段索引，则应用分段规则
        if (segmentIndex >= 0 && segmentIndex < index.length) {
            List<Integer> indexList = new ArrayList<>();
            indexList.add(index[segmentIndex]);
            MaxSegmentManager.addSegment(new MaxSegment(tag, indexList));

            String segmentStr = tag + ":" + index[segmentIndex];
            if (ad.segments == null){
                ad.segments = segmentStr;
            } else {
                ad.segments = ad.segments + "," + segmentStr;
            }
        }
    }
}