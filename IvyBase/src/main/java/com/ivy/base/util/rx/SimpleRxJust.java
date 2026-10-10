package com.ivy.base.util.rx;

public final class SimpleRxJust<T> extends SimpleRx<T> {

    private final T value;

    public SimpleRxJust(T value) {
        this.value = value;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        observer.onSuccess(value);
    }
}
