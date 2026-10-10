package com.ivy.base.util;

public interface Func<T, R> {
    R apply(T t);
}