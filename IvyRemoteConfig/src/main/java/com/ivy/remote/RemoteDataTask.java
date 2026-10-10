package com.ivy.remote;

import com.ivy.base.ILog;
import com.ivy.base.http.HttpUtil;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RemoteDataTask {

    private static final String TAG = "RemoteDataTask";

    public static String loadData(String url) {
        OkHttpClient okHttpClient = HttpUtil.getInstance().getOkHttpClient();
        Request request = new Request.Builder().url(url).get().build();
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
        } catch (Exception e) {
            ILog.e(TAG, "get url: " + url + " failed: " + e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }

    public static String post(String url, JSONObject body) {
        OkHttpClient okHttpClient = HttpUtil.getInstance().getOkHttpClient();
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody postBody = RequestBody.create(body.toString(), mediaType);
        Request request = new Request.Builder().url(url).post(postBody).build();

        Response response = null;
        try {
            ILog.i(IvyRemoteConfig.TAG, "post url: " + url);
            ILog.i(IvyRemoteConfig.TAG, "params: " + body.toString());
            response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
        } catch (Exception e) {
            ILog.e(TAG, "post url: " + url + " failed: " + e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }
}
