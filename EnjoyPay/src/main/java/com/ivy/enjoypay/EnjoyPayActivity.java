package com.ivy.enjoypay;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebView;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.Nullable;

public class EnjoyPayActivity extends Activity {

    public static int REQUEST_CODE = 9001;
    public static int RESULT_SUCCESS_CODE = 9002;
    public static int REQUEST_FAILED_CODE = 9003;

    private WebView webView = null;

    private boolean resultSent = false;
    private String merchantTransactionId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra("payUrl");
        merchantTransactionId = getIntent().getStringExtra("merchantTransactionId");
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(merchantTransactionId)) {
            finishPay(REQUEST_FAILED_CODE);
            return;
        }
        webView = new WebView(this);
        setContentView(webView);
        EnjoyPayViewConfiguration.init(webView, true, new EnjoyPayViewConfiguration.EnjoyOutJumpHandler() {
            @Override
            public void apply(Intent intent) {
                // 定义跳转应用的方法，注意避免线程问题
                startActivity(intent);
            }
        });
        // 加载支付 url，支付 url 为创建订单接口返回
        webView.loadUrl(url);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> {
                        finishPay(RESULT_SUCCESS_CODE);
                    }
            );
        }
    }

    @Override
    public void onBackPressed() {
        finishPay(RESULT_SUCCESS_CODE);
    }

    private void finishPay(int resultCode) {
        if (resultSent) {
            return;
        }

        resultSent = true;
        Intent intent = new Intent();
        intent.putExtra("merchantTransactionId", merchantTransactionId);
        setResult(resultCode, intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }


}
