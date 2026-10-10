package com.ivy.track.reframe;

import android.text.TextUtils;

import com.ivy.base.local.LocalStorage;
import com.ivy.base.track.IKeys;
import com.ivy.base.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubCondition {
    private static final long ONE_DAY = 24 * 3600 * 1000;

    private String reEvent;
    private int index;
    private String eventName;
    private List<PropertyCondition> properties;
    private DateCondition date;


    private SubCondition() {
        // Private constructor

    }

    private static final Pattern PATTERN = Pattern.compile("([^|{[\\\\]]+)?(?:\\{([^}]*)\\}|\\\\[([^]]+)\\\\])?\\|(.+)$");
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\[(\\w+)\\]([+\\-*/><=]{1,3})([^,]+)");

    private static List<PropertyCondition> format(String condition) {
        if (Util.isValidString(condition)) {
            List<PropertyCondition> properties = new ArrayList<>();
            Matcher m = PROPERTY_PATTERN.matcher(condition);
            while (m.find()) {
                PropertyCondition c = new PropertyCondition(m.group(1), m.group(2), m.group(3));
                properties.add(c);
            }
            return properties;
        }
        return null;
    }

    public static SubCondition format(String reEvent, int index, String expression) throws Exception {
        SubCondition subCondition = new SubCondition();
        subCondition.reEvent = reEvent;
        subCondition.index = index;
        String dateStr = null;

        Matcher m = PATTERN.matcher(expression);
        if (m.matches()) {
            subCondition.eventName = m.group(1);
            String cond1 = m.group(2);  // {} 内
            String condition = cond1;
            subCondition.properties = format(condition);
            dateStr = m.group(3);
        } else {
            throw new IllegalArgumentException(expression + "-->解析失败!!!");
        }

        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                String[] p;
                if (dateStr.startsWith(">=")) {
                    p = dateStr.substring(2).split("d");
                    subCondition.date = new DateCondition(">=", Integer.parseInt(p[0]), Integer.parseInt(p[1]));
                } else if (dateStr.startsWith("<=")) {
                    p = dateStr.substring(2).split("d");
                    subCondition.date = new DateCondition("<=", Integer.parseInt(p[0]), Integer.parseInt(p[1]));
                } else if (dateStr.startsWith(">")) {
                    p = dateStr.substring(1).split("d");
                    subCondition.date = new DateCondition(">", Integer.parseInt(p[0]), Integer.parseInt(p[1]));
                } else if (dateStr.startsWith("<")) {
                    p = dateStr.substring(1).split("d");
                    subCondition.date = new DateCondition("<", Integer.parseInt(p[0]), Integer.parseInt(p[1]));
                } else if (dateStr.startsWith("=")) {
                    p = dateStr.substring(1).split("d");
                    subCondition.date = new DateCondition("=", Integer.parseInt(p[0]), Integer.parseInt(p[1]));
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date format: " + dateStr);
            }
        }

        return subCondition;
    }

    public void clear() {
        try {
            String key = formatKey("meet-times");
            LocalStorage.Instance.removeValueForKey(key);
            key = formatKey("accumulate-value");
            LocalStorage.Instance.removeValueForKey(key);
            key = formatKey("record-status");
            LocalStorage.Instance.removeValueForKey(key);
            key = formatKey("acc_value");
            LocalStorage.Instance.removeValueForKey(key);
        } catch (Exception ignored) {
        }
    }

    private boolean loadRecordedState(String key) {
        return LocalStorage.Instance.decodeBoolean(key, false);
    }

    private void recordStatus(String key, boolean status) {
        LocalStorage.Instance.encodeBoolean(key, status);
    }

    private String formatKey(String subFix) {
        String propertySubFix = (properties != null && properties.size() > 0) ? properties.get(0).getProperty() + "-" + properties.get(0).getOperation() : "";
        String subEventNameSubFix = (eventName != null) ? eventName : "";
        return Integer.toString((reEvent + "-" + index + "-" + subEventNameSubFix + "-" + propertySubFix + "-" + subFix).hashCode());
    }

    public double realValue(){
        String key = formatKey("acc_value");
        return LocalStorage.Instance.decodeDouble(key, 0.0);
    }

    public boolean isMeet(String event, Map<String, Object> eventParams) throws Exception {
        String statusKey = formatKey("record-status");
        boolean status = loadRecordedState(statusKey);
        if (status) {
            return true;
        }

        if (compareEventName(event) && compareDate() && compareProperty(eventParams)) {
//            if (date == null || date.getDays() == 0) {
//                recordStatus(statusKey, true);
//                return true;
//            }

            String key = formatKey("meet-times");
            int times = LocalStorage.Instance.decodeInt(key, 0) + 1;
            LocalStorage.Instance.encodeInt(key, times);

            boolean result = false;
            switch (date.getOperation()) {
                case ">=":
                    result = times >= date.getTimes();
                    break;
                case "<=":
                    result = times <= date.getTimes();
                    break;
                case ">":
                    result = times > date.getTimes();
                    break;
                case "<":
                    result = times < date.getTimes();
                    break;
                case "=":
                    result = times == date.getTimes();
                    break;
            }

            recordStatus(statusKey, result);
            return result;
        }
        return false;
    }

    private boolean compareDate() throws Exception {
        if (date == null) return true;
        if (date.getDays() == 0) return true;

        long firstOpenTime = LocalStorage.Instance.decodeLong(IKeys.KEY_FIRST_OPEN_TIME, System.currentTimeMillis());
        long openedDays = (System.currentTimeMillis() - firstOpenTime) / ONE_DAY + 1;
        return openedDays <= date.getDays();
    }

    private boolean compareEventName(String event) {
        if (TextUtils.isEmpty(eventName)) {
            return true;
        }
        if (TextUtils.isEmpty(event)) {
            return false;
        }
        String[] args = eventName.split(",");
        for (String arg : args) {
            if (arg.equals(event)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkDate(PropertyCondition property) {
        try {
            int _target = Integer.parseInt(property.getValue().toString());
            long firstOpenTime = LocalStorage.Instance.decodeLong(IKeys.KEY_FIRST_OPEN_TIME, System.currentTimeMillis());
            int current_day = (int) Math.floor((System.currentTimeMillis() - firstOpenTime) / (24 * 60 * 60 * 1000.0f));
            switch (property.getOperation()) {
                case "=":
                    return _target == current_day;
                case ">":
                    return current_day > _target;
                case ">=":
                    return current_day >= _target;
                case "<":
                    return current_day < _target;
                case "<=":
                    return current_day <= _target;
                default:
                    return false;
            }
        } catch (Exception e) {

        }
        return false;
    }

    private boolean compareProperty(Map<String, Object> eventParams) throws Exception {
        if (properties == null) return true;
        if (properties.isEmpty()) return true;

        List<Boolean> status = new ArrayList<>();
        for (PropertyCondition property : properties) {
            boolean _passProperty = false;
            if (property.getProperty().equals("days")) {
                _passProperty = checkDate(property);
                status.add(_passProperty);
                continue;
            }

            if (eventParams == null || !eventParams.containsKey(property.getProperty())) {
                _passProperty = false;
                break;
            }
            Object value = eventParams.get(property.getProperty());
            switch (property.getOperation()) {
                case "=":
                    _passProperty = smartEquals(property.getValue(), value);
//                    _passProperty = Objects.equals(String.valueOf(property.getValue()), String.valueOf(value));
                    break;
                case "++>":
                case "++<":
                    double fValue = Double.parseDouble(value.toString());
                    double preValue = Double.parseDouble(property.getValue().toString());
                    String key = formatKey("accumulate-value");
                    double cacheValue = LocalStorage.Instance.decodeDouble(key, 0.0) + fValue;
                    LocalStorage.Instance.encodeDouble(key, cacheValue);
                    _passProperty = property.getOperation().equals("++>") ? cacheValue > preValue : cacheValue < preValue;
                    if (_passProperty && eventName.equalsIgnoreCase("af_ad_revenue")){
                        String value_key = formatKey("acc_value");
                        LocalStorage.Instance.encodeDouble(value_key, cacheValue);
                    }
                    break;
                default:
                    double numValue = Double.parseDouble(value.toString());
                    double numPreValue = Double.parseDouble(property.getValue().toString());
                    switch (property.getOperation()) {
                        case ">=":
                            _passProperty = numValue >= numPreValue;
                            break;
                        case "<=":
                            _passProperty = numValue <= numPreValue;
                            break;
                        case ">":
                            _passProperty = numValue > numPreValue;
                            break;
                        case "<":
                            _passProperty = numValue < numPreValue;
                            break;
                        default:
                            _passProperty = false;
                    }
            }
            status.add(_passProperty);
        }
        if (status.size() == properties.size()) {
            for (boolean state : status) {
                if (!state) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    // Getters and setters would be needed for all fields
    public String getReEvent() {
        return reEvent;
    }

    public int getIndex() {
        return index;
    }

    private boolean smartEquals(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        if (a instanceof Number && b instanceof Number) {
            return Double.compare(
                    ((Number) a).doubleValue(),
                    ((Number) b).doubleValue()
            ) == 0;
        }

        return Objects.equals(a.toString(), b.toString());
    }


}
