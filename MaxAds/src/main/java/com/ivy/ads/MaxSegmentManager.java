package com.ivy.ads;

import com.applovin.mediation.MaxSegment;
import com.applovin.mediation.MaxSegmentCollection;

import java.util.ArrayList;
import java.util.List;

public class MaxSegmentManager {

    public static MaxSegmentCollection.Builder mSegmentCollection;
    public static void addSegment(MaxSegment segment) {
        if (mSegmentCollection == null) {
            mSegmentCollection = MaxSegmentCollection.builder();
        }
        mSegmentCollection.addSegment(segment);
    }

    public static void addSegment(String segmentStr){
        if (mSegmentCollection == null) {
            mSegmentCollection = MaxSegmentCollection.builder();
        }
        mSegmentCollection.addSegment(str2Segment(segmentStr));
    }

    /**
     * 将字符串格式的分段规则转换为AppLovin的MaxSegment对象
     * 例如："1217:3" 转换为 MaxSegment(1217, [3])
     * @param str 分段规则字符串，格式为"标签ID:索引值"
     * @return MaxSegment对象，如果转换失败则返回null
     */
    private static MaxSegment str2Segment(String str) {
        try {
            String[] parts = str.split(":");
            if (parts.length == 2) {
                try {
                    int tagId = Integer.parseInt(parts[0]);
                    int indexValue = Integer.parseInt(parts[1]);

                    List<Integer> indexList = new ArrayList<>();
                    indexList.add(indexValue);
                    return new MaxSegment(tagId, indexList);
                } catch (NumberFormatException e) {
                    // 转换失败，记录日志或处理异常
                }
            }
        } catch (Exception e) {
            // 转换失败，记录日志或处理异常
        }
        return null;
    }
}
