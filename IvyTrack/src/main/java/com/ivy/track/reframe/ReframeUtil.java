package com.ivy.track.reframe;

import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ReframeUtil {
    private static final String TAG = "reframe";
    private final List<Condition> conditions = new ArrayList<>();

    private static ReframeUtil instance;

    public static ReframeUtil getInstance() {
        if (instance == null) {
            instance = new ReframeUtil();
        }
        return instance;
    }

    private ReframeUtil() {
        // Private constructor for singleton
    }

    public void setup(JSONObject json) {
        try {
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                try {
                    String eventName = keys.next();
                    Condition condition = Condition.format(eventName, json.getJSONObject(eventName));
                    conditions.add(condition);
                } catch (Exception e) {
                    Log.e(TAG, "format err:" + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    public void clear() {
        try {
            Iterator<Condition> iter = conditions.iterator();
            while (iter.hasNext()) {
                try {
                    Condition condition = iter.next();
                    condition.clearSubConditionStatus();
                } catch (Exception e) {

                }
            }
        } catch (Exception ignored) {
        }
    }

    public List<Result> check(String event, Map<String, Object> eventParams) {
        List<Result> list = new ArrayList<>();
        try {
            Iterator<Condition> iter = conditions.iterator();
            while (iter.hasNext()) {
                try {
                    Condition condition = iter.next();
                    Result result = condition.isMeet(event, eventParams);
                    if (result.state && result.str != null && !result.str.isEmpty()) {
                        list.add(result);
                    }
                } catch (Exception e) {
                    Log.i(TAG, "check reframe event for:" + event + " :" + e.getMessage());
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

}