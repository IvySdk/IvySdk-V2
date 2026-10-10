package com.ivy.base.util.rx;

import android.os.Handler;
import android.os.Looper;

public final class RxAndroid {

    private static final Scheduler MAIN = new Scheduler() {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void schedule(Runnable r) {
            handler.post(r);
        }
    };

    public static Scheduler main() {
        return MAIN;
    }
}
