//package com.ivy.core.ab;
//
//import android.content.Context;
//
//import com.ivy.base.grid.GridManager;
//import com.ivy.base.util.AdvertisingIdManager;
//import com.ivy.track.IvyTrack;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class IvyAB {
//
//    private IAB hsab;
//    public static final IvyAB Instance = new IvyAB();
//
//    private final List<EventTask> waitingEventTasks = new ArrayList<>();
//    public boolean inited = false;
//    private final JSONObject userProperties = new JSONObject();
//    private final HashMap<String, Object> eventParams = new HashMap<>();
//
//    private boolean country = false;
//    private boolean mediaSource = false;
//    private boolean adsInited = false;
//    private boolean isAbVersionSet = false;
//
//    public static class EventTask {
//        public String eventName;
//        public String eventType;
//        public String eventSrc;
//        public Map<String, Object> params;
//
//        public EventTask(String eventName, String eventType, String eventSrc, Map<String, Object> params) {
//            this.eventName = eventName;
//            this.eventType = eventType;
//            this.eventSrc = eventSrc;
//            this.params = params;
//        }
//    }
//
//    public void init(int testId, Context context, boolean debug, IAB.Callback callback) {
//        if (testId == 1) {
//            String js = GridManager.getInstance().getGridConfig("abtest", "");
//            if (js != null && !js.isEmpty()) {
//                try {
//                    AdvertisingIdManager.get(context, new AdvertisingIdManager.Callback() {
//                        @Override
//                        public void onResult(String id) {
//                            try {
//                                JSONObject data = new JSONObject(js).getJSONObject("hsab");
//                                hsab = new Hsab();
//                                hsab.init(context, data.optString("appid"), data.optString("channel"), id, !debug);
//                                hsab.setUserInfo(userProperties);
//                                hsab.setHeaderInfo(eventParams);
//                                hsab.loadAbTestConfigs(callback);
//
//                                inited = true;
//
//                                if (!waitingEventTasks.isEmpty()) {
//                                    for (EventTask task : waitingEventTasks) {
//                                        hsab.logEvent(task.eventName, task.eventType, task.eventSrc, task.params);
//                                    }
//                                    waitingEventTasks.clear();
//                                }
//                            } catch (Exception ignored) {
//                            }
//                        }
//                    });
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            } else {
//                callback.onResult(false);
//            }
//        } else {
//            inited = true;
//            eventParams.clear();
//            waitingEventTasks.clear();
//        }
//    }
//
//    public void setUserInfo(String key, Object value) {
//        if (inited && hsab != null) {
//            hsab.setUserInfo(key, value);
//        } else {
//            try {
//                userProperties.put(key, value);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public void triggerAds(boolean country, boolean mediaSource, boolean timeout) {
//        if (adsInited) return;
//
//        if (country) this.country = true;
//        if (mediaSource) this.mediaSource = true;
//
//        if (timeout || (this.country && this.mediaSource)) {
//            adsInited = true;
//            // Kotlin coroutine part skipped. Can implement async wait in Java if needed.
//        }
//    }
//
//    public void setHeaderInfo(String key, Object value) {
//        if (inited && hsab != null) {
//            hsab.setHeaderInfo(key, value);
//        } else {
//            eventParams.put(key, value);
//        }
//    }
//
//    public void loadAbTestConfigs(IAB.Callback callback) {
//        if (hsab != null) {
//            hsab.loadAbTestConfigs(callback::onResult);
//        }
//    }
//
//    public String getSSID() {
//        return hsab != null ? hsab.getSSID() : "";
//    }
//
//    public String getAllAbConfigs() {
//        return hsab != null ? hsab.getAllAbConfigs() : "{}";
//    }
//
//    public String getAbConfigString(String key, String defaultValue) {
//        return hsab != null ? hsab.getAbConfigString(key, defaultValue) : defaultValue;
//    }
//
//    public int getAbConfigInt(String key, int defaultValue) {
//        return hsab != null ? hsab.getAbConfigInt(key, defaultValue) : defaultValue;
//    }
//
//    public long getAbConfigLong(String key, long defaultValue) {
//        return hsab != null ? hsab.getAbConfigLong(key, defaultValue) : defaultValue;
//    }
//
//    public float getAbConfigFloat(String key, float defaultValue) {
//        return hsab != null ? hsab.getAbConfigFloat(key, defaultValue) : defaultValue;
//    }
//
//    public double getAbConfigDouble(String key, double defaultValue) {
//        return hsab != null ? hsab.getAbConfigDouble(key, defaultValue) : defaultValue;
//    }
//
//    public boolean getAbConfigBoolean(String key, boolean defaultValue) {
//        return hsab != null ? hsab.getAbConfigBoolean(key, defaultValue) : defaultValue;
//    }
//
//    public void logEvent(String event, String eventType, String eventSrc, Map<String, Object> params) {
//        if (inited && hsab != null) {
//            if (!isAbVersionSet) {
//                String version = hsab.getAbSdkVersion();
//                if (version != null && !version.isEmpty() && !version.isBlank()) {
//                    IvyTrack.getInstance().setUserProperty("ab_version", version);
//                    isAbVersionSet = true;
//                }
//            }
//            hsab.logEvent(event, eventType, eventSrc, params);
//        } else {
//            waitingEventTasks.add(new EventTask(event, eventType, eventSrc, params));
//        }
//    }
//
//}
