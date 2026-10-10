package com.ivy.core.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.ivy.base.App;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;
import com.ivy.track.IvyTrack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {

    private SharedPreferences sp = null;
    private String current_session_id = null;
    private long start_time = 0;
    private long visible_time = 0;
    private boolean isForeground = false;
    private long session_interval;
    private long LOOP_INTERVAL = 5 * 1000;

    private final Handler handler = new Handler(Looper.getMainLooper());

    public SessionManager() {
        sp = App.getInstance().getSharedPreferences("_session_", Context.MODE_PRIVATE);
        session_interval = sp.getLong("session_interval", 5 * 60 * 1000);
    }

    /**
     * @param interval 毫秒
     */
    public void updateSessionInterval(long interval) {
        if (interval >= 30000) {
            sp.edit().putLong("session_interval", interval).apply();
            session_interval = interval;
        }
    }

    public String currentSessionId() {
        return current_session_id;
    }

    private void onSessionStart() {
        if (isForeground) {
            return;
        }
        isForeground = true;
        LOOP_INTERVAL = 5000;
        current_session_id = sp.getString("session_id", null);
        long last_stop_time = sp.getLong("last_stop_time", System.currentTimeMillis());
        long now = System.currentTimeMillis();
        if (current_session_id == null ||
                now < last_stop_time ||
                now - last_stop_time > session_interval) {
            current_session_id = UUID.randomUUID().toString();
            visible_time = 0;
            sp.edit().putString("session_id", current_session_id).remove("duration").apply();
        } else {
            visible_time = sp.getLong("duration", 0);
        }
        start_time = SystemClock.elapsedRealtime();
        handler.removeCallbacks(trackRunnable);
        handler.postDelayed(trackRunnable, LOOP_INTERVAL);
        try {
            IvyTrack.getInstance().setUserProperty("session_id", current_session_id);
        } catch (Throwable ignored) {
        }
//        System.out.println("Session Manager >>>> 会话开始, session id=" + current_session_id + "; start time=" + start_time + "; visible time=" + visible_time);
    }

    private void onSessionStop() {
        if (!isForeground)
            return;
        isForeground = false;
        updateVisibleTime();
        handler.removeCallbacks(trackRunnable);

//        System.out.println("Session Manager >>>> 会话结束, session id=" + current_session_id + "; visible time=" + visible_time);
    }

    private long updateVisibleTime() {
        if (start_time <= 0) {
            return 0;
        }

        long now = SystemClock.elapsedRealtime();
        long diff = (now - start_time);
        visible_time += diff;
        start_time = now;
        sp.edit().putLong("duration", visible_time).putLong("last_stop_time", System.currentTimeMillis()).apply();
        return diff;
    }

    private final Runnable trackRunnable = new Runnable() {
        @Override
        public void run() {
            if (isForeground) {
                long duration = updateVisibleTime();
                if (LOOP_INTERVAL < 30 * 1000) {
                    LOOP_INTERVAL += 5 * 1000;
                }
                handler.postDelayed(this, LOOP_INTERVAL);
                triggerUploadTask(duration);
            }
        }
    };

    private void triggerUploadTask(long duration) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("session_id", current_session_id);
            map.put("session_duration", duration);
            map.put("session_total_time", visible_time);
            //map.put("session_ts", TimeUtil.getInstance().getTimestamp());
            IvyTrack.getInstance().logEvent("sdk_session", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, map);
        } catch (Exception ignored) {
        }
    }

    private DefaultLifecycleObserver lifecycleObserver = new DefaultLifecycleObserver() {
        @Override
        public void onCreate(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onCreate(owner);
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onDestroy(owner);
        }

        @Override
        public void onPause(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onPause(owner);
        }

        @Override
        public void onResume(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onResume(owner);
        }

        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onStart(owner);
            onSessionStart();
        }

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onStop(owner);
            onSessionStop();
        }
    };

    public void start() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleObserver);
    }

    public void stop() {
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(lifecycleObserver);
    }


}
