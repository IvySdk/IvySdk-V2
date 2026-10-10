package com.ivy.base.util.rx;

public interface Observer<T> {
    void onSuccess(T value);

    void onError(Throwable e);
}