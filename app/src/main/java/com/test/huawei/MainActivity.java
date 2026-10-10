package com.test.huawei;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

/**
 * Created by song on 16/5/26.
 */
public class MainActivity extends Activity {
    //    FirestoreAdapter dbAdapter = null;
    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
//
//        AndroidSdk.Builder builder = new AndroidSdk.Builder();
//
//        builder.setPaymentListener(new PaymentSystemListener() {
//
//        });
//
//        AndroidSdk.onCreate(this, builder);
//AndroidSdk.enableAbTest(0);
//AndroidSdk.pullAbTestConfigs();
//
//
//        dbAdapter = FirestoreAdapter.getInstance();
//
////        try {
////            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(),
////                    PackageManager.GET_SIGNATURES);
////            for (Signature signature : info.signatures) {
////                MessageDigest md = MessageDigest.getInstance("SHA");
////                md.update(signature.toByteArray());
////                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
////            }
////        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
////            e.printStackTrace();
////        }
////
////        AndroidSdk.Builder builder = new AndroidSdk.Builder();
////        builder.setSdkResultListener(new SdkResultListener() {
////            @Override
////            public void onInitialized() {
////                asyncToast("sdk initialized");
////            }
////
////            @Override
////            public void onReceiveServerExtra(String data) {
////                asyncToast("server data: " + data);
////            }
////
////            @Override
////            public void onReceiveNotificationData(String data) {
////                asyncToast("noti: " + data);
////            }
////        }).setUserCenterListener(new UserCenterListener() {
////            @Override
////            public void onReceiveLoginResult(boolean success) {
////                asyncToast("login? " + success);
////            }
////
////            @Override
////            public void onReceiveFriends(String friends) {
////                asyncToast("receive friends? " + friends);
////            }
////
////            @Override
////            public void onReceiveInviteResult(boolean success) {
////                asyncToast("invite? " + success);
////            }
////
////            @Override
////            public void onReceiveChallengeResult(int count) {
////                asyncToast("challenge? " + " count: " + count);
////            }
////
////            @Override
////            public void onReceiveLikeResult(boolean success) {
////                asyncToast("like? " + success);
////            }
////
////        }).setAdEventListener(new AdEventListener() {
////            @Override
////            public void onAdShow(String tag, String platform, int type){
////                super.onAdShow(tag, platform, type);
////                asyncToast("on Ad Show : " + tag);
////            }
////
////            @Override
////            public void onAdClicked(String tag, String platform, int type) {
////                super.onAdClicked(tag, platform, type);
////                asyncToast("on Ad Click : " + tag);
////            }
////        }).setPaymentListener(new PaymentSystemListener() {
////            @Override
////            public void onPaymentSuccess(int billId) {
////                asyncToast("payment success: " + billId);
////            }
////
////            @Override
////            public void onPaymentFail(int billId) {
////                asyncToast("payment fail: " + billId);
////            }
////
////            @Override
////            public void onPaymentCanceled(int bill) {
////                asyncToast("payment cancel: " + bill);
////            }
////
////            @Override
////            public void onPaymentSystemValid() {
////                Log.e("DEMO", "pay system is valid");
////            }
////        });
////
////        AndroidSdk.onCreate(this, builder);
////        AndroidSdk.loadExtra(1);
//
//        setContentView(R.layout.activity_main);
//        // ATTENTION: This was auto-generated to handle app links.
//        Intent appLinkIntent = getIntent();
//        String appLinkAction = appLinkIntent.getAction();
//        Uri appLinkData = appLinkIntent.getData();
//
//        DeviceUtil.getInstance().getCpuInfo();


    }

