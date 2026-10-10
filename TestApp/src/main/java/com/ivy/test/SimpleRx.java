package com.ivy.test;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleRx<T> {

    public interface Function<T, R> {
        R apply(T t) throws Exception;
    }

    public interface Consumer<T> {
        void accept(T t);
    }

    public interface ErrorConsumer {
        void accept(Throwable e);
    }

    public interface Predicate<T> {
        boolean test(T t);
    }


    private T data;
    private ExecutorService subscribeExecutor;
    private Handler observeHandler;
    private ErrorConsumer errorConsumer;

    public SimpleRx(T data) {
        this.data = data;
        this.subscribeExecutor = Executors.newSingleThreadExecutor();
        this.observeHandler = new Handler(Looper.getMainLooper());
    }

    public static <T> SimpleRx<T> just(T data) {
        return new SimpleRx<>(data);
    }

    // map
    public <R> SimpleRx<R> map(Function<T, R> mapper) {
        try {
            R result = mapper.apply(data);
            return new SimpleRx<>(result).subscribeOn(subscribeExecutor).observeOn(observeHandler).onError(errorConsumer);
        } catch (Exception e) {
            if (errorConsumer != null) errorConsumer.accept(e);
            return new SimpleRx<>(null);
        }
    }

    // 异步 flatMap
    public <R> SimpleRx<R> flatMap(Function<T, SimpleRx<R>> mapper) {
        try {
            SimpleRx<R> newRx = mapper.apply(data);
            newRx.subscribeExecutor = this.subscribeExecutor;
            newRx.observeHandler = this.observeHandler;
            newRx.errorConsumer = this.errorConsumer;
            return newRx;
        } catch (Exception e) {
            if (errorConsumer != null) errorConsumer.accept(e);
            return new SimpleRx<>(null);
        }
    }

    // mapList
    public <R> SimpleRx<List<R>> mapList(Function<T, R> mapper) {
        if (!(data instanceof List)) {
            throw new IllegalStateException("mapList only works with List");
        }
        List<?> oldList = (List<?>) data;
        List<R> newList = new ArrayList<>();
        try {
            for (Object item : oldList) {
                newList.add(mapper.apply((T) item));
            }
        } catch (Exception e) {
            if (errorConsumer != null) errorConsumer.accept(e);
        }
        return new SimpleRx<>(newList).subscribeOn(subscribeExecutor).observeOn(observeHandler).onError(errorConsumer);
    }

    // filter
    public SimpleRx<T> filter(Predicate<T> predicate) {
        try {
            if (!predicate.test(data)) {
                data = null;
            }
        } catch (Exception e) {
            if (errorConsumer != null) errorConsumer.accept(e);
        }
        return this;
    }

    // subscribeOn
    public SimpleRx<T> subscribeOn(ExecutorService executor) {
        this.subscribeExecutor = executor;
        return this;
    }

    // observeOn
    public SimpleRx<T> observeOn(Handler handler) {
        this.observeHandler = handler;
        return this;
    }

    // 错误处理
    public SimpleRx<T> onError(ErrorConsumer consumer) {
        this.errorConsumer = consumer;
        return this;
    }

    // 订阅
    public void subscribe(Consumer<T> consumer) {
        subscribeExecutor.execute(() -> {
            T result = data;
            observeHandler.post(() -> consumer.accept(result));
        });
    }
}
