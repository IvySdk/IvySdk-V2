package com.ivy.track;

import android.content.Context;
import android.os.SystemClock;
import android.util.Base64;

import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.http.HttpUtil;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;
import com.ivy.base.track.AbsTrack;
import com.ivy.base.track.EventIDs;
import com.ivy.base.track.EventParams;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;
import com.ivy.base.track.IConversationCallback;
import com.ivy.base.track.IKeys;
import com.ivy.base.track.ITrack;
import com.ivy.base.track.TrackPlatform;
import com.ivy.base.util.IvyUtil;
import com.ivy.base.util.Util;
import com.ivy.track.property.PropertyUtil;
import com.ivy.track.reframe.ReframeUtil;
import com.ivy.track.reframe.Result;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class IvyTrack implements ITrack {

    // 统计平台
    private final Map<TrackPlatform, AbsTrack> trackPlatforms = new HashMap<>();

    // 事件流向
    private EventChannelUtil eventChannelUtil;

    private final List<EventTask> waitingEventTasks = Collections.synchronizedList(new ArrayList<>());
    private final List<UserProperty> userProperties = Collections.synchronizedList(new ArrayList<>());

    // 测试模式
    private boolean debug = false;
    private boolean lowDevice = false;

    // 首次运行时间戳
    private long firstOpenTime = 0L;
    private boolean alreadyReceivedAfConversion = false;

    private final AtomicBoolean isTrackPlatformsReady = new AtomicBoolean(false);
    private final AtomicBoolean deRecordEventTag = new AtomicBoolean(false);

    private final HashMap<String, Boolean> lowDeviceEvents = new HashMap<>();

    private static final String TAG = "IvyTrack";
    //    public static final long ONE_DAY_MILLISECONDS = 24 * 60 * 60 * 1000;
    private String last_checked_event = "";
    private long last_checked_timestamp = SystemClock.elapsedRealtime();

    private static IvyTrack _instance = null;

    public static IvyTrack getInstance() {
        if (_instance == null) {
            synchronized (IvyTrack.class) {
                if (_instance == null) {
                    _instance = new IvyTrack();
                }
            }
        }
        return _instance;
    }

    private IvyTrack() {
        // 初始化 trackPlatforms
        for (TrackPlatform tp : TrackPlatform.values()) {
            trackPlatforms.put(tp, null);
        }
    }

    @Override
    public void setup(Context context, String appId, String config, boolean debug, IConversationCallback conversationCallback, Runnable readyListener) {
        this.debug = debug;
        setUserProperty(EventParams.EVENT_PARAM_ROLE_ID, Util.roleId());
        PropertyUtil.getInstance();
        setupFirstOpenTime();

        try {
            JSONObject base = new JSONObject(config);
            lowDevice = LocalStorage.Instance.decodeString("low_device", "no").equals("yes");
            if (lowDevice) {
                lowDeviceEvents.put(EventIDs.SDK_FIRST_APP_OPEN, true);
                lowDeviceEvents.put(EventIDs.EVENT_APP_OPEN, true);
                lowDeviceEvents.put(EventIDs.PLATFORM_INIT_START, true);
                lowDeviceEvents.put(EventIDs.IAP_PURCHASED, true);
                lowDeviceEvents.put(EventIDs.IAP_PRE_ORDER, true);
                lowDeviceEvents.put(EventIDs.IAP_VERIFICATION, true);
                lowDeviceEvents.put(EventIDs.IAP_USER_CANCEL, true);
                JSONArray arr = base.optJSONArray("low_events");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); ++i) {
                        lowDeviceEvents.put(arr.getString(i), true);
                    }
                }
                setUserProperty("low_device", 1);
            } else {
                setUserProperty("low_device", 0);
            }

            JSONObject custom = base.optJSONObject("custom");
            if (custom != null) {
                ReframeUtil.getInstance().setup(custom);
            } else {
                ILog.w(TAG, "no reframe config find!!!");
            }

            JSONObject orient = base.optJSONObject("orient");
            if (orient != null) {
                if (eventChannelUtil == null) eventChannelUtil = new EventChannelUtil();
                eventChannelUtil.setup(orient);
            } else {
                ILog.e(TAG, "load event channel failed!!!");
            }

            JSONObject platform = base.optJSONObject("platform");
            if (platform != null) {
                setupTrackPlatforms(platform, appId, conversationCallback, readyListener);
                ILog.d(TAG, "build track platform end");
            } else {
                ILog.e(TAG, "load track platform failed!!!");
            }

            // 打点设备信息
            loadDeviceInfo(appId);

        } catch (Exception e) {
            ILog.e(TAG, "setup track failed:" + e.getMessage());
            // e.printStackTrace();
        }
    }

    private void setupTrackPlatforms(JSONObject json, String appId, IConversationCallback conversationCallback, Runnable readyListener) {
        Map<TrackPlatform, AbsTrack> map = buildTrackPlatforms(json);
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.equalsIgnoreCase(TrackPlatform.FACEBOOK.getValue())) {
                AbsTrack absTrack = map.get(TrackPlatform.FACEBOOK);
                String fbConfig = json.optString(key);
                if (absTrack != null) {
                    absTrack.setup(App.getInstance(), appId, fbConfig, debug, null, null);
                    trackPlatforms.put(TrackPlatform.FACEBOOK, absTrack);
                    TestManager.getInstance().addEvent("Facebook", "打点 初始化成功", EventState.NORMAL, "");
                } else {
                    ILog.e(TAG, String.format("track platform %s init failed", TrackPlatform.FACEBOOK.getValue()));
                }
            } else if (key.equalsIgnoreCase(TrackPlatform.FIREBASE.getValue())) {
                AbsTrack absTrack = map.get(TrackPlatform.FIREBASE);
                String fbConfig2 = json.optString(key);
                if (absTrack != null) {
                    absTrack.setup(App.getInstance(), appId, fbConfig2, debug, null, null);
                    trackPlatforms.put(TrackPlatform.FIREBASE, absTrack);
                    TestManager.getInstance().addEvent("Firebase", "打点初始化成功", EventState.NORMAL, "");
                } else {
                    ILog.e(TAG, String.format("track platform %s init failed", TrackPlatform.FIREBASE.getValue()));
                }
            } else if (key.equalsIgnoreCase(TrackPlatform.APPSFLYER.getValue())) {
                AbsTrack absTrack = map.get(TrackPlatform.APPSFLYER);
                String afConfig = json.optString(key);
                if (absTrack != null) {
                    TestManager.getInstance().addEvent("Appsflyer", "打点开始初始化", EventState.NORMAL, "");
                    absTrack.setup(App.getInstance(), appId, afConfig, debug, new IConversationCallback() {
                        @Override
                        public void onConversionDataSuccess(Map<String, Object> data) {
                            TestManager.getInstance().addEvent("Appsflyer", "归因成功", EventState.NORMAL, data);
                            String afId = getAppsflyerId();
                            if (afId != null) {
                                setUserProperty("appsflyer_id", afId);
                                LocalStorage.Instance.encodeString(IKeys.KEY_AF_ID, afId);
                            }
                            if (alreadyReceivedAfConversion) {
                                return;
                            }
                            alreadyReceivedAfConversion = true;
                            try {
                                String status = Objects.requireNonNull(data.get("af_status")).toString();
                                if (!status.equalsIgnoreCase("Organic")) {
                                    Map<String, Object> map = formatAppsflyerConversionData(data);
                                    if (!map.isEmpty())
                                        IvyTrack.getInstance().logEvent(EventIDs.SDK_FIRST_AF_CONVERSION, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, map);
                                } else {
//                                    JSONObject _json = new JSONObject();
//                                    _json.put("media_source", "organic");
//                                    LocalStorage.Instance.encodeString("cache_install_data", _json.toString());
//                                    setUserProperty("af_campaign", "Organic");
                                    LocalStorage.Instance.encodeString(IKeys.KEY_AF_MEDIA_SOURCE, "Organic");
                                }
                            } catch (Exception ignore) {

                            }
                            if (conversationCallback != null) {
                                conversationCallback.onConversionDataSuccess(data);
                            }

                        }

                        @Override
                        public void onConversionDataFail(String error) {
                            TestManager.getInstance().addEvent("Appsflyer", "归因失败", EventState.WARNING, "");
                            if (conversationCallback != null) {
                                conversationCallback.onConversionDataFail(error);
                            }
                        }

                        @Override
                        public void onAppOpenAttribution(Map<String, String> attribution) {
                            Map<String, Object> map = new HashMap<>();
                            if (attribution != null) {
                                map.putAll(attribution);
                            }
                            IvyTrack.getInstance().logEvent(EventIDs.SDK_FIRST_AF_APP_OPEN_ATTRIBUTION, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, map);
                            if (conversationCallback != null) {
                                conversationCallback.onAppOpenAttribution(attribution);
                            }
                        }

                        @Override
                        public void onAttributionFailure(String error) {
                            if (conversationCallback != null) {
                                conversationCallback.onAttributionFailure(error);
                            }
                        }
                    }, () -> {
                        TestManager.getInstance().addEvent("Appsflyer", "打点初始化成功", EventState.NORMAL, "");
                    });
                    trackPlatforms.put(TrackPlatform.APPSFLYER, absTrack);
                    String afId = getAppsflyerId();
                    if (afId != null) {
                        setUserProperty("appsflyer_id", afId);
                        LocalStorage.Instance.encodeString(IKeys.KEY_AF_ID, afId);
                    }
                } else {
                    ILog.e(TAG, String.format("track platform %s init failed", TrackPlatform.APPSFLYER.getValue()));
                }
            } else if (key.equalsIgnoreCase(TrackPlatform.THINKING_DATA.getValue())) {
                AbsTrack absTrack = map.get(TrackPlatform.THINKING_DATA);
                String tdConfig = json.optString(key);
                if (absTrack != null) {
                    absTrack.setup(App.getInstance(), appId, tdConfig, debug, null, null);
                    trackPlatforms.put(TrackPlatform.THINKING_DATA, absTrack);
                    TestManager.getInstance().addEvent("IVY", "打点初始化成功", EventState.NORMAL, "");
                } else {
                    ILog.e(TAG, String.format("track platform %s init failed", TrackPlatform.THINKING_DATA.getValue()));
                }
            } else if (key.equalsIgnoreCase(TrackPlatform.AXON.getValue())) {
                AbsTrack absTrack = map.get(TrackPlatform.AXON);
                String tdConfig = json.optString(key);
                if (absTrack != null) {
                    absTrack.setup(App.getInstance(), appId, tdConfig, debug, null, null);
                    trackPlatforms.put(TrackPlatform.AXON, absTrack);
                    TestManager.getInstance().addEvent("AXON", "打点初始化成功", EventState.NORMAL, "");
                } else {
                    ILog.e(TAG, String.format("track platform %s init failed", TrackPlatform.AXON.getValue()));
                }
            } else if (key.equalsIgnoreCase(TrackPlatform.PANGLE.getValue())) {
                AbsTrack absTrack = map.get(TrackPlatform.PANGLE);
                String tdConfig = json.optString(key);
                if (absTrack != null) {
                    absTrack.setup(App.getInstance(), appId, tdConfig, debug, null, null);
                    trackPlatforms.put(TrackPlatform.PANGLE, absTrack);
                    TestManager.getInstance().addEvent("Pangle", "打点初始化成功", EventState.NORMAL, "");
                } else {
                    ILog.e(TAG, String.format("track platform %s init failed", TrackPlatform.PANGLE.getValue()));
                }
            }
        }
        isTrackPlatformsReady.set(true);
        if (readyListener != null) readyListener.run();
    }

    private Map<TrackPlatform, AbsTrack> buildTrackPlatforms(JSONObject json) {
        Iterator<String> keys = json.keys();
        Map<TrackPlatform, AbsTrack> map = new HashMap<>();
        while (keys.hasNext()) {
            String key = keys.next();
            if (lowDevice && !key.equalsIgnoreCase(TrackPlatform.APPSFLYER.getValue())) {
                continue;
            }
            if (key.equalsIgnoreCase(TrackPlatform.FACEBOOK.getValue())) {
                AbsTrack absTrack = TrackPlatformUtil.instantiateTrackPlatform(TrackPlatform.FACEBOOK);
                if (absTrack != null) {
                    map.put(TrackPlatform.FACEBOOK, absTrack);
                }
            } else if (key.equalsIgnoreCase(TrackPlatform.FIREBASE.getValue())) {
                AbsTrack absTrack = TrackPlatformUtil.instantiateTrackPlatform(TrackPlatform.FIREBASE);
                if (absTrack != null) {
                    map.put(TrackPlatform.FIREBASE, absTrack);
                }
            } else if (key.equalsIgnoreCase(TrackPlatform.APPSFLYER.getValue())) {
                AbsTrack absTrack = TrackPlatformUtil.instantiateTrackPlatform(TrackPlatform.APPSFLYER);
                if (absTrack != null) {
                    map.put(TrackPlatform.APPSFLYER, absTrack);
                }
            } else if (key.equalsIgnoreCase(TrackPlatform.THINKING_DATA.getValue())) {
                AbsTrack absTrack = TrackPlatformUtil.instantiateTrackPlatform(TrackPlatform.THINKING_DATA);
                if (absTrack != null) {
                    map.put(TrackPlatform.THINKING_DATA, absTrack);
                }
            } else if (key.equalsIgnoreCase(TrackPlatform.AXON.getValue())) {
                AbsTrack absTrack = TrackPlatformUtil.instantiateTrackPlatform(TrackPlatform.AXON);
                if (absTrack != null) {
                    map.put(TrackPlatform.AXON, absTrack);
                }
            } else if (key.equalsIgnoreCase(TrackPlatform.PANGLE.getValue())) {
                AbsTrack absTrack = TrackPlatformUtil.instantiateTrackPlatform(TrackPlatform.PANGLE);
                if (absTrack != null) {
                    map.put(TrackPlatform.PANGLE, absTrack);
                }
            }
        }
        return map;
    }

    private Map<String, Object> formatAppsflyerConversionData(Map<String, Object> map) throws Exception {
        JSONObject json = new JSONObject();
        Map<String, Object> map2 = new HashMap<>();

        Object mediaSource = map.get("media_source");
        if (mediaSource != null) {
            String it = mediaSource.toString();
            setUserProperty("media_source", it, TrackPlatform.THINKING_DATA);
            LocalStorage.Instance.encodeString(IKeys.KEY_AF_MEDIA_SOURCE, it);
            json.put("media_source", it);
            map2.put("media_source", it);
        } else {
            json.put("media_source", "");
        }

        Object campaign = map.get("campaign");
        if (campaign != null) {
            String it = campaign.toString();
//            setUserProperty("campaign_name", it, TrackPlatform.THINKING_DATA);
            LocalStorage.Instance.encodeString(IKeys.KEY_AF_CAMPAIGN, it);
            json.put("campaign_name", it);
            map2.put("campaign_name", it);
        } else {
            json.put("campaign_name", "");
        }

        Object campaignId = map.get("af_c_id");
        if (campaignId != null) {
            String it = campaignId.toString();
//            setUserProperty("campaign_id", it, TrackPlatform.THINKING_DATA);
            LocalStorage.Instance.encodeString(IKeys.KEY_AF_CAMPAIGN_ID, it);
            json.put("campaign_id", it);
            map2.put("campaign_id", it);
        } else {
            json.put("campaign_id", "");
        }

        Object siteId = map.get("af_siteid");
        if (siteId != null) {
            String it = siteId.toString();
//            setUserProperty("siteid", it, TrackPlatform.THINKING_DATA);
            json.put("siteid", it);
            map2.put("siteid", it);
        } else {
            json.put("siteid", "");
        }

        Object adType = map.get("af_ad_type");
        if (adType != null) {
            String it = adType.toString();
//            setUserProperty("ad_type", it, TrackPlatform.THINKING_DATA);
            json.put("ad_type", it);
            map2.put("ad_type", it);
        } else {
            json.put("ad_type", "");
        }

        Object agency = map.get("agency");
        if (agency != null) {
            String it = agency.toString();
//            setUserProperty("media_channel", it, TrackPlatform.THINKING_DATA);
            json.put("media_channel", it);
            map2.put("media_channel", it);
        } else {
            json.put("media_channel", "");
        }

        Object ad = map.get("af_ad");
        if (ad != null) {
            String it = ad.toString();
//            setUserProperty("ad_name", it, TrackPlatform.THINKING_DATA);
            json.put("ad_name", it);
            map2.put("ad_name", it);
        } else {
            json.put("ad_name", "");
        }

        Object adgroupId = map.get("adgroup_id");
        if (adgroupId != null) {
            String it = adgroupId.toString();
//            setUserProperty("ad_id", it, TrackPlatform.THINKING_DATA);
            json.put("ad_id", it);
            map2.put("ad_id", it);
        } else {
            json.put("ad_id", "");
        }

        Object adset = map.get("adset");
        if (adset != null) {
            String it = adset.toString();
//            setUserProperty("adset_name", it, TrackPlatform.THINKING_DATA);
            LocalStorage.Instance.encodeString(IKeys.KEY_AF_AD_SET, it);
            json.put("adset_name", it);
            map2.put("adset_name", it);
        } else {
            json.put("adset_name", "");
        }

        Object adsetId = map.get("adset_id");
        if (adsetId != null) {
            String it = adsetId.toString();
//            setUserProperty("adset_id", it, TrackPlatform.THINKING_DATA);
            json.put("adset_id", it);
            map2.put("adset_id", it);
        } else {
            json.put("adset_id", "");
        }
//        LocalStorage.Instance.encodeString("cache_install_data", json.toString());
        return map2;
    }

    private boolean isReady() {
        return isTrackPlatformsReady.get();
    }

    private List<TrackPlatform> checkEventPlatform(String eventName) {
        return eventChannelUtil != null ? eventChannelUtil.getTrackPlatform(eventName) : null;
    }

    @Override
    public void setUserProperty(String key, Object value) {
        if (value instanceof Boolean) {
            if (debug) {
                throw new RuntimeException("user property not allow data type as bool!!!");
            } else {
                ILog.e(TAG, key + " is bool!!! not allow");
            }
            return;
        }
        if (!isReady()) {
            userProperties.add(new UserProperty(key, value, TrackPlatform.NONE));
            return;
        }
        for (Map.Entry<TrackPlatform, AbsTrack> entry : trackPlatforms.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().setUserProperty(key.toLowerCase(), value);
            }
        }
    }

    public void setUserProperty(String key, String value, TrackPlatform platform) {
        if ("customer_user_id".equals(key)) {
            LocalStorage.Instance.encodeString(IKeys.KEY_CUSTOM_USER_ID, value);
        }
        if (!isReady()) {
            ILog.i(TAG, "set user property:\n" + key + "=" + value + "; platform=" + (platform == null ? "all" : platform.getValue()) + "\n ; waiting ready;");
            userProperties.add(new UserProperty(key, value, platform));
            return;
        }
        ILog.i(TAG, "set user property:\n" + key + "=" + value + "; platform=" + (platform == null ? "all" : platform.getValue()));
        AbsTrack track = trackPlatforms.get(platform);
        if (track != null) {
            track.setUserProperty(key.toLowerCase(), value);
        }
    }

    private void resetUserProperties() {
        if (userProperties.isEmpty()) {
            return;
        }
        try {
            Iterator<UserProperty> iter = userProperties.iterator();
            while (iter.hasNext()) {
                UserProperty item = iter.next();
                TrackPlatform platform = item.getPlatform();
                if (platform == TrackPlatform.NONE) {
                    for (Map.Entry<TrackPlatform, AbsTrack> entry : trackPlatforms.entrySet()) {
                        AbsTrack track = entry.getValue();
                        if (track != null) {
                            track.setUserProperty(item.getKey().toLowerCase(), item.getValue());
                        }
                    }
                } else {
                    AbsTrack track = trackPlatforms.get(platform);
                    if (track != null) {
                        track.setUserProperty(item.getKey().toLowerCase(), item.getValue());
                    }
                }
                iter.remove();
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }

    @Override
    public void appsflyerInviteUser(String channel, String campaign, String inviterId, String inviterAppId) {
        AbsTrack appsflyerTrack = trackPlatforms.get(TrackPlatform.APPSFLYER);
        if (appsflyerTrack != null) {
            appsflyerTrack.appsflyerInviteUser(channel, campaign, inviterId, inviterAppId);
        }
    }

    @Override
    public String getAppsflyerInviterId() {
        AbsTrack appsflyerTrack = trackPlatforms.get(TrackPlatform.APPSFLYER);
        if (appsflyerTrack != null) {
            return appsflyerTrack.getAppsflyerInviterId();
        }
        return null;
    }

    @Override
    public String getAppsflyerId() {
        AbsTrack appsflyerTrack = trackPlatforms.get(TrackPlatform.APPSFLYER);
        if (appsflyerTrack != null) {
            return appsflyerTrack.getAppsflyerId();
        }
        return null;
    }


    @Override
    public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms) {
//        if (eventName.equalsIgnoreCase("sdk_app_open") || eventName.equalsIgnoreCase("sdk_first_open") || eventName.equalsIgnoreCase("sdk_test_user")) {

        if (lowDevice && !lowDeviceEvents.containsKey(eventName)) {
            return;
        }

        if (isReady()) {
            deRecordEvent();
            Map<String, Object> lowerParams = lowerEventParams(params);
            doLogEvent(eventName, eventType, eventSrc, lowerParams, platforms, true);

        } else {
            recordEvent(eventName, eventType, eventSrc, params, platforms);
        }
//        }
    }

    private void recordEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms) {
        try {
            ILog.i(TAG, "start record event:" + eventName);
            EventTask task = new EventTask(eventName, eventType, eventSrc, params, platforms);
            waitingEventTasks.add(task);
            ILog.i(TAG, "record event:" + eventName);
        } catch (Exception ignored) {
        }
    }

    private void deRecordEvent() {
        try {
            if (deRecordEventTag.compareAndSet(false, true)) {
                resetUserProperties();
                if (waitingEventTasks.isEmpty()) {
                    ILog.i(TAG, "de-record event; empty list");
                    return;
                }
                Iterator<EventTask> it = waitingEventTasks.iterator();
                while (it.hasNext()) {
                    EventTask eventTask = it.next();
                    ILog.i(TAG, "de-record event:" + eventTask.getEventName());
                    doLogEvent(eventTask.getEventName(), eventTask.getEventType(), eventTask.getEventSrc(), lowerEventParams(eventTask.getParams()), eventTask.getPlatforms(), true);
                    it.remove();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void doLogEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms, boolean checkCustomizeEvent) {
        // 检测事件流向并打点, 优先使用配置文件中的控制条件
        ILog.i(TAG, "send event:" + eventName);
        if (params == null) {
            params = new HashMap<>();
        }
        params.put(EventParams.EVENT_PARAM_EVENT_SRC, eventSrc);
        params.put(EventParams.EVENT_PARAM_ROLE_ID, Util.roleId());
        List<TrackPlatform> plats = checkEventPlatform(eventName);
        if (plats != null) {
            // 配置文件中的流向
            for (TrackPlatform item : plats) {
                AbsTrack _track = trackPlatforms.get(item);
                if (_track != null) {
                    _track.logEvent(eventName, eventType, eventSrc, appendProperties(item, params));
                }
            }
        } else {
            if (platforms != null) {
                // 打点指定的流向
                for (TrackPlatform item : platforms) {
                    AbsTrack _track = trackPlatforms.get(item);
                    if (_track != null) {
                        _track.logEvent(eventName, eventType, eventSrc, appendProperties(item, params));
                    }
                }
            } else {
                // 默认流向所有平台
                // appsflyer 和 ThinkData 互斥， 优先流向 appsflyer
                boolean afAndTdExist = trackPlatforms.get(TrackPlatform.APPSFLYER) != null && trackPlatforms.get(TrackPlatform.THINKING_DATA) != null;
                for (Map.Entry<TrackPlatform, AbsTrack> trackEntry : trackPlatforms.entrySet()) {
                    if (afAndTdExist && trackEntry.getKey() == TrackPlatform.THINKING_DATA) {
                        continue;
                    }
                    AbsTrack _track = trackEntry.getValue();
                    if (_track != null) {
                        _track.logEvent(eventName, eventType, eventSrc, appendProperties(trackEntry.getKey(), params));
                    }
                }
            }
        }

        if (EventSrc.EVENT_SRC_COMBINATION.equals(eventSrc)) {
            // 组合事件，不再重复组合
            return;
        }

        if (checkCustomizeEvent) {
            try {
                if (last_checked_event.equals(eventName) && SystemClock.elapsedRealtime() - last_checked_timestamp < 1000) {
                    return;
                }
                if (Objects.equals(eventSrc, EventSrc.EVENT_SRC_CLIENT)) {
                    last_checked_event = eventName;
                    last_checked_timestamp = SystemClock.elapsedRealtime();
                } else {
                    last_checked_event = "";
                }
                boolean mock = params.containsKey("mock");
                List<Result> resultList = ReframeUtil.getInstance().check(eventName, params);
                if (resultList != null && !resultList.isEmpty()) {
                    for (Result item : resultList) {
                        String newEventName = item.str;
                        ILog.i(TAG, "build reframe event:" + newEventName);
                        if (newEventName != null) {
                            if (newEventName.toLowerCase().startsWith("in_app_purchase")) {
                                try {
                                    String _value = newEventName.replace("in_app_purchase_", "").replace("_", ".");
                                    double value = Double.parseDouble(_value);
                                    Map<String, Object> map = new HashMap<>() {{
                                        put("currency", "USD");
                                        put("value", value);
                                    }};
                                    if (mock) {
                                        map.put("mock", true);
                                    }
                                    logEvent("in_app_purchase", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_COMBINATION, map, List.of(TrackPlatform.FIREBASE));
                                } catch (Exception ignored) {
                                }
                            } else if (newEventName.toLowerCase().startsWith("fb_mobile_purchase")) {
                                try {
                                    String _value = newEventName.replace("fb_mobile_purchase_", "").replace("_", ".");
                                    double price_amount = Double.parseDouble(_value);
                                    Map<String, Object> map = new HashMap<>() {{
                                        put("currency", "USD");
                                        put("price_amount", price_amount);
                                        put(EventParams.EVENT_PARAM_PAY_STATE, 3);
                                    }};
                                    if (mock) {
                                        map.put("mock", true);
                                    }
                                    logEvent("fb_mobile_purchase", EventType.EVENT_TYPE_PURCHASE, EventSrc.EVENT_SRC_COMBINATION, map, List.of(TrackPlatform.FACEBOOK));
                                } catch (Exception ignored) {
                                }
                            } else if (newEventName.toLowerCase().startsWith(EventIDs.EVENT_PUSH_REWARDED)) {
                                //String userId = getUserId();
                                // reportPushReward(userId, "virtual_" + userId, null, "2", "1", false);
                            } else {
                                Map<String, Object> p = new HashMap<>();
                                p.put("value", item.value);
                                p.put("currency", "USD");
                                if (mock) {
                                    p.put("mock", true);
                                }
                                logEvent(newEventName, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_COMBINATION, p);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                ILog.e(TAG, "combine_event err:" + e.getMessage());
                //    e.printStackTrace();
            }
        }
    }

    private Map<String, Object> lowerEventParams(Map<String, Object> src) {
        if (src == null) {
            return null;
        }
        Map<String, Object> dest = new HashMap<>();
        for (Map.Entry<String, Object> entry : src.entrySet()) {
            if (entry.getKey() != null) {
                dest.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
            }
        }
        return dest;
    }

    private Map<String, Object> appendProperties(TrackPlatform item, Map<String, Object> src) {
        if (item == TrackPlatform.APPSFLYER || item == TrackPlatform.THINKING_DATA) {
            Map<String, Object> tmp = new HashMap<>();

            Map<String, Object> commons = PropertyUtil.getInstance().loadProperties();
            for (Map.Entry<String, Object> entry : commons.entrySet()) {
                if (!tmp.containsKey(entry.getKey())) {
                    tmp.put(entry.getKey(), entry.getValue());
                }
            }
            if (item == TrackPlatform.APPSFLYER) {
                if (src != null) {
                    tmp.putAll(src);
                }
            } else if (item == TrackPlatform.THINKING_DATA) {
                if (src != null) {
                    tmp.put("extra", src);
                }
            }
            return tmp;
        }
        return src;
    }

    private void setupFirstOpenTime() {
        if (LocalStorage.Instance.containsKey(IKeys.KEY_FIRST_OPEN_TIME)) {
            this.firstOpenTime = LocalStorage.Instance.decodeLong(IKeys.KEY_FIRST_OPEN_TIME, System.currentTimeMillis());
        } else {
            this.firstOpenTime = System.currentTimeMillis();
            LocalStorage.Instance.encodeLong(IKeys.KEY_FIRST_OPEN_TIME, this.firstOpenTime);
        }
    }

    public long getFirstOpenTime() {
        if (firstOpenTime == 0L) {
            setupFirstOpenTime();
        }
        return this.firstOpenTime;
    }

    private void loadDeviceInfo(String appId) {
        try {
            JSONObject json = IvyUtil.loadDeviceInfoJson();
            if (json == null) return;
            json.put("app_id", appId);
            json.put("event_name", EventIDs.DEVICE_INFO);
            new Thread(() -> {
                try {
                    String rawMessage = json.toString();
                    ByteArrayOutputStream os = new ByteArrayOutputStream(rawMessage.getBytes().length);
                    GZIPOutputStream gos = new GZIPOutputStream(os);
                    gos.write(rawMessage.getBytes());
                    gos.close();
                    byte[] compressed = os.toByteArray();
                    os.close();

                    String params = new String(Base64.encode(compressed, Base64.DEFAULT));
                    okhttp3.OkHttpClient okHttpClient = HttpUtil.getInstance().getOkHttpClient();
                    MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                    RequestBody postBody = RequestBody.create(params, mediaType);
                    Request request = new Request.Builder().url("https://func.ivymobile.com/dimension/table/update").addHeader("Content-Encoding", "gzip").post(postBody).build();
                    Response response = okHttpClient.newCall(request).execute();
                    if (response.isSuccessful()) {
                        ILog.i(TAG, "upload device info success");
                    } else {
                        ILog.w(TAG, "upload device info failed");
                    }
                } catch (Exception ignored) {
                }
            }).start();
        } catch (Exception e) {
            //  e.printStackTrace();
        }
    }

    private String getUserId() {
        String ssid = LocalStorage.Instance.decodeString("appsflyer_id", "");
        if (ssid.isEmpty()) {
            return Util.roleId();
        }
        return ssid;
    }


}
