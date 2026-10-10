package com.ivy.base.util.rx;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Schedulers {

    public static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Schedulers() {
    }

    public static Scheduler from(final Executor executor) {
        return new Scheduler() {
            @Override
            public void schedule(Runnable r) {
                executor.execute(r);
            }
        };
    }

    public static Scheduler io() {
        return new Scheduler() {
            @Override
            public void schedule(Runnable r) {
                executorService.execute(r);
            }
        };
    }

    public static Scheduler immediate() {
        return new Scheduler() {
            @Override
            public void schedule(Runnable r) {
                r.run();
            }
        };
    }



}