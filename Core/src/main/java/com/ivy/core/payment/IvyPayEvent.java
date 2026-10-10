package com.ivy.core.payment;

import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;
import com.ivy.base.track.TrackPlatform;
import com.ivy.base.util.Util;
import com.ivy.base.util.rx.Action;
import com.ivy.track.IvyTrack;

import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class IvyPayEvent {

    private final AtomicBoolean runningState = new AtomicBoolean(false);
    private final AtomicBoolean taskPreparerState = new AtomicBoolean(false);
    private final AtomicBoolean taskRunningState = new AtomicBoolean(false);
    private final Map<String, Integer> events = new ConcurrentHashMap<>();
    private ScheduledExecutorService executor;
    private final String fileName = "payment_events.dat";

    public IvyPayEvent() {
        init();
    }

    public void init() {
        taskPreparerState.set(false);
        String file_path = App.getInstance().getCacheDir().getAbsolutePath() + File.separator + fileName;
        Util.loadCachedFileData(file_path, new Action<String>() {
            @Override
            public void call(String cachedData) {
                if (cachedData != null) {
                    try {
                        JSONObject json = new JSONObject(cachedData);
                        Iterator<String> iterator = json.keys();
                        while (iterator.hasNext()) {
                            String key = iterator.next();
                            int count = json.optInt(key, 0);
                            if (count > 0) {
                                events.put(key, count);
                            }
                        }
                    } catch (Exception e) {
                        ILog.e("pay_event", "load cached events data failed:" + e.getMessage());
                    }
                } else {
                    ILog.i("pay_event", "no cached pay events");
                }
            }
        });
        taskPreparerState.set(true);
    }

    public void update(String event, int count) {
        ILog.i("pay_event", "update event:" + event + ", count:" + count);
        taskPreparerState.set(false);
        int preCount = 0;
        if (events.containsKey(event)) {
            preCount = events.get(event);
        }
        int eventCount = count + preCount;
        if (eventCount <= 0) {
            ILog.i("pay_event", "skip event:" + event + ", count less than 0");
            taskPreparerState.set(true);
            return;
        }
        events.put(event, eventCount);
        taskPreparerState.set(true);
    }

    public void start() {
        ILog.e("pay_event", "start called");
        runningState.set(true);
        startTask();
    }

    public void destroy() {
        ILog.e("pay_event", "destroy called");
        runningState.set(false);
        taskPreparerState.set(false);
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    private void startTask() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(() -> {
            if (runningState.get() && taskPreparerState.get() && !taskRunningState.get()) {
                taskRunningState.set(true);
                try {
                    JSONObject json = new JSONObject();
                    for (Map.Entry<String, Integer> entry : events.entrySet()) {
                        try {
                            if (entry.getValue() > 0) {
                                IvyTrack.getInstance().logEvent(
                                        entry.getKey(),
                                        EventType.EVENT_TYPE_COMMON,
                                        EventSrc.EVENT_SRC_SDK,
                                        null,
                                        null
                                );

                                int count = entry.getValue() - 1;
                                ILog.i("pay_event", "log event:" + entry.getKey()
                                        + "; count:" + entry.getValue()
                                        + "; left count:" + count);

                                if (count > 0) {
                                    json.put(entry.getKey(), count);
                                    events.put(entry.getKey(), count);
                                } else {
                                    events.remove(entry.getKey());
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    Util.cacheDataToFile(fileName, json.toString());
                } catch (Exception ignored) {
                } finally {
                    taskRunningState.set(false);
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }
}
