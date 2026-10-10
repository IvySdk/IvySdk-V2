package com.ivy.track.reframe;

import com.ivy.base.local.LocalStorage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InvalidObjectException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Condition {
    private String eventName;
    private List<Token> postfix;
    private boolean repeat;
    private List<Integer> countArray;
    private List<SubCondition> subConditions;

    private Condition() {
        // Private constructor
    }

    public static Condition format(String event, JSONObject json) throws Exception {
        Condition condition = new Condition();
        condition.eventName = event;

        if (json.has("expr")) {
            String expr = json.optString("expr");
            condition.postfix = EventLogicUtil.formatLogicalExpression(expr);
            if (condition.postfix == null || condition.postfix.isEmpty()) {
                throw new InvalidParameterException(event + "-->规则表达式解析失败!!!");
            }
        } else {
            throw new InvalidParameterException(event + "-->未配置规则表达式!!!");
        }

        condition.repeat = json.optBoolean("repeat", false);

        if (json.has("count")) {
            JSONArray countArr = json.getJSONArray("count");
            if (countArr.length() > 0) {
                condition.countArray = new ArrayList<>();
                for (int i = 0; i < countArr.length(); i++) {
                    condition.countArray.add(countArr.getInt(i));
                }
            }
        }

        if (json.has("params")) {
            JSONArray paramsArr = json.getJSONArray("params");
            if (paramsArr.length() > 0) {
                condition.subConditions = new ArrayList<>();
                for (int i = 0; i < paramsArr.length(); i++) {
                    SubCondition sub = SubCondition.format(event, i + 1, paramsArr.optString(i));
                    condition.subConditions.add(sub);
                }
            } else {
                throw new InvalidParameterException(event + "-->未配置规则条件!!!");
            }
        } else {
            throw new InvalidParameterException(event + "-->未配置规则条件!!!");
        }
        return condition;
    }

    public void clearSubConditionStatus() {
        try {
            if (subConditions != null) {
                for (SubCondition sub : subConditions) {
                    sub.clear();
                }
            }
        } catch (Exception ignored) {
        }
    }


    public Result isMeet(String event, Map<String, Object> eventParams) throws Exception {
        if (postfix == null || postfix.isEmpty()) {
            throw new InvalidObjectException(eventName + "-->无效逻辑表达");
        }
        double value = 0;
        Map<String, Boolean> resultMap = new HashMap<>();
        if (subConditions != null) {
            for (SubCondition sub : subConditions) {
                boolean result = sub.isMeet(event, eventParams);
                value = sub.realValue();
                resultMap.put(String.valueOf(sub.getIndex()), result);
            }
        }

        boolean result = EventLogicUtil.evaluate(postfix, resultMap);
        if (result) {
            if (countArray != null) {
                String timesCountKey = "ev_" + eventName + "_count";
                int count = LocalStorage.Instance.decodeInt(timesCountKey, 0) + 1;
                LocalStorage.Instance.encodeInt(timesCountKey, count);

                for (int v : countArray) {
                    if (v == count) {
                        String newEventName = eventName + "_" + v;
                        boolean alreadySent = LocalStorage.Instance.decodeBoolean(newEventName, false);
                        if (alreadySent) {
                            return new Result(false, null, value);
                        }
                        LocalStorage.Instance.encodeBoolean(newEventName, true);
                        clearSubConditionStatus();
                        return new Result(true, newEventName, value);
                    }
                }
                clearSubConditionStatus();
                return new Result(false, null, value);
            } else {
                if (repeat) {
                    clearSubConditionStatus();
                    return new Result(true, eventName, value);
                } else {
                    String key = "ev_" + eventName;
                    boolean alreadySent = LocalStorage.Instance.decodeBoolean(key, false);
                    if (alreadySent) {
                        return new Result(false, null, value);
                    }
                    LocalStorage.Instance.encodeBoolean(key, true);
                    clearSubConditionStatus();
                    return new Result(true, eventName, value);
                }
            }
        }
        return new Result(false, null, value);
    }


}