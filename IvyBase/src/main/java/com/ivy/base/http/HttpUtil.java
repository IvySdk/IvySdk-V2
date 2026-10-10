package com.ivy.base.http;

import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {

    // 单例实例
    private static final HttpUtil INSTANCE = new HttpUtil();

    private OkHttpClient okHttpClient;

    private HttpUtil() {
        // 私有构造函数，防止外部实例化
    }

    public static HttpUtil getInstance() {
        return INSTANCE;
    }

    public synchronized OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
        }
        return okHttpClient;
    }

    public Call request(Request request, Callback callback) {
        Call call = getOkHttpClient().newCall(request);
        call.enqueue(callback);
        return call;
    }
}
