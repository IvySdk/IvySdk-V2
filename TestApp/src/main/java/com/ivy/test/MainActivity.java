package com.ivy.test;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.client.AndroidSdk;
import com.android.client.SplashImageLoader;
import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.ad.AdType;
import com.ivy.base.ad.IAdListener;
import com.ivy.core.Builder;
import com.ivy.core.IvySdk;
import com.puzzlegames.puzzlebrickslegend.R;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ImageView splashImageView;

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        ILog.d("FCM", "main received");
        System.out.println("FCM intent");
        AndroidSdk.onNewIntent(intent);
        Toast.makeText(getApplicationContext(), "step 1", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        AndroidSdk.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        System.out.println("FCM onCreate");
        ILog.d("FCM", "main onCreate");
        App.setInstance(getApplication());
        ActivityUtil.getInstance().setActivity(this);
        AndroidSdk.displayInNotch();

        setContentView(R.layout.activity_main);

//        ImmersionBar.with(this)
//                .fitsSystemWindows(false)
//                .transparentStatusBar()
//                .transparentNavigationBar()
//                .hideBar(BarHide.FLAG_HIDE_BAR)
//                .init();
        IvySdk.getInstance().onCreate(this);
        SplashImageLoader.loadAndAddSplash(this, "splash", ((ViewGroup) getWindow().getDecorView()));
        AndroidSdk.onCreate(this, new Builder.Build().setAdListener(new IAdListener() {
            @Override
            public void onInited() {

            }

            @Override
            public void onAdLoadSuccess(AdType adType) {

            }

            @Override
            public void onAdLoadFailure(AdType adType, String reason) {

            }

            @Override
            public void onAdShowSuccess(AdType adType, String tag, String placement) {
                ILog.d("main", adType.value + ";" + tag + ";" + placement);
            }

            @Override
            public void onAdShowFailed(AdType adType, String reason, String tag, String placement) {

            }

            @Override
            public void onAdClicked(AdType adType, String tag, String placement) {

            }

            @Override
            public void onAdClosed(AdType adType, boolean gotReward, String tag, String placement) {
                ILog.e("main", adType + "::" + gotReward);
            }

            @Override
            public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, boolean af) {

            }


        }).build());