//    private void showBubbleNotification() {
//        // Create a bubble intent.
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
//            Intent target = new Intent(this, BubbleActivity.class);
//            PendingIntent bubbleIntent = PendingIntent.getActivity(this, 0, target, PendingIntent.FLAG_MUTABLE /* flags */);
//
//            String CATEGORY_TEXT_SHARE_TARGET = "com.example.category.IMG_SHARE_TARGET";
//
//            Person chatPartner = new Person.Builder()
//                    .setName("Chat partner")
//                    .setImportant(true)
//                    .build();
//
//// Create a sharing shortcut.
//            String shortcutId = "test_shortcut_id";
//            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, shortcutId)
//                    .setCategories(Collections.singleton(CATEGORY_TEXT_SHARE_TARGET))
//                    .setIntent(new Intent(Intent.ACTION_DEFAULT))
//                    .setLongLived(true)
//                    .setShortLabel(chatPartner.getName())
//                    .build();
//// Create a bubble metadata.
//            Notification.BubbleMetadata bubbleData = new Notification.BubbleMetadata.Builder(bubbleIntent, Icon.createWithResource(this, R.drawable.ic_launcher))
//                    .setDesiredHeight(600)
//                    .setAutoExpandBubble(true)
//                    .setSuppressNotification(true)
//                    .build();
//            String channelId = "test_channel_id";
//            String channelName = "test_channel_name";
//// Create a notification, referencing the sharing shortcut.
//            Notification.Builder builder = new Notification.Builder(this, channelId)
//                    .setContentIntent(bubbleIntent)
//                    .setSmallIcon(R.drawable.ic_launcher)
//                    .setBubbleMetadata(bubbleData)
//                    .setShortcutId(shortcutId)
//                    .addPerson(chatPartner);
//
//            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
//                manager.createNotificationChannel(channel);
//            }
//            manager.notify(0, builder.build());
//        }
//    }
//
//    private static byte[] ivBytes = {0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30};
//
//    void asyncToast(final String message) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    boolean nativeShowing;
//    boolean nativeBannerShowing;
//
//    private void resetComponentState(String cls, int state) {
//        ComponentName componentName = new ComponentName(getPackageName(), cls);
//        getPackageManager().setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP);
//    }
//
//    private void changeIcon(String targetComponentName) {
//
//        String originCls = getClass().getName();
//        String lastCls = IvySdk.mmGetStringValue("lastCls", "");
//        Log.d("ActivityAlias", "origin:" + originCls + "  last:" + lastCls + "  target:" + targetComponentName);
//        if (TextUtils.isEmpty(lastCls)) {
//            lastCls = originCls;
//        }
//        if (targetComponentName.equalsIgnoreCase(lastCls)) {
//            return;
//        }
//        IvySdk.mmSetStringValue("lastCls", targetComponentName);
//        resetComponentState(targetComponentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
//        resetComponentState(lastCls, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
//    }
//
//    private void testChangeIcon() {
//        int index = new Random().nextInt() % 3;
//        String targetCls = "com.test.huawei.MainActivity";
//        if (index == 0) {
//            targetCls = "com.test.icon";
//        } else if (index == 1) {
//            targetCls = "com.test.icon2";
//        } else if (index == 2) {
//            targetCls = "com.test.huawei.MainActivity";
//        }
//        changeIcon(targetCls);
//    }
//
//
//
//
//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.changeIcon:
//                Map<String, Object> map = new HashMap<>();
//                map.put("ad_format", "rewarded");
//                AndroidSdk.track("test_event", map);
////                String result = AndroidSdk.getRemoteConfigString("ad_price");
////                AndroidSdk.toast("ad_price==>" + result);
////                AndroidSdk.track("ttest", "level,10");
////                showBubbleNotification();
////                testChangeIcon();
////                    AndroidSdk.pushLocalNotification("test",
////                            "test title",
////                            "test subtitle",
////                            "test big ttest big texttest big textextbig ttest big texttest big textexbig ttest big texttest big textexbig ttest big texttest big textexbig ttest big texttest big textexbig ttest big texttest big textexbig ttest big texttest big textexbig ttest big texttest big textexbig ttest big texttest big textex",
////                            "test_icon",
////                            "test",
////                            null,
////                            true,
////                            10,
////                            false,
////                            false,
////                            false);
//                break;
//            case R.id.free:
//                AndroidSdk.showRewardAd("1", new AdListener(){
//                    @Override
//                    public void onAdShow() {
//                        super.onAdShow();
//                        Log.e("ttt", "ad show");
//                    }
//                });
//                break;
//            case R.id.start:
//
//                AndroidSdk.showFullAd("default");
//                break;
//            case R.id.pause:
//                // IvySdk.showInterstitialAd();
//                // IvySdk.showPopupInAppMessage("hehe", "您的ticket信息有更新", null, null);
//                AndroidSdk.checkHelpEngagement("farmtownguest");
//                break;
//
//            case R.id.exit:
//                //IvySdk.showInterstitialAd();
//int state = IvySdk.getNotificationPermissionState(MainActivity.this);
//if (state == 2){
//    IvySdk.requestNotificationPermission(MainActivity.this);
//}
//                break;
//
//            case R.id.more:
//
////                IvySdk.moreGame();
//                try {
//                    JSONObject data = new JSONObject();
//                    data.put("content", "???");
//                    AndroidSdk.helpshift( "farmtownguest", data.toString());
////                    Unity.sendChat("https://mergeelves-chats.firebaseio.com/", "chats/jsding", data.toString());
//                } catch(Throwable t) {
//                    t.printStackTrace();
//                }
////                AndroidSdk.showWebView("来自精灵合成宝贝家的", "https://docs.google.com/forms/d/e/1FAIpQLSeVuRdssdP3isCvlWiLy4BoD9evJDzMKnYLlbc00cE5ot6S3g/viewform?usp=sf_link");
//                // Unity.initializeAndLinkFacebookAfterSignIn();
//                // throw new RuntimeException("Test Crash"); // Force a crash
//
////                AndroidSdk.loginFacebook(new FacebookLoginListener() {
////                    @Override
////                    public void onReceiveLoginResult(boolean success) {
////                        FirestoreAdapter.getInstance().initializeAndLinkFacebookAfterSignIn(new DatabaseConnectListener() {
////                            @Override
////                            public void onSuccess() {
////
////                            }
////
////                            @Override
////                            public void onFail() {
////
////                            }
////                        });
////                     }
////
////                    @Override
////                    public void onReceiveFriends(String friends) {
////
////                    }
////                });
//
//
//                break;
//
//            case R.id.banner:
////                AndroidSdk.shareInviteLink("https://www.baidu.com");
////                bannerIdx = (++bannerIdx) % bannerPos.length;
//
//                IvySdk.showBannerAd(IvySdk.POS_CENTER_BOTTOM);
////                IvySdk.showSplashBannerAd(IvySdk.POS_CENTER_BOTTOM);
//                break;
//
//            case R.id.bill:
//               // Unity3dPlayerActivity.launch(this, 1);
//                // IvySdk.pay("block.puzzle.removeads", "");
////                if (AndroidSdk.isXsollaSupport()){
////                    AndroidSdk.logoutXsolla();
////                    if(!AndroidSdk.isLoginXsolla()){
////                        AndroidSdk.loginXsolla();
////                    }
////                } else {
////                    AndroidSdk.pay(1);
////                }
//                AndroidSdk.pay(1);
//                break;
//
//            case R.id.close_banner:
////                AndroidSdk.closeSplashBanner();
//                IvySdk.closeBanners();
////                AndroidSdk.openFacebook("61550756672632", "Bubble-Pop-Star");
//                break;
//
//            case R.id.custom:
////                AndroidSdk.verifyIdCard();
////                Logger.debug("AndroidSdk", AndroidSdk.hasNotch());
//////                Logger.debug("AndroidSdk", AndroidSdk.getSKUDetail(1).toJson().toString());
//////                Logger.debug("AndroidSdk", AndroidSdk.getSKUDetail(22).toJson().toString());
////
////                Logger.debug("AndroidSdk", AndroidSdk.getPurchaseHistory("subs"));
////                toast("notchHeight: " + AndroidSdk.getNotchHeight());
//                // AndroidSdk.changeSku(22, 23, null);
//                dbAdapter.read("users",  new DatabaseListener() {
//                    @Override
//                    public void onData(String collection, String data) {
//                        System.out.println("------");
//                    }
//
//                    @Override
//                    public void onSuccess(String collection) {
//                        System.out.println("onSuccess");
//
//                    }
//
//                    @Override
//                    public void onFail(String collection) {
//                        System.out.println("onFail");
//
//                    }
//                });
//                break;
//
//            case R.id.pass_level:
//                AndroidSdk.pay(22);
//                break;
//
////            case R.id.free:
//////                if (AndroidSdk.hasRewardAd("default")) {
////                    AndroidSdk.showRewardAd("default", new AdListener(){
////                        @Override
////                        public void onAdReward(boolean skip) {
////                            super.onAdReward(skip);
////                            Toast.makeText(MainActivity.this, "user rewarded:" + skip, Toast.LENGTH_SHORT).show();
////                        }
////                    });
//////                } else {
//////                    Toast.makeText(this, "no video ad", Toast.LENGTH_SHORT).show();
//////                }
////                break;
//
//            case R.id.native_1:
//               // AndroidSdk.showNativeBanner("default", 27, 94, 408, 290, null);
//                // AndroidSdk.rateUs();
//                toast("Notification status: " + IvySdk.isNotificationChannelEnabled(IvySdk.getActivity()));
//                IvySdk.openNotificationSettings(IvySdk.getActivity());
////                if (nativeShowing) {
////                    AndroidSdk.hideNativeAdScrollView("unlock_pre");
////                } else {
////                    AndroidSdk.showNativeAdScrollView("unlock_pre", AndroidSdk.HIDE_BEHAVIOR_NO_HIDE, 50);
////                }
//                nativeShowing = !nativeShowing;
//                break;
//
//            case R.id.share:
////                new Thread(new Runnable() {
////                    @Override
////                    public void run() {
////                        AndroidSdk.share();
////                    }
////                }).start();
//                AndroidSdk.playerFinder();
//                break;
//
//            case R.id.islogin:
//                // IvySdk.showDeliciousBanner(0, 0, 800, 1200, null);
////                toast("is login: " + AndroidSdk.isLogin());
//
//                IvySdk.slientLoginGoogle(new GoogleListener() {
//                    @Override
//                    public void onSuccess(String googleId, String googleEmail) {
////                        dbAdapter.initializeAfterSignIn("play", null, null, null);
//                    }
//
//                    @Override
//                    public void onFails() {
//
//                    }
//                });
//                break;
//
//            case R.id.login:
////                IvySdk.closeNativeAd();
////                AndroidSdk.login();
//                AndroidSdk.loginGoogle(new GoogleListener() {
//                    @Override
//                    public void onSuccess(String googleId, String googleEmail) {
//                        Log.d("AndroidSdk", "GoogleID: " + googleId);
////                        dbAdapter.initializeAfterSignIn("play", null, null, null);
//
//                        dbAdapter.snapshot("messages", new DatabaseChangedListener() {
//                            @Override
//                            public void onData(String collection, String jsonData) {
//                                Log.e("AndroidSdk", "receiving collection: " + collection + " jsonData >>>>>> " + jsonData);
//                            }
//                        });
//
//
//
//                    }
//
//                    @Override
//                    public void onFails() {
//                        Log.d("AndroidSdk", "on Failes");
//
//
//                    }
//                });
//                break;
//
//            case R.id.logout:
////                AndroidSdk.logout();
//                AndroidSdk.logoutGoogle(new GoogleListener() {
//                    @Override
//                    public void onSuccess(String googleId, String googleEmail) {
//                        Log.d("AndroidSdk", "Google Logout");
//                    }
//
//                    @Override
//                    public void onFails() {
//                        Log.d("AndroidSdk", "Google Logout failed");
//
//                    }
//                });
//                break;
//
//            case R.id.invite:
//                AndroidSdk.invite();
//                break;
//
//            case R.id.challenge:
//                try {
//                } catch(Exception ex) {
//
//                }
////                AndroidSdk.challenge("haha title", "heihei message");
//                break;
//
//            case R.id.friends:
////                toast(AndroidSdk.friends());
//                break;
//
//            case R.id.me: {
//                AndroidSdk.showRewardedInterstitial("default", new AdListener() {
//                    public void onAdShow() {
//                        toast("展示出来了");
//
//                    }
//
//                    public void onAdClosed() {
//                        toast("关闭");
//
//                    }
//
//                    public void onAdClicked() {
//                    }
//
//                    public void onAdShowFails() {
//                    }
//
//                    public void onAdReward(boolean skip) {
//                        toast("获得奖励");
//                    }
//
//
//                    public void onAdLoadSuccess() {
//                        toast("加载成功");
//
//                    }
//
//                    public void onAdLoadFails() {
//                        toast("加载失败");
//
//                    }
//                });
////                try {
////                    String me1 = AndroidSdk.me();
////                    JSONObject me = new JSONObject(me1);
////                    if (me.has("picture")) {
////                        ImageView vv = new ImageView(this);
////                        vv.setImageURI(Uri.parse(me.getString("picture")));
////                        AlertDialog dialog = new AlertDialog.Builder(this).setMessage(me.getString("name")).create();
////                        dialog.show();
////                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(256, 256);
////                        ((FrameLayout) dialog.getWindow().getDecorView()).addView(vv, lp);
////                    } else {
////                        toast(me1);
////                    }
////                } catch (JSONException e) {
////                    e.printStackTrace();
////                }
//            }
//            break;
//
//            case R.id.like:
////                AndroidSdk.like();
////                AndroidSdk.appFeedback("https://merge-elves-38816766.web.app/english/sections.json", "https://us-central1-merge-elves-38816766.cloudfunctions.net/", "https://docs.google.com/forms/d/e/1FAIpQLSeOV9E2txOwLJNMnXA5GCbTSX_jVYsWGIQE9Bhvgq8ykIgR9w/viewform?usp=sf_link","{}");
//                break;
////
////            case R.id.submit_score:
////                AndroidSdk.submitScore("endless", 232, "");
////                break;
////
////            case R.id.load_friend:
////                AndroidSdk.loadLeaderBoard("endless", 1, 20, "");
////                break;
////
//            case R.id.load_global:
////                AndroidSdk.loadGlobalLeaderBoard("endless", 1, 20);
//
//                IvySdk.logEvent("test_event", null);
//
//                break;
////
////            case R.id.show_sales:
////                AndroidSdk.showSales(2);
////                break;
//
//
//            case R.id.show_native_banner:
//                IvySdk.showDeliciousIcon(100, 100, 200, 200, null);
////                if (nativeBannerShowing) {
////                    AndroidSdk.hideNativeBanner("default");
////                } else {
////                    AndroidSdk.showNativeBanner("default", 50, 50);
////                }
//                nativeBannerShowing = !nativeBannerShowing;
//                break;
//            case R.id.showRectBanner:
//                AndroidSdk.showRectBanner(0,0, 0, 0);
//                break;
//        }
//    }
//
//    private void toast(final String message) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        IvySdk.onStart();
//    }
//
//    @Override
//    protected void onStop() {
//        AndroidSdk.onStop();
//        super.onStop();
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        AndroidSdk.onPause();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        AndroidSdk.onResume(this);
//    }
//
//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        AndroidSdk.onNewIntent(intent);
//    }
//
//    @Override
//    protected void onDestroy() {
//        IvySdk.onDestroy();
//        super.onDestroy();
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        AndroidSdk.onActivityResult(requestCode, resultCode, data);
//        super.onActivityResult(requestCode, resultCode, data);
//    }
//
//    @Override
//    public void onBackPressed() {
//        IvySdk.onQuit();
//    }
}
