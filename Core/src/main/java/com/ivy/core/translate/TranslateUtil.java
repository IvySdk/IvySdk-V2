package com.ivy.core.translate;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.ivy.base.ILog;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;
import com.ivy.base.util.Util;
import com.ivy.core.IvySdk;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TranslateUtil {

    private static OkHttpClient okHttpClient = null;

    public interface Action {
        void onResult(String from, String dst);
    }

    public static void translate(String from_lang, String dst_lang, String data, Action action) {
        try {
            TestManager.getInstance().addEvent("Translate", "开始翻译", EventState.NORMAL, "from_lang=" + from_lang + ";dst_lang=" + dst_lang + ";data=" + data);
            String apiKey = IvySdk.getInstance().getGridConfig("translate.key", null);
            if (TextUtils.isEmpty(apiKey)) {
                ILog.w("translate", "failed: invalid api key");
                TestManager.getInstance().addEvent("Translate", "翻译错误", EventState.ERROR, "invalid api key");
                if (action != null) action.onResult(data, "");
                return;
            }
            JSONObject json = new JSONObject();
            json.put("q", data);
            if (!TextUtils.isEmpty(from_lang)) {
                json.put("source", from_lang);
            }
            json.put("target", dst_lang);
            json.put("format", "text");
            String url = "https://translation.googleapis.com/language/translate/v2";
            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("x-goog-api-key", apiKey)
                    .addHeader("X-Android-Package", Util.packageName())
                    .build();
            if (okHttpClient == null) {
                okHttpClient = new OkHttpClient.Builder()
                        .connectTimeout(2, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(true)
                        .build();
            }
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    ILog.w("translate", "request failed:" + e.getMessage());
                    TestManager.getInstance().addEvent("Translate", "翻译错误", EventState.ERROR, "request failed:" + e.getMessage());
                    if (action != null) {
                        action.onResult(data, "");
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        String result = response.body().string();
                        ILog.d("translate", "response:" + result);
                        TestManager.getInstance().addEvent("Translate", "翻译结果", EventState.NORMAL, result);
                        String translated_txt = new JSONObject(result)
                                .getJSONObject("data")
                                .getJSONArray("translations")
                                .getJSONObject(0)
                                .getString("translatedText");
                        if (action != null) {
                            action.onResult(data, translated_txt);
                        }
                    } catch (Exception e) {
                        TestManager.getInstance().addEvent("Translate", "翻译错误", EventState.ERROR, "解析失败:" + e.getMessage());
                        if (action != null) {
                            action.onResult(data, "");
                        }
                    }
                }
            });
        } catch (Exception e) {
            ILog.w("translate", "failed:" + e.getMessage());
            TestManager.getInstance().addEvent("Translate", "翻译错误", EventState.ERROR, "failed:" + e.getMessage());
            if (action != null) {
                action.onResult(data, "");
            }
        }

    }


}
