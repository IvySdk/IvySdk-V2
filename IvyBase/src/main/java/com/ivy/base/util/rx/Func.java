package com.ivy.base.util.rx;

public interface Func<T, R> {
    R call(T value) throws Exception;
}
