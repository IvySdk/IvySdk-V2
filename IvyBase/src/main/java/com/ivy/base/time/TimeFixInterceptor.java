package com.ivy.base.time;

import com.ivy.base.ILog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Interceptor;
import okhttp3.Response;

public class TimeFixInterceptor implements Interceptor {
    private final SntpClient sntpClient;

    public TimeFixInterceptor(SntpClient client) {
        this.sntpClient = client;
    }

    @Override
    public Response intercept(Chain chain) throws java.io.IOException {
        Response response = chain.proceed(chain.request());

        // 如果 NTP 还没成功，则尝试从 HTTP Header 获取
        if (sntpClient != null && !sntpClient.isSuccess()) {
            String dateStr = response.header("Date");
            if (dateStr != null) {
                try {
                    // HTTP Date 格式通常为: Sun, 06 Nov 1994 08:49:37 GMT
                    SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                    format.setTimeZone(TimeZone.getTimeZone("GMT"));
                    Date serverDate = format.parse(dateStr);

                    if (serverDate != null) {
                        sntpClient.updateOffsetFromExternal(serverDate.getTime());
                        ILog.d("time", "Time synced via HTTP Header: " + serverDate + ";" + serverDate.getTime());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return response;
    }
}