package com.ivy.base.test;

import android.os.Handler;
import android.os.Looper;

import com.ivy.base.test.data.Event;
import com.ivy.base.test.data.EventMonitor;

import java.util.ArrayList;
import java.util.List;

public class TraceManager {

    public interface EventListener {
        void onRefresh(List<Event> events);
    }

    private List<Event> events = new ArrayList<>();

    private EventListener listener = null;
    private Handler handler = new Handler(Looper.getMainLooper());

    public void addEvent(Event event) {
        events.add(event);
    }

    private Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            if (listener != null) {
                listener.onRefresh(events);
                handler.postDelayed(refreshTask, 1000);
            }
        }
    };

    public void setListener(EventListener l) {
        this.listener = l;
        handler.postDelayed(refreshTask, 1000);
    }

    public void clear() {
        listener = null;
        handler.removeCallbacks(refreshTask);
    }


}
