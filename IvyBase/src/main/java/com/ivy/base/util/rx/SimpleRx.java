package com.ivy.base.util.rx;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SimpleRx<T> {

    protected abstract void subscribeActual(Observer<? super T> observer);



    public final Disposable subscribe(final Observer<? super T> observer) {
        final AtomicBoolean disposed = new AtomicBoolean(false);

        subscribeActual(new Observer<T>() {
            @Override
            public void onSuccess(T value) {
                if (!disposed.get()) {
                    observer.onSuccess(value);
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!disposed.get()) {
                    observer.onError(e);
                }
            }
        });

        return new Disposable() {
            @Override
            public void dispose() {
                disposed.set(true);
            }

            @Override
            public boolean isDisposed() {
                return disposed.get();
            }
        };
    }

    // ---------- operators ----------

    public final <R> SimpleRx<R> map(final Func<T, R> mapper) {
        final SimpleRx<T> source = this;

        return new SimpleRx<R>() {
            @Override
            protected void subscribeActual(final Observer<? super R> observer) {
                source.subscribe(new Observer<T>() {
                    @Override
                    public void onSuccess(T value) {
                        try {
                            observer.onSuccess(mapper.call(value));
                        } catch (Throwable e) {
                            observer.onError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        observer.onError(e);
                    }
                });
            }
        };
    }

    public final SimpleRx<T> observeOn(final Scheduler scheduler) {
        final SimpleRx<T> source = this;

        return new SimpleRx<T>() {
            @Override
            protected void subscribeActual(final Observer<? super T> observer) {
                source.subscribe(new Observer<T>() {
                    @Override
                    public void onSuccess(final T value) {
                        scheduler.schedule(new Runnable() {
                            @Override
                            public void run() {
                                observer.onSuccess(value);
                            }
                        });
                    }

                    @Override
                    public void onError(final Throwable e) {
                        scheduler.schedule(new Runnable() {
                            @Override
                            public void run() {
                                observer.onError(e);
                            }
                        });
                    }
                });
            }
        };
    }

    public final SimpleRx<T> subscribeOn(final Scheduler scheduler) {
        final SimpleRx<T> source = this;

        return new SimpleRx<T>() {
            @Override
            protected void subscribeActual(final Observer<? super T> observer) {
                scheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        source.subscribe(observer);
                    }
                });
            }
        };
    }


}
