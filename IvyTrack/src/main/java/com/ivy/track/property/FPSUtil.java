package com.ivy.track.property;

import android.view.Choreographer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FPSUtil {

    private boolean isListening = false;
    private int fps = 60;
    private long lastFrameTimeNanos = 0;

    private final Choreographer.FrameCallback callback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (lastFrameTimeNanos != 0L) {
                long frameInterval = frameTimeNanos - lastFrameTimeNanos;
                fps = (int) (1000000000 / frameInterval);
            }
            lastFrameTimeNanos = frameTimeNanos;
            reListen();
        }
    };

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public void startListenFPS() {
        Choreographer.getInstance().postFrameCallback(callback);
        isListening = true;
    }

    public void stopListenFPS() {
        Choreographer.getInstance().removeFrameCallback(callback);
        isListening = false;
    }

    private void reListen() {
        executor.schedule(() -> {
            if (isListening) {
                Choreographer.getInstance().postFrameCallback(callback);
            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    public int getFPS() {
        return fps;
    }

    public void release() {
        executor.shutdown();
    }
}