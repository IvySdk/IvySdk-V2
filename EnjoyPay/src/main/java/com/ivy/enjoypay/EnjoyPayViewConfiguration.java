package com.ivy.enjoypay;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class EnjoyPayViewConfiguration {

    private final static String WEB_VIEW_TAG = "EnjoyPayView";

    /**
     *
     * @param webView        内置浏览器控件
     * @param fixUaToBrowser 是否去除WebView标识(对部分通道体验有优化)
     * @param webViewClient  定制Client
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static void init(WebView webView, boolean fixUaToBrowser, EnjoyWebViewClient webViewClient) {
        WebSettings webSettings = webView.getSettings();
        //config base .
        webViewSettingUpdate(webSettings, fixUaToBrowser);
        //setting download
        webView.setDownloadListener(new EnjoyDownloadListener(webView.getContext()) {
            @Override
            public void handleStartActivity(Intent intent) {
                webViewClient.handleStartActivity(intent);
            }
        });
        webView.setWebViewClient(webViewClient);
        webView.setWebChromeClient(new EnjoyWebChromeClient(fixUaToBrowser));
    }

    /**
     * 初始化WebView
     *
     * @param webView 内置浏览器控件
     * @param handler app外跳转线程处理
     */
    public static void init(WebView webView, EnjoyOutJumpHandler handler) {
        init(webView, false, handler);
    }

    /**
     * 初始化WebView
     *
     * @param webView        内置浏览器控件
     * @param fixUaToBrowser 是否去除WebView标识(对部分通道体验有优化)
     * @param handler        app外跳转线程处理
     */
    public static void init(WebView webView, boolean fixUaToBrowser, EnjoyOutJumpHandler handler) {
        init(webView, fixUaToBrowser, new EnjoyWebViewClient() {
            @Override
            public void handleStartActivity(Intent intent) {
                handler.apply(intent);
            }
        });
    }


    /**
     * first use chrome,then use other
     */
    private static boolean browser(Uri uri, String packageName, EnjoyOutJumpHandler handler) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        boolean hasPackage = packageName != null && packageName.trim().length() > 0;
        if (hasPackage) {
            intent.setPackage(packageName);
        }
        try {
            handler.apply(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.w(WEB_VIEW_TAG, e);
        }

        if (!hasPackage) {
            return false;
        }

        try {
            handler.apply(new Intent(Intent.ACTION_VIEW, uri));
            return true;
        } catch (ActivityNotFoundException e) {
            Log.w(WEB_VIEW_TAG, e);
            return false;
        }
    }

    private static Uri isMarketAppUri(Uri uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return null;
        }
        if (!"play.google.com".equalsIgnoreCase(uri.getHost())) {
            return null;
        }
        if (!uri.getPath().equals("/store/apps/details")) {
            return null;
        }
        String id = uri.getQueryParameter("id");
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return Uri.parse("market://details?" + uri.getEncodedQuery());
    }

    private static Uri isRuStore(Uri uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return null;
        }
        if (!"www.rustore.ru".equalsIgnoreCase(uri.getHost())) {
            return null;
        }
        String appUriStart = "/catalog/app/";
        if (!uri.getPath().startsWith(appUriStart)) {
            return null;
        }
        return Uri.parse("rustore://apps.rustore.ru/app/" + uri.getPath().replace(appUriStart, ""));
    }

    public abstract static class EnjoyWebViewClient extends WebViewClient {

        public abstract void handleStartActivity(Intent intent);

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (Arrays.asList("http", "https", "ws", "wss", "file").contains(uri.getScheme())) {
                Uri marketUri = isMarketAppUri(uri);
                if (marketUri != null) {
                    return openMarket(marketUri);
                }
                marketUri = isRuStore(uri);
                if (marketUri != null) {
                    return openWithMarket(marketUri, "ru.vk.store");
                }
                return super.shouldOverrideUrlLoading(view, request);
            }
            try {
                Log.i(WEB_VIEW_TAG, uri.getScheme());
                //通过Intent跳转
                if (uri.getScheme().equals("intent")) {
                    Intent intent = null;
                    try {
                        intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                        // make sure it does NOT open in the stack of your activity
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        // task reparenting if needed
                        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        // if the Google Play was already open in a search result
                        //  this make sure it still go to the app page you requested
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        // this make sure only the Google Play app is allowed to
                        // intercept the intent
                        handleStartActivity(intent);
                        Log.i(WEB_VIEW_TAG, "start intent:" + uri);
                    } catch (ActivityNotFoundException | URISyntaxException e) {
                        Log.w(WEB_VIEW_TAG, e);
                        Log.i(WEB_VIEW_TAG, "can not open intent:" + uri);
                        if (intent != null) {
                            String browserFallbackUrl = intent.getStringExtra("browser_fallback_url");
                            if (browserFallbackUrl != null && browserFallbackUrl.startsWith("https")) {
                                Log.i(WEB_VIEW_TAG, "backup url:" + browserFallbackUrl);
                                view.loadUrl(browserFallbackUrl);
                                return true;
                            } else {
                                if (intent.getPackage() != null && intent.getPackage().trim().length() > 0) {
                                    return openMarket(Uri.parse("market://details?id=" + intent.getPackage()));
                                }
                            }
                        }
                    }
                    return true;
                }
                //如果是market协议跳转到market
                if (uri.getScheme().equals("market")) {
                    return openMarket(uri);
                }
                //自定义协议
                try {
                    Log.i(WEB_VIEW_TAG, uri.toString());
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    handleStartActivity(intent);
                    return true;
                } catch (ActivityNotFoundException e) {
                    Log.w(WEB_VIEW_TAG, e);
                    //如果存在url浏览器调用情况跳转至浏览器,最后的备用方案
                    String packageId = uri.getQueryParameter("packageId");
                    Uri url = loadParamUrl(uri);
                    if (packageId != null && packageId.trim().length() > 0) {
                        boolean flag = browser(url, packageId, this::handleStartActivity);
                        if (flag) {
                            return true;
                        }
                    }

                    if (url != null) {
                        view.loadUrl(url.toString());
                        return true;
                    }
                }
            } catch (RuntimeException runtime) {
                Log.w(WEB_VIEW_TAG, runtime);
            }
            Log.w(WEB_VIEW_TAG, "Scheme '" + uri.getScheme() + "' had not been support.");
            return true;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            Log.i(WEB_VIEW_TAG, "onReceivedError:" + request.getUrl() + " error:" + error);
            super.onReceivedError(view, request, error);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            Log.i(WEB_VIEW_TAG, "onReceivedHttpError:" + request.getUrl() + " error:" + errorResponse);
            super.onReceivedHttpError(view, request, errorResponse);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Log.i(WEB_VIEW_TAG, "onReceivedSslError:" + error);
            super.onReceivedSslError(view, handler, error);
        }

        private Uri loadParamUrl(Uri uri) {
            String url = uri.getQueryParameter("url");
            if (url == null || url.trim().length() == 0) {
                url = uri.getQueryParameter("navigate_url");
            }
            if (url != null && url.trim().length() > 0) {
                return Uri.parse(url);
            }
            return null;
        }

        private boolean openWithMarket(Uri uri, String packageName) {
            Intent playIntent = new Intent(Intent.ACTION_VIEW, uri);
            playIntent.setPackage(packageName);
            // make sure it does NOT open in the stack of your activity
            playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // task reparenting if needed
            playIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            // if the Google Play was already open in a search result
            //  this make sure it still go to the app page you requested
            playIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            // this make sure only the Google Play app is allowed to
            // intercept the intent
            try {
                handleStartActivity(playIntent);
                Log.i(WEB_VIEW_TAG, packageName);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.w(WEB_VIEW_TAG, e);
                return false;
            }
        }

        private boolean openMarket(Uri uri) {
            if (openWithMarket(uri, "com.android.vending")) {
                return true;
            }
            if (openWithMarket(uri, "ru.vk.store")) {
                return true;
            }
            return browser(Uri.parse("https://play.google.com/store/apps/details?" + uri.getEncodedQuery()), "com.android.chrome", this::handleStartActivity);
        }
    }

    private abstract static class EnjoyDownloadListener implements DownloadListener {

        private final static String WEB_VIEW_TAG = "WebView";

        private final Context applicationContext;

        public EnjoyDownloadListener(Context context) {
            this.applicationContext = context.getApplicationContext();
        }

        public abstract void handleStartActivity(Intent intent);

        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
            Log.i(WEB_VIEW_TAG, "DOWNLOAD url:" + url);
            Log.i(WEB_VIEW_TAG, "DOWNLOAD mimetype:" + mimetype);
            Uri uri = Uri.parse(url);
            Log.i(WEB_VIEW_TAG, "DOWNLOAD Scheme:" + uri.getScheme());
            if ("data".equalsIgnoreCase(uri.getScheme())) {
                Log.i(WEB_VIEW_TAG, "DOWNLOAD SchemeSpecificPart:" + uri.getSchemeSpecificPart());
                String text = uri.getSchemeSpecificPart();
                int index = text.indexOf(',');
                if (index < 0) {
                    return;
                }
                String data = "";
                if (index + 1 < text.length()) {
                    data = text.substring(index + 1);
                }

                String info = text.substring(0, index);

                String mimeType;
                boolean useBase64 = false;
                index = info.indexOf(';');
                if (index < 0) {
                    mimeType = info;
                } else {
                    mimeType = info.substring(0, index);
                    if (index + 1 < info.length()) {
                        useBase64 = "base64".equalsIgnoreCase(info.substring(index + 1));
                    }
                }

                byte[] array;
                if (useBase64) {
                    array = Base64.decode(data, Base64.DEFAULT);
                } else {
                    try {
                        array = URLDecoder.decode(data, StandardCharsets.UTF_8.name()).getBytes(StandardCharsets.UTF_8);
                    } catch (UnsupportedEncodingException e) {
                        Log.w(WEB_VIEW_TAG, e);
                        return;
                    }
                }
                try {
                    File file = saveFile(array, mimeType, applicationContext);
                    Toast.makeText(applicationContext, "File save:" + file, Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    Log.w(WEB_VIEW_TAG, e);
                }
                return;
            }
            browser(uri, "com.android.chrome", this::handleStartActivity);
        }


        public File saveFile(byte[] b, String mimiType, Context context) throws IOException {
            // 创建文件路径
            File storagePath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (!storagePath.exists()) {
                if (!storagePath.mkdirs()) {
                    return null;
                }
            }
            if (!storagePath.isDirectory()) {
                return null;
            }
            String fileName = UUID.randomUUID().toString().replace("-", "").toLowerCase();
            if (mimiType != null) {
                String subFix = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimiType);
                if (subFix != null) {
                    fileName = fileName + '.' + subFix;
                }
            }
            File file = new File(storagePath, fileName);

            if (file.createNewFile()) {
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(b);
                    out.flush();
                    return file;
                } catch (IOException e) {
                    Log.w(WEB_VIEW_TAG, e);
                }
            }
            return null;
        }
    }


    public interface EnjoyOutJumpHandler {
        void apply(Intent intent);
    }

    private static class EnjoyWebChromeClient extends WebChromeClient {

        private Runnable cancelHandler = null;

        private final boolean fixUaToBrowser;

        EnjoyWebChromeClient(boolean fixUaToBrowser) {
            this.fixUaToBrowser = fixUaToBrowser;
        }

        @Override
        public void onCloseWindow(WebView window) {
            Log.i(WEB_VIEW_TAG, "onCloseWindow:" + this.cancelHandler);
            if (cancelHandler != null) {
                try {
                    cancelHandler.run();
                } catch (RuntimeException e) {
                    Log.w(WEB_VIEW_TAG, "onCloseWindow", e);
                }
            } else {
                Log.w(WEB_VIEW_TAG, "onCloseWindow handler is null");
            }
            super.onCloseWindow(window);
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            WebView subWebView = new WebView(view.getContext());
            subWebView.setWebViewClient(new EnjoyWebViewClient() {
                @Override
                public void handleStartActivity(Intent intent) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            view.getContext().startActivity(intent);
                        } catch (Exception e) {
                            Log.e("safeStartActivity", "Failed to start activity", e);
                        }
                    });
                }
            }); // 可选
            webViewSettingUpdate(subWebView.getSettings(), fixUaToBrowser);
            EnjoyWebChromeClient chromeClient = new EnjoyWebChromeClient(fixUaToBrowser);
            subWebView.setWebChromeClient(chromeClient);
            // 你可以使用弹出 Dialog 或自己定义的弹窗方式
            AlertDialog dialog = new AlertDialog.Builder(view.getContext()).setView(subWebView).setCancelable(false).setPositiveButton(android.R.string.cancel, (d, w) -> subWebView.destroy()).create();
            dialog.show();
            chromeClient.cancelHandler = new Runnable() {
                @Override
                public void run() {
                    dialog.cancel();
                }
            };
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(subWebView);
            resultMsg.sendToTarget();
            return true;
        }

    }

    @SuppressLint("SetJavaScriptEnabled")
    private static void webViewSettingUpdate(WebSettings webSettings, boolean fixUaToBrowser) {
        //config base .
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);

        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setDefaultTextEncodingName(StandardCharsets.UTF_8.name());
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setSaveFormData(true);

        if (fixUaToBrowser) {
            String userAgent = webSettings.getUserAgentString();
            webSettings.setUserAgentString(userAgent.replaceFirst("(\\s*)Build/([^;)]*)(;\\s*[^)]*)?", "$1Build/$2"));
        }
    }
}
