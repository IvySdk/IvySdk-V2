package com.ivy.base.test;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import com.alibaba.fastjson.JSON;
import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.test.data.Event;
import com.ivy.base.test.data.EventMonitor;
import com.ivy.base.test.data.EventState;
import com.ivy.base.test.data.NecessaryMonitor;
import com.ivy.base.test.view.FloatingButton;
import com.ivy.base.test.view.TestActivity;
import com.ivy.base.util.Util;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TestManager {

    private static final String TEST_BX_ID_KEY = "_test_bx_id_";

    private static TestManager _instance = null;

    private boolean DEBUG = false;

    // 悬浮按钮
    private FloatingButton floatingButton;
    //    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());

    private EventMonitor common_event_monitor = new EventMonitor();

    private int event_seq = 0;


    private List<String> traceTags = new ArrayList<>();
    private TraceManager traceManager = null;
    private NecessaryMonitor necessaryMonitor = null;

    private FuncCallback callback = null;

    private boolean hasErrorInfo = false;
    private volatile boolean isAllNecessaryCollected = false;

    private Handler handler = new Handler(Looper.getMainLooper());


    public interface FuncCallback {
        void clearReframeRecord();

        void onEvent(String event, Bundle params);

        String getActivedAbInfo();

        boolean enterAbVariants(String varid);

        String getAllAbConfig();

        void auth(String platform, int action);

        void firebaseAuth(String platform, int action);

        void pay();

        void adTask(int taskId);

        void translate(String src);

        void onPlayGameStats(String eventId, JSONObject jsonObject);

        void updateAchievement(String achievementId, int step, boolean unlock);
    }

    private TestManager() {
    }

    public static TestManager getInstance() {
        if (_instance == null) {
            synchronized (TestManager.class) {
                if (_instance == null) {
                    _instance = new TestManager();
                }
            }
        }
        return _instance;
    }

    public void setup(boolean debug, FuncCallback c) {
        this.DEBUG = debug;
        if (debug) {
            Context context = App.getInstance().getApplicationContext();
            showFloatingButton(context);
            traceManager = new TraceManager();
            necessaryMonitor = new NecessaryMonitor() {
                @Override
                protected void onStateChanged(boolean state) {
                    super.onStateChanged(state);
                    isAllNecessaryCollected = state;
                }
            };
            this.callback = c;
        }
    }

    private void showFloatingButton(Context context) {
        WindowManager wm = ActivityUtil.getInstance().getActivity().getWindowManager();

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                (int) Util.dp2Px(context, 48),
                (int) Util.dp2Px(context, 48),
                WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.x = 0;
        params.y = 0;

        floatingButton = new FloatingButton(context) {
            @Override
            protected void onFloatClick() {
                Intent intent = new Intent(context, TestActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }
        };

        floatingButton.attach(wm, params);
    }

    public void destroy() {
        this.callback = null;
        endTrace();
        if (common_event_monitor != null) common_event_monitor.clear();
        common_event_monitor = null;
        if (traceManager != null) traceManager.clear();
        traceManager = null;
    }

    private void runOnUIThread(Runnable runnable) {
        try {
            handler.post(runnable);
        } catch (Throwable throwable) {
        }
    }

    public void addEvent(String tag, String info, EventState state, String data) {
        if (!DEBUG) return;
        if (info == null) return;
        if (data == null) data = "";
        String finalData = data;
        runOnUIThread(() -> {
            long timestamp = System.currentTimeMillis();
            String dateStr = sdf.format(new Date(timestamp));
            Event event = new Event(tag, event_seq++, info, state, finalData, timestamp, dateStr);
            common_event_monitor.addEvent(event);
            check(event);
        });
    }

    public void addEvent(String tag, String info, EventState state, Map<String, Object> params) {
        if (!DEBUG) return;
        if (info == null) return;
        runOnUIThread(() -> {
            String data = "";
            if (params != null) {
                data = JSON.toJSONString(params);
            }
            long timestamp = System.currentTimeMillis();
            String dateStr = sdf.format(new Date(timestamp));
            Event event = new Event(tag, event_seq++, info, state, data, timestamp, dateStr);
            common_event_monitor.addEvent(event);
            check(event);
        });
    }

    public void addEvent(String tag, String info, EventState state, Bundle params) {
        if (!DEBUG) return;
        if (info == null) return;
        runOnUIThread(() -> {
            String data = "";
            if (params != null) {
                data = bundleToJson(params);
            }
            long timestamp = System.currentTimeMillis();
            String dateStr = sdf.format(new Date(timestamp));
            Event event = new Event(tag, event_seq++, info, state, data, timestamp, dateStr);
            common_event_monitor.addEvent(event);
            check(event);
        });
    }

    private String bundleToJson(Bundle bundle) {
        if (bundle == null) return "{}";
        Map<String, Object> map = new HashMap<>();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            map.put(key, value);
        }
        return JSON.toJSONString(map, true);
    }

    public EventMonitor getCommonEventMonitor() {
        return common_event_monitor;
    }

    public void startTrace(String... tags) {
        if (!DEBUG) return;
        traceTags.clear();
        traceTags.addAll(Arrays.asList(tags));
        traceManager.clear();
        traceManager = new TraceManager();
    }

    public void endTrace() {
        traceTags.clear();
    }

    private void check(Event event) {
        if (event.state == EventState.ERROR)
            hasErrorInfo = true;

        if (necessaryMonitor != null) necessaryMonitor.check(event);

        if (floatingButton != null)
            floatingButton.update(hasErrorInfo || !isAllNecessaryCollected);

        if (!traceTags.isEmpty() && traceManager != null) {
            if (traceTags.contains(event.tag)) traceManager.addEvent(event);
        }
    }

    public TraceManager getCurrentTrace() {
        return traceManager;
    }

    public NecessaryMonitor getNecessaryMonitor() {
        return necessaryMonitor;
    }

    public void clearReframeRecord() {
        if (callback != null) {
            callback.clearReframeRecord();
        }
    }

    public void sendEvent(String event, Bundle params) {
        if (callback != null) {
            callback.onEvent(event, params);
        }
    }

    public String getActivedAbInfo() {
        if (callback != null) {
            return callback.getActivedAbInfo();
        }
        return null;
    }

    public boolean enterAbVariants(String varid) {
        return callback != null && callback.enterAbVariants(varid);
    }

    public String getAllAbConfig() {
        if (callback != null) {
            return callback.getAllAbConfig();
        }
        return null;
    }

    public void auth(String platform, int action) {
        if (callback != null) {
            callback.auth(platform, action);
        }
    }

    public void firebaseAuth(String channel, int action) {
        if (callback != null) {
            callback.firebaseAuth(channel, action);
        }
    }

    public void pay() {
        if (callback != null) {
            callback.pay();
        }
    }

    public void translate(String src) {
        if (callback != null) {
            callback.translate(src);
        }
    }

    public void adTask(int taskId) {
        if (callback != null) {
            callback.adTask(taskId);
        }
    }

    public void setTestBxId(String bxId) {
        if (!DEBUG) return;
        String value = bxId == null ? "" : bxId.trim();
        if (value.isEmpty()) {
            LocalStorage.Instance.removeValueForKey(TEST_BX_ID_KEY);
        } else {
            LocalStorage.Instance.encodeString(TEST_BX_ID_KEY, value);
        }
    }

    public String getTestBxId() {
        if (!DEBUG) return "";
        return LocalStorage.Instance.decodeString(TEST_BX_ID_KEY, "");
    }

    public String getCurrentBxId() {
        String testBxId = getTestBxId();
        if (!testBxId.isEmpty()) return testBxId;
        return DEBUG ? LocalStorage.Instance.decodeString("_bx_id_", "") : "";
    }

    public void onPlayGameStats(String eventId, JSONObject jsonObject) {
        if (callback != null) {
            callback.onPlayGameStats(eventId, jsonObject);
        }
    }

    public void updateAchievement(String achievementId, int step, boolean unlock) {
        TestManager.getInstance().addEvent("PlayGames", unlock ? "开始解锁成就:" + achievementId : "更新成就，step=" + step, EventState.NORMAL, "{}");
        if (callback != null) {
            callback.updateAchievement(achievementId, step, unlock);
        }
    }

}
