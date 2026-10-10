package com.ivy.base.time;

import android.os.Build;

import androidx.annotation.NonNull;

import com.ivy.base.ILog;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TimeUtil {

    private SntpClient sntpClient = null;
    private OkHttpClient client = null;
    private static TimeUtil _instance = null;
    private TimeUtil(){}

    public static TimeUtil getInstance() {
        if (_instance == null) {
            synchronized (TimeUtil.class) {
                if (_instance == null) {
                    _instance = new TimeUtil();
                }
            }
        }
        return _instance;
    }

    public void initTime() {
        client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new TimeFixInterceptor(getSntpClient()))
                .connectTimeout(2, TimeUnit.SECONDS)
                .build();
        loadHttpTime("https://google.com");
        loadHttpTime("https://cloudflare.com");
        loadSntpTime();
    }

    public SntpClient getSntpClient() {
        if (sntpClient == null) {
            sntpClient = new SntpClient();
        }
        return sntpClient;
    }

    private void loadHttpTime(String url) {
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

            }
        });
    }

    private void loadSntpTime() {
        String[] hosts = {
                "time.google.com",
                "time.cloudflare.com",
                "ntp.aliyun.com",
                "pool.ntp.org"
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (String host : hosts) {
                    ILog.d("time", "start:" + host);
                    boolean result = getSntpClient().requestTime(host, 2000);
                    ILog.d("time", "end:" + host + " ;" + result);
                    if (result) {
                        break;
                    }
                }
            }
        }).start();
    }

    public long getTimestamp() {
        if (sntpClient != null && sntpClient.isSuccess()) {
            return sntpClient.currentTimeMillis();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Instant.now().toEpochMilli();
        }
        return System.currentTimeMillis();
    }

    public String getFormattedUTCTime(String timeFormat) {
        long timestamp = getTimestamp();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant instant = Instant.ofEpochMilli(timestamp);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat)
                    .withLocale(Locale.US)
                    .withDecimalStyle(DecimalStyle.STANDARD);
            return instant.atZone(ZoneOffset.UTC).format(formatter);
        } else {
            Date date = new Date(timestamp);
            SimpleDateFormat formatter = new SimpleDateFormat(timeFormat, Locale.US);
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            return formatter.format(date);
        }
    }

    public String getFormattedLocalTime(String timeFormat) {
        long timestamp = getTimestamp();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant instant = Instant.ofEpochMilli(timestamp);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat)
                    .withLocale(Locale.US)
                    .withDecimalStyle(DecimalStyle.STANDARD);
            return instant.atZone(ZoneId.systemDefault()).format(formatter);
        } else {
            Date date = new Date(timestamp);
            SimpleDateFormat formatter = new SimpleDateFormat(timeFormat, Locale.US);
            return formatter.format(date);
        }
    }


}