//        AndroidSdk.setBXId("old");
//        AndroidSdk.triggerBannerAd("0", null);
//        AndroidSdk.requestNotificationPermission();
//        AndroidSdk.pushNotificationTask("task1", "task_title", "task_subtitle", "task_big_texttask_big_texttask_big_texttask_big_texttask_big_texttask_big_texttask_big_texttask_big_texttask_big_texttask_big_texttask_big_texttask_big_texttask_big_texttask_big_text", null, null, null, System.currentTimeMillis() + 5 * 60 * 1000, 15 * 60 * 1000, true, "task11", true);
//        AndroidSdk.pushNotificationTask("task2", "task_title 22", "task_subtitle 22", null, null, null, "haha.png", System.currentTimeMillis() + 1 * 60 * 1000, 15 * 60 * 1000, true, "task22", true);
//        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                SplashImageLoader.removeSplash(((ViewGroup) getWindow().getDecorView()));
//                int hh = AndroidSdk.getGestureBarHeight();
//                ILog.d("TAG", hh + "");
////                throw new RuntimeException("Test broken");
//            }
//        }, 15 * 1000);
//
//        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                AndroidSdk.toast("hahhaha");
////                AndroidSdk.pay(100, null, null);
////
////                for (int i = 0; i < 2; i++) {
////
////                    String data = Util.randomString(14, false);
////                    String dd =  Util.base64encode(data);
////                    AndroidSdk.uploadTrajectory(i, "0", dd);
////                }
//
////                String str = loadAssets("180027.json");
////
////                AndroidSdk.uploadTrajectory(1, "0", str);
//
////                AndroidSdk.updateHelperLanguage("de");
////AndroidSdk.pay(100, "", null);
//                AndroidSdk.enjoyPay(100, "", null);
//            }
//        });
//
//        findViewById(R.id.pause).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                AndroidSdk.toast("hahhaha");
////                AndroidSdk.pay(100, null, null);
//
////                TrajectoryManager.getInstance().triggerUploadWorker();
//                AndroidSdk.updateHelperLanguage("en");
//            }
//        });
//
//        findViewById(R.id.pass_level).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                AndroidSdk.toast("hahhaha");
////                AndroidSdk.pay(100, null, null);
//
////                TrajectoryManager.getInstance().triggerUploadWorker();
//                AndroidSdk.showHelper("E001", "", "", "hahahhaha");
//                v.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        AndroidSdk.updateHelperLanguage("de");
//                    }
//                }, 5 * 1000);
//            }
//        });
//        String action = AndroidSdk.getShortcutAction();
//        ILog.d("main", "onNewIntent=" + action);


    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        ILog.d("main", "onMultiWindowModeChanged=" + isInMultiWindowMode);
    }


    public void submitGameEvent(String eventId, JSONObject property) {
        if (property != null) {
            Iterator<String> iter = property.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                try {
                    Object value = property.get(key);
                    if (value instanceof Long || value instanceof Integer) {
                        long dd = ((Number) value).longValue();
//                        long dd = Long.parseLong(value.toString());
                    }
                    if (value instanceof Double || value instanceof Float) {
                        double dd = ((Number) value).doubleValue();
                    }
                    if (value instanceof Boolean) {
                        boolean dd = Boolean.parseBoolean(value.toString());
                    }
                    if (value instanceof String) {
                        String dd = String.valueOf(value);
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String loadAssets(String fileName) {
        try {
            byte[] buffer = new byte[App.getInstance().getAssets().open(fileName).available()];
            App.getInstance().getAssets().open(fileName).read(buffer);
            return new String(buffer);
        } catch (Exception e) {

        }
        return null;
    }

    public void onClick(View v) {
//        if (v.getId() == R.id.start) {
//            try {
//                JSONObject json = new JSONObject();
//
//                AndroidSdk.trackEvent("GameOver", json.toString(), 5);
//            } catch (Exception ignored) {
//            }
//        }
//        Map<String, String> data = new HashMap<>();
//        switch (v.getId()) {
//            case R.id.start:
//                try {
//                    JSONObject json = new JSONObject();
//                    json.put("revenue", 5);
//                    AndroidSdk.trackEvent("af_ad_revenue", json.toString(), 3);
//                }catch (Exception ignored){}
//
////                AndroidSdk.rateUs();
////                try {
////                    JSONObject json = new JSONObject();
////                    json.put("ad_format", "interstitial");
////                    AndroidSdk.trackEvent("sdk_ad_shown", json.toString(), 5);
////                }catch (Exception ignored){}
////                AndroidSdk.trackEvent("GameOver", "", 5);
////                AndroidSdk.showWebView("haha", "https://10093supporttest.winwingplane.com/?appid=2750&uuid=Editor&os=android");
////                AndroidSdk.vibrate(100, 50);
////                AndroidSdk.showInterstitialAd("default", "", "");
////                data.put("title", "title1");
////                data.put("body", "subtitle1");
////                data.put("action", "test action1");
////                NotificationHelper.show(v.getContext(), "title1", "subtitle1", data);
////
////                data.put("title", "title2");
////                data.put("body", "subtitle2");
////                data.put("action", "test action2");
////                data.put("bigText", "bigTextbigTextbigTextbigTextbigTextbigTextbigTextbigTextbigTextbigText");
////                NotificationHelper.show(v.getContext(), "title2", "subtitle2", data);
////
////
////                data.put("title", "title3");
////                data.put("body", "subtitle3");
////                data.put("action", "test action3");
////                data.remove("bigText");
////                data.put("image", "https://pic.rmb.bdstatic.com/bjh/news/3fe6db1a8d291be39192f9a06c74ce99.png");
////                NotificationHelper.show(v.getContext(), "title3", "subtitle3", data);
//                break;
//
//            case R.id.pause:
//                try {
//                    JSONObject json = new JSONObject();
//                    json.put("success", "0");
//                    AndroidSdk.trackEvent("normalgame", json.toString(), 5);
//                } catch (Exception ignored) {
//                }
////                AndroidSdk.vibrateWithCure("100,50,3000,100", "255,0,0,50");
////                AndroidSdk.showRewardedAd("default", "", "");
//                break;
//
//            case R.id.banner:
//
//                AndroidSdk.triggerBannerAd("0", null);
//                AndroidSdk.showBannerAd("", 0, "", "");
//                break;
//
//            case R.id.close_banner:
//                AndroidSdk.closeBannerAd("");
//                break;
//        }
    }
}