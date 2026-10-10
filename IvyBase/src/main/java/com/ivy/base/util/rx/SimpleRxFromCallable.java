package com.ivy.base.util.rx;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public final class SimpleRxFromCallable<T> extends SimpleRx<T> {

    private final Callable<T> task;
    private final Executor executor;

    public SimpleRxFromCallable(Callable<T> task, Executor executor) {
        this.task = task;
        this.executor = executor;
    }

    @Override
    protected void subscribeActual(final Observer<? super T> observer) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    observer.onSuccess(task.call());
                } catch (Throwable e) {
                    observer.onError(e);
                }
            }
        });
    }
}
