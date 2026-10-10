package com.ivy.core;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;

import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.ad.AdType;
import com.ivy.base.ad.IAdListener;
import com.ivy.base.ad.IAdManager;
import com.ivy.base.billing.IIPurchaseResult;
import com.ivy.base.billing.IPurchase;
import com.ivy.base.billing.IPurchaseResult;
import com.ivy.base.firebase.IFirebase;
import com.ivy.base.games.IPlayGameEvents;
import com.ivy.base.games.archive.IArchiveResult;
import com.ivy.base.games.auth.AuthPlatforms;
import com.ivy.base.games.auth.FirebaseLinkChannel;
import com.ivy.base.games.auth.IAuthResponse;
import com.ivy.base.games.auth.IAuthResult;
import com.ivy.base.games.auth.IFirebaseAuthReload;
import com.ivy.base.games.auth.IFirebaseUnlink;
import com.ivy.base.grid.GridManager;
import com.ivy.base.grid.IGridQuery;
import com.ivy.base.helper.IHelper;
import com.ivy.base.helper.IHelperCallback;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.message.FCMMessage;
import com.ivy.base.notification.NotificationPermissionUtil;
import com.ivy.base.notification.NotificationReminderScheduler;
import com.ivy.base.notification.NotificationTask;
import com.ivy.base.notification.NotificationTaskManager;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;
import com.ivy.base.time.TimeUtil;
import com.ivy.base.track.EventIDs;
import com.ivy.base.track.EventParams;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;
import com.ivy.base.track.IConversationCallback;
import com.ivy.base.track.IEvent;
import com.ivy.base.track.IKeys;
import com.ivy.base.track.TrackPlatform;
import com.ivy.base.util.AdvertisingIdManager;
import com.ivy.base.util.AppStoreUtil;
import com.ivy.base.util.IToast;
import com.ivy.base.util.InsetsHelper;
import com.ivy.base.util.ScreenLayoutController;
import com.ivy.base.util.ShortcutUtil;
import com.ivy.base.util.Util;
import com.ivy.base.util.rx.Func;
import com.ivy.base.util.rx.Observer;
import com.ivy.base.util.rx.RxAndroid;
import com.ivy.base.util.rx.Schedulers;
import com.ivy.base.util.rx.SimpleRxJust;
import com.ivy.core.Uac.UAC;
import com.ivy.core.ab.IvyABManager;
import com.ivy.core.ab.LocalAB;
import com.ivy.core.helper.IvyHelper;
import com.ivy.core.message.FCMMessager;
import com.ivy.core.payment.IvyPay;
import com.ivy.core.payment.IvyPayEvent;
import com.ivy.core.session.SessionManager;
import com.ivy.core.trajectory.TrajectoryManager;
import com.ivy.core.translate.TranslateUtil;
import com.ivy.core.util.Helper;
import com.ivy.core.util.KwaiUtil;
import com.ivy.games.IvyGames;
import com.ivy.remote.IvyRemoteConfig;
import com.ivy.track.IvyTrack;
import com.ivy.track.reframe.ReframeUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class IvySdk implements IEvent, IPurchase, IGridQuery, IHelper {

    public static final String TAG = "IvySdk";

    // 单例
    private static IvySdk _instance = null;

    public static IvySdk getInstance() {
        if (_instance == null) {
            synchronized (IvySdk.class) {
                if (_instance == null) {
                    _instance = new IvySdk();
                }
            }
        }
        return _instance;
    }

    private boolean debug = false;
    private String appId = "";

    private IvyGames ivyGames;
    private IvyHelper ivyHelper;
    private IvyPay ivyPay;
    private IvyPayEvent ivyPayEvent;
    private Builder builder;

    private IFirebase firebaseImpl;

    // uac
    private UAC uac;
    private double adPingThreshold = 0.1;

    // 快手
    private KwaiUtil kwaiUtil;

    // 登录平台初始化标记
    private final AtomicBoolean authPlatformsInitializeStatus = new AtomicBoolean(false);

    private Handler mainHandler = new Handler(Looper.getMainLooper());
//    private AtomicBoolean isGoogleInstallRefferrStartLoad = new AtomicBoolean(false);

    // AF 归因优先初始化广告：归因在超时内回来则立即初始化，否则超时后兜底初始化
    private final AtomicBoolean blastAdsInitialized = new AtomicBoolean(false);
    private Runnable blastAdsTimeoutRunnable = null;

    private SessionManager sessionManager = null;

    private IvySdk() {
        // 私有构造函数，确保单例
    }

    public void onCreate(Activity activity) {
        TimeUtil.getInstance().initTime();
        App.setInstance(activity.getApplication());
        ActivityUtil.getInstance().setActivity(activity);
        sessionManager = new SessionManager();
        sessionManager.start();
        LocalStorage.Instance.init();
        AdvertisingIdManager.get(activity.getApplicationContext(), null);
        loadMetaConfig();
        displayInNotch();
        onNewIntent(activity.getIntent());
        recordFirstTimeStamp();
        IvyABManager.getInstance().updateInstallData();
        LocalAB.initWithContext(activity);

        if (debug) {
            TestManager.getInstance().setup(debug, new TestManager.FuncCallback() {
                @Override
                public void clearReframeRecord() {
                    ReframeUtil.getInstance().clear();
                }

                @Override
                public void onEvent(String event, Bundle params) {
                    IvySdk.getInstance().logEvent(event, params);
                }

                @Override
                public String getActivedAbInfo() {
                    return IvyABManager.getInstance().getAbHitResults();
                }

                @Override
                public boolean enterAbVariants(String varid) {
                    return LocalAB.activateVariant(App.getInstance(), varid);
                }

                @Override
                public String getAllAbConfig() {
                    return IvyABManager.getInstance().getAllAbConfigs();
                }

                @Override
                public void auth(String platform, int action) {
                    if (platform.equalsIgnoreCase("facebook")) {
                        if (action == 1) {
                            IvySdk.getInstance().loginFacebook();
                        } else {
                            IvySdk.getInstance().logoutFacebook();
                        }
                    } else if (platform.equalsIgnoreCase("google")) {
                        if (action == 1) {
                            IvySdk.getInstance().loginGoogle();
                        } else {
                            IvySdk.getInstance().logoutGoogle();
                        }
                    } else if (platform.equalsIgnoreCase("playgames")) {
                        if (action == 1) {
                            IvySdk.getInstance().loginPlayGames();
                        } else {
                            IvySdk.getInstance().logoutPlayGames();
                        }
                    }
                }

                @Override
                public void firebaseAuth(String platform, int action) {
                    if (action == 1) {
                        if (platform.equalsIgnoreCase("Facebook")) {
                            IvySdk.getInstance().loginWithFacebook(new IAuthResponse() {
                                @Override
                                public void onLoginResult(String platform, boolean status, String channel, String reason) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", channel + "绑定" + (status ? "成功" : "失败"), status ? EventState.NORMAL : EventState.ERROR, "" + reason);
                                    // TestManager.getInstance().endTrace();
                                }
                            });
                        } else if (platform.equalsIgnoreCase("Google")) {
                            IvySdk.getInstance().loginWithGoogle(new IAuthResponse() {
                                @Override
                                public void onLoginResult(String platform, boolean status, String channel, String reason) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", channel + "绑定" + (status ? "成功" : "失败"), status ? EventState.NORMAL : EventState.ERROR, "" + reason);
                                    // TestManager.getInstance().endTrace();
                                }
                            });
                        } else if (platform.equalsIgnoreCase("PlayGames")) {
                            IvySdk.getInstance().loginWithPlayGames(new IAuthResponse() {
                                @Override
                                public void onLoginResult(String platform, boolean status, String channel, String reason) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", channel + "绑定" + (status ? "成功" : "失败"), status ? EventState.NORMAL : EventState.ERROR, "" + reason);
                                    // TestManager.getInstance().endTrace();
                                }
                            });
                        } else if (platform.equalsIgnoreCase("匿名")) {
                            IvySdk.getInstance().loginAnonymous(new IAuthResponse() {
                                @Override
                                public void onLoginResult(String platform, boolean status, String channel, String reason) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", channel + "绑定" + (status ? "成功" : "失败"), status ? EventState.NORMAL : EventState.ERROR, "" + reason);
//                                    TestManager.getInstance().endTrace();
                                }
                            });
                        } else if (platform.equalsIgnoreCase("email")) {
                            IvySdk.getInstance().loginWithEmailAndPassword(Util.roleId() + "@gmail.com", "123456", new IAuthResponse() {
                                @Override
                                public void onLoginResult(String platform, boolean status, String channel, String reason) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", channel + "绑定" + (status ? "成功" : "失败"), status ? EventState.NORMAL : EventState.ERROR, "" + reason);
                                    // TestManager.getInstance().endTrace();
                                }
                            });
                        }
                    } else if (action == 2) {
                        IvySdk.getInstance().logoutFirebase();
//                        IvySdk.getInstance().unlinkFirebaseWithChannel();
                    } else if (action == 3) {
                        if (platform.equalsIgnoreCase("Facebook")) {
                            IvySdk.getInstance().unlinkFirebaseWithChannel("facebook", new IFirebaseUnlink() {
                                @Override
                                public void onUnlinked(String unlinkChannel, boolean status, String reason) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", unlinkChannel + "解绑" + (status ? "成功" : "失败"), status ? EventState.NORMAL : EventState.ERROR, "" + reason);
                                    // TestManager.getInstance().endTrace();
                                }
                            });
                        } else if (platform.equalsIgnoreCase("google")) {
                            IvySdk.getInstance().unlinkFirebaseWithChannel("Google", new IFirebaseUnlink() {
                                @Override
                                public void onUnlinked(String unlinkChannel, boolean status, String reason) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", unlinkChannel + "解绑" + (status ? "成功" : "失败"), status ? EventState.NORMAL : EventState.ERROR, "" + reason);
                                    //  TestManager.getInstance().endTrace();
                                }
                            });
                        } else if (platform.equalsIgnoreCase("PlayGames")) {
                            IvySdk.getInstance().unlinkFirebaseWithChannel("playgames", new IFirebaseUnlink() {
                                @Override
                                public void onUnlinked(String unlinkChannel, boolean status, String reason) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", unlinkChannel + "解绑" + (status ? "成功" : "失败"), status ? EventState.NORMAL : EventState.ERROR, "" + reason);
                                    // TestManager.getInstance().endTrace();
                                }
                            });
                        } else if (platform.equalsIgnoreCase("匿名")) {
                            IvySdk.getInstance().unlinkFirebaseWithChannel("anonymous", new IFirebaseUnlink() {
                                @Override
                                public void onUnlinked(String unlinkChannel, boolean status, String reason) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", unlinkChannel + "解绑" + (status ? "成功" : "失败"), status ? EventState.NORMAL : EventState.ERROR, "" + reason);
                                    // TestManager.getInstance().endTrace();
                                }
                            });
                        } else if (platform.equalsIgnoreCase("email")) {
                            IvySdk.getInstance().unlinkFirebaseWithChannel("email", new IFirebaseUnlink() {
                                @Override
                                public void onUnlinked(String unlinkChannel, boolean status, String reason) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", unlinkChannel + "解绑" + (status ? "成功" : "失败"), status ? EventState.NORMAL : EventState.ERROR, "" + reason);
                                    // TestManager.getInstance().endTrace();
                                }
                            });
                        }
                    }
                }

                @Override
                public void pay() {
                    if (ivyPay != null) {
                        int id = ivyPay.randomBillId();
                        if (id < 0) {
                            TestManager.getInstance().addEvent("Payment", "开始付费失败，找不到合法billId，检查配置", EventState.ERROR, "");
                            return;
                        }
                        TestManager.getInstance().addEvent("Payment", "开始付费，billid=" + id, EventState.NORMAL, "");
                        IvySdk.getInstance().pay(id, null, null);
                    } else {
                        TestManager.getInstance().addEvent("Payment", "计费未初始化，检查配置", EventState.ERROR, "");
                    }
                }

                @Override
                public void adTask(int taskId) {
                    if (taskId == 1) {
                        if (hasInterstitialAd("mock", null)) {
                            showInterstitialAd("mock", "mock", null);
                        } else {
                            IToast.toastAnyway("当前无广告");
                        }
                    } else if (taskId == 2) {
                        if (hasRewardedAd("mock", null)) {
                            showRewardedAd("mock", "mock", null);
                        } else {
                            IToast.toastAnyway("当前无广告");
                        }
                    } else if (taskId == 3) {
                        if (hasBannerAd("mock", null)) {

                        } else {
                            IToast.toastAnyway("当前无广告");
                        }
                        showBannerAd(0, "mock", "mock", null);
                    } else if (taskId == 4) {
                        closeBannerAd("mock");
                    }
                }

                @Override
                public void translate(String src) {
                    TranslateUtil.translate("", "en", src, new TranslateUtil.Action() {
                        @Override
                        public void onResult(String from, String dst) {
                            try {
                                JSONObject json = new JSONObject();
                                json.put("src", from);
                                json.put("dst", dst);
                                // sendMessage("onTranslateResult", json.toString());
                                ILog.d(TAG, json.toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void onPlayGameStats(String eventId, JSONObject jsonObject) {
                    submitPlayGameEvent(eventId, jsonObject.toString());
                }

                @Override
                public void updateAchievement(String achievementId, int step, boolean unlock) {
                    if (unlock) {
                        IvySdk.getInstance().unlockAchievement(achievementId);
                    } else {
                        IvySdk.getInstance().setAchievementStep(achievementId, step);
                    }
                }

            });
            TestManager.getInstance().addEvent("IVY", "sdk启动", EventState.NORMAL, "");
        }

        //LogMonitor.startMonitoring();
    }

    private void recordFirstTimeStamp() {
        int openTimes = LocalStorage.Instance.decodeInt(IKeys.KEY_APP_START_TIMES, 0);
        if (openTimes == 0) {
            LocalStorage.Instance.encodeLong("_sdk_first_time_", getTodayStartTime());
        }
    }

    private void resetLocalAB(Activity activity) {
        // AF 归因回来，持久化标记（供后续启动直接跳过超时）
        LocalStorage.Instance.encodeBoolean("af_attributed", true);
        // 立即触发广告初始化（如果还未初始化）
        ILog.i(TAG, "AF attribution received, triggering initBlastAds");
        triggerInitBlastAds();
        LocalAB.initWithContext(activity);
        TestManager.getInstance().addEvent("IVY", "ab 分配完成", EventState.NORMAL, "");
        if (builder != null && builder.remoteConfigSyncListener != null) {
            builder.remoteConfigSyncListener.onAbTestConfigsSynced();
        }
    }

    public void onCreate(Activity activity, Builder builder) {
        this.builder = builder;
        ILog.i(TAG, "start do onCreate");
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadPlayInstallReferrer();
            }
        }, 10 * 1000);

//        new IPUtil().startLoadIP();
        IvyRemoteConfig.getInstance().loadLocalConfig(json -> {
            ILog.i(GridManager.TAG, "load cached app config success");
            TestManager.getInstance().addEvent("IVY", "default.json 读取结果", EventState.NORMAL, json.toString());
            GridManager.getInstance().setData(json);
            TrajectoryManager.getInstance().triggerUploadWorker();
            checkLowDevices(json);
            GridManager.getInstance().parseConfig(new GridManager.IGrid() {
                @Override
                public void onPayment(String data, String enjoy_pay_data) {
                    super.onPayment(data, enjoy_pay_data);
                    ILog.i(TAG, "payment config:");
                    ILog.i(TAG, ": " + data);
                    TestManager.getInstance().addEvent("IVY", "计费点配置", EventState.NORMAL, data);
                    if (data != null) {
                        if (ivyPay == null) ivyPay = new IvyPay();
                        ivyPay.setup(appId, data, enjoy_pay_data, debug, IvySdk.this.purchaseResponseListener);
                    } else {
                        ILog.w(TAG, "payment config invalid");
                    }
                }

                @Override
                public void onGameServices(String data) {
                    super.onGameServices(data);
                    ILog.i(TAG, "game services config:");
                    ILog.i(TAG, ": " + data);
                    TestManager.getInstance().addEvent("IVY", "游戏服务配置", EventState.NORMAL, data);
                    if (data != null) {
                        if (ivyGames == null) {
                            ivyGames = new IvyGames();
                        }
                        ivyGames.setup(appId, data, authResult, playGamesListener, debug);
                    } else {
                        ILog.w(TAG, "no game services configured");
                    }
                }

                @Override
                public void onTrackConfig(String data) {
                    super.onTrackConfig(data);
                    ILog.i(TAG, "track config:");
                    ILog.i(TAG, ": " + data);
                    TestManager.getInstance().addEvent("IVY", "事件打点配置", EventState.NORMAL, data);
                    if (data != null) {
                        setUserProperty("appId", appId, TrackPlatform.NONE);
                        if (LocalStorage.Instance.containsKey(IKeys.KEY_PAYING_USER)) {
                            setUserProperty("paying_user", "1", null);
                        }
                        IvyTrack.getInstance().setup(App.getInstance(), appId, data, debug, new IConversationCallback() {
                            @Override
                            public void onConversionDataSuccess(Map<String, Object> var1) {
                                IvyABManager.getInstance().updateInstallData();

                                IvyABManager.getInstance().loadAbTestConfigs(appId, debug, state -> {
                                    ILog.i(TAG, "refresh ab data in Conversion Data Success callback");
                                    if (state) {
                                        logEvent("sdk_ab_pull_successed", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, null);
                                    }
                                });

                                try {
                                    boolean hasReloadRemoteConfig = LocalStorage.Instance.decodeBoolean("reSyncRemoteConfig", false);
                                    boolean isNewUser = LocalStorage.Instance.decodeBoolean(IKeys.KEY_APP_NEW_USER, false);
                                    if (isNewUser && !hasReloadRemoteConfig) {
                                        LocalStorage.Instance.encodeBoolean("reSyncRemoteConfig", true);
                                        Map<String, Object> headerInfo = IvyABManager.getInstance().getHeaderInfo();
                                        IvyRemoteConfig.getInstance().loadRemoteData(appId, debug, json, headerInfo, (success, e) -> {
                                            resetLocalAB(activity);
                                            IvyABManager.getInstance().updateInstallData();
                                        });
                                    }
                                } catch (Exception ignored) {
                                }
                            }

                            @Override
                            public void onConversionDataFail(String var1) {

                            }

                            @Override
                            public void onAppOpenAttribution(Map<String, String> var1) {
                            }

                            @Override
                            public void onAttributionFailure(String var1) {
                            }
                        }, () -> {
                            if (ivyPayEvent == null) {
                                ivyPayEvent = new IvyPayEvent();
                            }
                            ivyPayEvent.start();

                        });
                    } else {
                        ILog.w(TAG, "track platform config invalid");
                    }
                    IvyRemoteConfig.getInstance().loadUserInfo(appId, null);
                }

                @Override
                public void onCustomerServiceConfig(String data) {
                    super.onCustomerServiceConfig(data);
                    TestManager.getInstance().addEvent("IVY", "AIHelp配置", EventState.NORMAL, data);
                    if (data != null) {
                        if (ivyHelper == null) ivyHelper = new IvyHelper();
                        ivyHelper.setup(data, debug, customerEvents);
                    } else {
                        ILog.w(TAG, "customer config invalid");
                    }
                }

                @Override
                public void onGridRemoteConfig(String data) {
                    super.onGridRemoteConfig(data);
                    ILog.i(TAG, "grid remote config config:");
                    ILog.i(TAG, ": " + data);
                    TestManager.getInstance().addEvent("IVY", "兜底RemoteConfig配置", EventState.NORMAL, data);
                    if (data != null) {
                        try {
                            Map<String, Object> map = new HashMap<>();
                            JSONObject rcJson = new JSONObject(data);
                            Iterator<String> keys = rcJson.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                map.put(key, rcJson.opt(key));
                            }
                            IvyRemoteConfig.getInstance().setDefaultData(map);
                        } catch (Exception e) {
                            ILog.e(TAG, "set default remote config failed:" + e.getMessage());
                        }
                    }
                    setupDefaultRemoteData(data);
                }

                @Override
                public void onAdConfig(String data) {
                    super.onAdConfig(data);
                    ILog.i(TAG, "ad config:");
                    ILog.i(TAG, ": " + data);
//                    initAds(null);
//                    setBXId("old");
                }

                @Override
                public void onData() {
                    super.onData();
                    initUac();
                    adPingThreshold = GridManager.getInstance().getGridConfig("adPingThreshold", 0.1);
                    initKwai();
                }
            });

            new Thread(() -> {
                int totalWaitTime = 0;
                ILog.i(TAG, "waiting install data for load ivy remote config");
                while (!LocalStorage.Instance.containsKey("cache_install_data") && totalWaitTime < 3000) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                    totalWaitTime += 1000;
                }
                mainHandler.post(() -> {
                    ILog.i(TAG, "start load ivy remote config");
                    IvyABManager.getInstance().updateInstallData();
                    Map<String, Object> headerInfo = IvyABManager.getInstance().getHeaderInfo();
                    IvyRemoteConfig.getInstance().loadRemoteData(appId, debug, json, headerInfo, (success, result) -> {
                        resetLocalAB(activity);
                        IvyABManager.getInstance().updateInstallData();
                        if (builder != null && builder.remoteConfigSyncListener != null) {
                            builder.remoteConfigSyncListener.onRemoteConfigSynced();
                        }
                    });
                });
            }).start();
        });
        setupFirebase();
        onAppStart();
        scheduleInitBlastAds();


//        appOpenEvent(1);
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showUpdateDialogIfNeeded();
            }
        }, 2000);

        FCMMessage.setMessager(new FCMMessager());
        long now = SystemClock.elapsedRealtime();
        lastTriggerInterTime = now;
        lastTriggerVideoTime = now;

    }

    private void checkLowDevices(JSONObject data) {
        if (LocalStorage.Instance.containsKey("low_device")) {
            return;
        }
        JSONObject ads = data.optJSONObject("ads");
        if (ads == null) {
            return;
        }
        JSONArray lowDevices = ads.optJSONArray("low_devices");
        if (lowDevices == null) {
            return;
        }

        String tt = Build.DEVICE;
        String ts = Build.MODEL;
        for (int i = lowDevices.length() - 1; i >= 0; i--) {
            String lowDevice = lowDevices.optString(i);
            if (tt.equals(lowDevice) || ts.equals(lowDevice)) {
                LocalStorage.Instance.encodeString("low_device", "yes");
                return;
            }
        }
        LocalStorage.Instance.encodeString("low_device", "no");
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        return intent != null;
    }

    private void showUpdateDialogIfNeeded() {
        try {
            JSONObject updateConfig = new JSONObject(getRemoteConfigString("sdk_update"));
            Activity main = ActivityUtil.getInstance().getActivity();
            if (updateConfig.optBoolean("enable", false)) {
                final String pkg = updateConfig.getString("package");
                if (isAppInstalled(main, pkg)) {
                    return;
                }
                final String url = updateConfig.getString("url");
                AlertDialog dialog = new AlertDialog.Builder(main, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert).setTitle("New Version Available!").setMessage("Update now to enjoy the latest improvements.").setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppStoreUtil.openAppStore(url, "");
                    }
                }).setCancelable(false).create();
                dialog.show();
                Window dialogWindow = dialog.getWindow();
                if (dialogWindow != null) {
                    dialogWindow.setWindowAnimations(0);
                    View decorView = dialogWindow.getDecorView();
                    decorView.setScaleX(0.1f);
                    decorView.setScaleY(0.1f);
                    decorView.animate().scaleX(1).scaleY(1).setInterpolator(new OvershootInterpolator()).setDuration(1000).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUserInfo(String key) {
        Map<String, Object> m = LocalAB.userInfo(App.getInstance(), false);
        return String.valueOf(m.get(key));
    }

    public void onStart() {
    }

    public void onResume() {
//        IvyAds.getInstance().onResume();
    }

    public void onPause() {
//        IvyAds.getInstance().onPause();
    }

    public void onStop() {
    }

    public void onDestroy() {
        if (ivyPayEvent != null) {
            ivyPayEvent.destroy();
        }
        TestManager.getInstance().destroy();
        if (sessionManager != null) sessionManager.stop();
//        IvyAds.getInstance().onDestroy();
    }

    public void setSessionInterval(long interval) {
        if (sessionManager != null) {
            sessionManager.updateSessionInterval(interval);
        }
    }

    public String getSessionId() {
        return sessionManager == null ? "" : sessionManager.currentSessionId();
    }

    private void handleFCMData(Intent intent) {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    //上报通知
                    try {
                        String payload = intent.getStringExtra("payload");
                        Bundle bundle = null;
                        if (TextUtils.isEmpty(payload)) {
                            bundle = intent.getExtras();
                        } else {
                            bundle = FCMMessager.getBundle(payload);
                        }
                        if (bundle != null) {
                            if (bundle.keySet().contains("tid")) {
                                IvySdk.getInstance().logEvent(EventIDs.EVENT_PUSH_REWARDED, bundle);
                            }
                        }
                    } catch (Exception ignored) {
                    }
//                    String fcm_action = intent.getStringExtra("fcm_action");
//                    if (!TextUtils.isEmpty(fcm_action)) {
//                        if (builder != null && builder.notificationEvent != null) {
//                            builder.notificationEvent.onReceivedNotificationAction(fcm_action);
//                        }
//                        LocalStorage.Instance.encodeString(IKeys.KEY_NOTIFICATION_ACTION, fcm_action, 15 * 60);
//                    }
                } catch (Throwable ignored) {
                }
            }
        }, 1000);
    }

    public void onNewIntent(Intent intent) {
        ILog.d("FCM", "onNewIntent 111");
        if (intent != null) {
            ILog.d("FCM", "onNewIntent 222");
            if (intent.hasExtra("notification_action")) {
                ILog.d("FCM", "onNewIntent 333");
                mainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String action = intent.getStringExtra("notification_action");
                            if (action != null) {
                                ILog.i(TAG, "received notification event:" + action);
                                if (builder != null && builder.notificationEvent != null) {
                                    builder.notificationEvent.onReceivedNotificationAction(action);
                                }
                                LocalStorage.Instance.encodeString(IKeys.KEY_NOTIFICATION_ACTION, action, 15 * 60);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }, 1000);
            } else if (intent.hasExtra("shortcut_action")) {
                String shortcut_id = intent.getStringExtra("shortcut_id");
                ILog.d("FCM", "onNewIntent 666:" + shortcut_id);
                ShortcutUtil.setShortcutAction(shortcut_id);
            } else {
                ILog.d("FCM", "onNewIntent 555");
                handleFCMData(intent);
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (ivyGames != null) {
            ivyGames.onActivityResult(requestCode, resultCode, data);
        }
        if (ivyPay != null) {
            ivyPay.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        NotificationPermissionUtil.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onAppStart() {
        int openTimes = LocalStorage.Instance.decodeInt(IKeys.KEY_APP_START_TIMES, 0) + 1;
        if (openTimes == 1) {
            IvySdk.getInstance().logEvent(EventIDs.SDK_FIRST_APP_OPEN, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK);
            List<TrackPlatform> platforms = new ArrayList<>();
            platforms.add(TrackPlatform.THINKING_DATA);
            IvySdk.getInstance().logEvent(EventIDs.SDK_FIRST_APP_OPEN_IVY, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, null, platforms);
            LocalStorage.Instance.encodeBoolean(IKeys.KEY_APP_NEW_USER, true);
        } else {
            LocalStorage.Instance.encodeBoolean(IKeys.KEY_APP_NEW_USER, false);
        }
        LocalStorage.Instance.encodeInt(IKeys.KEY_APP_START_TIMES, openTimes);
        HashMap<String, Object> params = new HashMap<>();
        params.put("times", openTimes);
        IvySdk.getInstance().logEvent(EventIDs.EVENT_APP_OPEN, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params);
        List<TrackPlatform> platforms = new ArrayList<>();
        platforms.add(TrackPlatform.THINKING_DATA);
        IvySdk.getInstance().logEvent(EventIDs.EVENT_APP_OPEN_IVY, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params, platforms);

    }

    private long getTodayStartTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public void loadCountryCode(final Consumer<String> callback) {
        String countryCode = LocalStorage.Instance.decodeString("cache_country_code", "");
        if (countryCode == null || countryCode.isEmpty()) {
            IvyRemoteConfig.getInstance().loadUserInfo(appId, code -> {
                if (code != null) {
                    IvyABManager.getInstance().setHeaderInfo("country_code", code);
                    callback.accept(code);
                } else {
                    callback.accept(Util.countryCode());
                }
            });
        } else {
            IvyABManager.getInstance().setHeaderInfo("country_code", countryCode);
            callback.accept(countryCode);
        }
    }

    public String getMediaSource() {
        try {
            return LocalStorage.Instance.decodeString(IKeys.KEY_AF_MEDIA_SOURCE, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getCampaignName() {
        try {
            return LocalStorage.Instance.decodeString(IKeys.KEY_AF_CAMPAIGN, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getAdSetName() {
        try {
            return LocalStorage.Instance.decodeString(IKeys.KEY_AF_AD_SET, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void enableAbTest(int channel) {
        ILog.i(TAG, "ab channel:" + channel);
        if (channel == 0) {
//            IvyTrack.getInstance().updateHuoShanCacheTag(null);
        }
        IvyABManager.getInstance().enableAbTest(channel, debug);
    }

    public void setHeaderInfo(String json) {
        try {
            ILog.i(TAG, "add header info:" + json);
            if (json == null) {
                throw new IllegalArgumentException("illegal head info data");
            }
            JSONObject jsonObject = new JSONObject(json);
            Iterator<String> iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                Object value = jsonObject.opt(key);
                if (value != null) {
                    IvyABManager.getInstance().setHeaderInfo(key, value);
                    IvyTrack.getInstance().setUserProperty(key, value);
                }
            }
        } catch (Exception e) {
            ILog.i(TAG, "add head info error:" + e.getMessage());
        }
    }

    public void setUserInfo(String json) {
        try {
            ILog.i(TAG, "add user info:" + json);
            if (json == null) {
                throw new IllegalArgumentException("illegal user info data");
            }
            JSONObject jsonObject = new JSONObject(json);
            Iterator<String> iterator = jsonObject.keys();
            Map<String, Object> params = new HashMap<>();
            while (iterator.hasNext()) {
                String key = iterator.next();
                Object value = jsonObject.opt(key);
                if (value != null) {
                    IvyTrack.getInstance().setUserProperty(key, value);
                    params.put(key, value);
                    IvyABManager.getInstance().setUserInfo(key, value);
                }
            }
            IvyTrack.getInstance().logEvent("sdk_user_info", EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params);
            TestManager.getInstance().addEvent("IVY", "设置用户属性", EventState.NORMAL, json);
        } catch (Exception e) {
            ILog.i(TAG, "add user info error:" + e.getMessage());
        }
    }

    public void startTrackEvent() {
        ILog.i(TAG, "call start track event");
        logEvent(EventIDs.EVENT_START_TRACK_EVENT, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK);
        IvyABManager.getInstance().setUserInfo("device_model", Build.MANUFACTURER + "_" + Build.MODEL);
        IvyABManager.getInstance().setUserInfo("android_api_version", Build.VERSION.SDK_INT);
        IvyABManager.getInstance().setUserInfo("android_version", Build.VERSION.RELEASE);
        IvyABManager.getInstance().setUserInfo("cores", Runtime.getRuntime().availableProcessors());
        IvyABManager.getInstance().setUserInfo("ram", Util.byte2GB(Util.totalMemory()));
        IvyABManager.getInstance().setUserInfo("disk", Util.byte2GB(Util.totalDiskSize()));
        IvyABManager.getInstance().updateInstallData();
        IvyABManager.getInstance().loadAbTestConfigs(appId, debug, state -> {
            ILog.i(TAG, "ab test ssid = " + getAbtestSSID());
            if (builder != null && builder.remoteConfigSyncListener != null) {
                builder.remoteConfigSyncListener.onAbTestConfigsSynced();
            }
        });
    }

    public void pullAbTestConfigs() {
        ILog.i(TAG, "call pullAbTestConfigs");
        IvyABManager.getInstance().updateInstallData();
        IvyABManager.getInstance().loadAbTestConfigs(appId, debug, state -> {
            if (builder != null && builder.remoteConfigSyncListener != null) {
                builder.remoteConfigSyncListener.onAbTestConfigsSynced();
            }
        });
    }

    public String getAbtestSSID() {
        return IvyABManager.getInstance().getAbtestSSID();
    }

    private void initAds(JSONObject data) {
//        if (adInitializedTag.get()) {
//            return;
//        }
//        adInitializedTag.set(true);
//
//        try {
//            String jsonStr = IvyABManager.Instance.getAbConfigString("ads", "");
//            if (jsonStr == null || jsonStr.isEmpty() || "null".equals(jsonStr)) {
//                jsonStr = GridManager.Instance.getGridConfig("ads", "");
//            }
//            ILog.i(TAG, "calculate ad config:");
//            ILog.i(TAG, String.valueOf(jsonStr));
//
//            String blastAdsStr = IvyABManager.Instance.getAbConfigString("blockads", "");
//            if (blastAdsStr == null || blastAdsStr.isEmpty() || "null".equals(blastAdsStr)) {
//                blastAdsStr = GridManager.Instance.getGridConfig("blockads", "");
//            }
//            if (blastAdsStr != null && blastAdsStr.isEmpty()) {
//                blastAdsStr = null;
//            }
//            ILog.i(TAG, "calculate blast ad config:");
//            ILog.i(TAG, String.valueOf(blastAdsStr));
//
//            IvyAds.Instance.setup(jsonStr, blastAdsStr, debug, IvyRemoteConfig.Instance, adListener, preloadTag -> {
//                setUserProperty(EventParams.EVENT_PRELOAD_TAG, preloadTag.toString(), null);
//                setupFirebaseDataAnalytics();
//                return null;
//            }, segment -> {
//                if (segment != null) {
//                    IvyTrack.Instance.setUserProperty("segment", segment);
//                }
//                return null;
//            });
//        } catch (Exception e) {
//            IvyTrack.Instance.advertisingProcess(2);
//        }
    }

    public boolean getAbConfigBoolean(String key, boolean defaultValue) {
        return IvyABManager.getInstance().getAbConfigBoolean(key, defaultValue);
    }

    public int getAbConfigInt(String key, int defaultValue) {
        return IvyABManager.getInstance().getAbConfigInt(key, defaultValue);
    }

    public double getAbConfigDouble(String key, double defaultValue) {
        return IvyABManager.getInstance().getAbConfigDouble(key, defaultValue);
    }

    public long getAbConfigLong(String key, long defaultValue) {
        return IvyABManager.getInstance().getAbConfigLong(key, defaultValue);
    }

    public float getAbConfigFloat(String key, float defaultValue) {
        return IvyABManager.getInstance().getAbConfigFloat(key, defaultValue);
    }

    public String getAbConfigString(String key, String defaultValue) {
        return IvyABManager.getInstance().getAbConfigString(key, defaultValue);
    }

    public int loadNotificationPermissionState() {
        return NotificationPermissionUtil.loadNotificationPermissionState();
    }

    public void requestNotificationPermission() {
        NotificationPermissionUtil.requestNotificationPermission();
    }

    public void openNotificationSettings() {
        NotificationPermissionUtil.openNotificationSettings();
    }

    public void pushNotificationTask(String tag, String title, String subtitle, String bigText, String smallIcon, String largeIcon, String bigPicture, long pushTime, long interval, boolean autoCancel, String action, boolean repeat) {
        NotificationTask task = new NotificationTask(tag, title, subtitle, bigText, smallIcon, largeIcon, bigPicture, pushTime, interval, autoCancel, action, repeat);
        NotificationTaskManager.getInstance().addTask(App.getInstance(), task, false);
        long _delay = pushTime - System.currentTimeMillis();
        if (_delay <= 10) {
            _delay = 10;
            NotificationReminderScheduler.startOneTimeReminder(App.getInstance(), _delay);
        } else {
            if (_delay < 15 * 60 * 1000) {
                NotificationReminderScheduler.startOneTimeReminder(App.getInstance(), _delay);
            } else {
                NotificationReminderScheduler.startPeriodicReminder(App.getInstance());
            }
        }
    }

    public void cancelNotification(String tag) {
        if (tag != null) {
            NotificationTaskManager.getInstance().removeTask(App.getInstance(), tag, false);
        } else {
            NotificationTaskManager.getInstance().removeAllTask(App.getInstance(), false);
        }
    }

    public void addBootNotification(String tag, String title, String subtitle, String bigText, String smallIcon, String largeIcon, String bigPicture, String action) {
        NotificationTask task = new NotificationTask(tag, title, subtitle, bigText, smallIcon, largeIcon, bigPicture, 0, 0, true, action, false);
        NotificationTaskManager.getInstance().addTask(App.getInstance(), task, true);
    }

    public void removeBootNotification(String tag) {
        if (tag != null) {
            NotificationTaskManager.getInstance().removeTask(App.getInstance(), tag, true);
        } else {
            NotificationTaskManager.getInstance().removeAllTask(App.getInstance(), true);
        }
    }

    private void loadPlayInstallReferrer() {
//        InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(App.getInstance()).build();
//        referrerClient.startConnection(new InstallReferrerStateListener() {
//            @Override
//            public void onInstallReferrerSetupFinished(int responseCode) {
//                try {
//                    if (responseCode != InstallReferrerClient.InstallReferrerResponse.OK) {
//                        referrerClient.endConnection();
//                        ILog.i(TAG, "load play install referrer failed:service unavailable");
//                        return;
//                    }
//
//                    new Thread(() -> {
//                        ReferrerDetails response = null;
//                        try {
//                            response = referrerClient.getInstallReferrer();
//                        } catch (Exception ignored) {
//                        } finally {
//                            try {
//                                referrerClient.endConnection();
//                            } catch (Exception ignored) {
//                            }
//                        }
//                        try {
//                            if (response == null) {
//                                return;
//                            }
//                            String referrerUrl = response.getInstallReferrer();
//                            long referrerClickTime = response.getReferrerClickTimestampSeconds();
//                            long appInstallTime = response.getInstallBeginTimestampSeconds();
//                            boolean instantExperienceLaunched = response.getGooglePlayInstantParam();
//                            if (TextUtils.isEmpty(referrerUrl)) {
//                                return;
//                            }
////                            JSONObject body = new JSONObject();
////                            body.put("referrerUrl", referrerUrl);
////                            body.put("referrerClickTime", referrerClickTime);
////                            body.put("appInstallTime", appInstallTime);
////                            body.put("instantExperienceLaunched", instantExperienceLaunched);
////                            OkHttpClient okHttpClient = HttpUtil.getInstance().getOkHttpClient();
////                            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
////                            RequestBody postBody = RequestBody.create(body.toString(), mediaType);
////                            Request request = new Request.Builder().url("http://216.227.142.162:8080/data").post(postBody).build();
////                            okHttpClient.newCall(request).execute();
//                        } catch (Exception ignored) {
//
//                        }
//                    }).start();
//                } catch (Exception ignore) {
//                }
//            }
//
//            @Override
//            public void onInstallReferrerServiceDisconnected() {
//
//            }
//        });

//        if (isGoogleInstallRefferrStartLoad.compareAndSet(false, true)) {
//            new Thread(() -> {
//                InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(App.getInstance()).build();
//                referrerClient.startConnection(new InstallReferrerStateListener() {
//                    @Override
//                    public void onInstallReferrerSetupFinished(int responseCode) {
//                        try {
//                            if (responseCode != InstallReferrerClient.InstallReferrerResponse.OK) {
//                                referrerClient.endConnection();
//                                ILog.i(TAG, "load play install referrer failed:service unavailable");
//                                return;
//                            }
//                            Executors.newSingleThreadExecutor().execute(new Runnable() {
//                                @Override
//                                public void run() {
//                                    ReferrerDetails response = null;
//                                    try {
//                                        response = referrerClient.getInstallReferrer();
//                                    } catch (Exception ignored) {
//                                    } finally {
//                                        try {
//                                            referrerClient.endConnection();
//                                        } catch (Exception ignored) {
//                                        }
//                                    }
//                                    try {
//                                        if (response == null) {
//                                            return;
//                                        }
//                                        String referrerUrl = response.getInstallReferrer();
//                                        long referrerClickTime = response.getReferrerClickTimestampSeconds();
//                                        long appInstallTime = response.getInstallBeginTimestampSeconds();
//                                        boolean instantExperienceLaunched = response.getGooglePlayInstantParam();
//                                        if (TextUtils.isEmpty(referrerUrl)) {
//                                            return;
//                                        }
//                                        JSONObject body = new JSONObject();
//                                        body.put("referrerUrl", referrerUrl);
//                                        body.put("referrerClickTime", referrerClickTime);
//                                        body.put("appInstallTime", appInstallTime);
//                                        body.put("instantExperienceLaunched", instantExperienceLaunched);
//                                        OkHttpClient okHttpClient = HttpUtil.getInstance().getOkHttpClient();
//                                        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
//                                        RequestBody postBody = RequestBody.create(body.toString(), mediaType);
//                                        Request request = new Request.Builder().url("http://216.227.142.162:8080/data").post(postBody).build();
//                                        okHttpClient.newCall(request).execute();
//                                    } catch (Exception ignored) {
//
//                                    } finally {
//
//                                    }
//                                }
//                            });
//                        } catch (Exception ignored) {
//                        }
//                    }
//
//                    @Override
//                    public void onInstallReferrerServiceDisconnected() {
//                        // 可以在下次调用时重试连接
//                    }
//                });
//            }).start();
//        }
    }

//    private void logAdTriggerEvent(AdType adType, String placement, boolean state, String clientInfo) {
//        Map<String, Object> params = Util.formatAdClientInfo(clientInfo);
//        if (params == null) {
//            params = new HashMap<>();
//        }
//        params.put(EventParams.EVENT_PARAM_AD_PLACEMENT, placement);
//        params.put(EventParams.EVENT_PARAM_AD_FORMAT, adType.getValue());
//        params.put(EventParams.EVENT_PARAM_STATE, state ? 1 : 0);
//
//        logEvent(EventIDs.AD_TRIGGER_EVENT, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params);
//    }

//    private void logAdEvent(AdType adType, String placement, boolean state, String clientInfo) {
//        Map<String, Object> params = Util.formatAdClientInfo(clientInfo);
//        if (params == null) {
//            params = new HashMap<>();
//        }
//        params.put(EventParams.EVENT_PARAM_AD_PLACEMENT, placement);
//        params.put(EventParams.EVENT_PARAM_AD_FORMAT, adType.getValue());
//        params.put(EventParams.EVENT_PARAM_STATE, state ? 1 : 0);
//
//        logEvent(EventIDs.AD_REACH_PAGE, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params);
//    }


    @Override
    public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms) {
        IvyTrack.getInstance().logEvent(eventName, eventType, eventSrc, params, platforms);

        if (EventType.EVENT_TYPE_AD_REVENUE.equals(eventType)) {
            collectAdRevenue(params);
            if (kwaiUtil != null && params != null) {
                Object revenueObj = params.get(EventParams.EVENT_PARAM_REVENUE);
                if (revenueObj instanceof Double) {
                    kwaiUtil.checkAdRevenue((Double) revenueObj);
                }
            }
        }

        if (EventType.EVENT_TYPE_COMMON.equals(eventType) && EventSrc.EVENT_SRC_SDK.equals(eventSrc) && EventIDs.AD_SHOW_SUCCESS.equals(eventName)) {
            if (kwaiUtil != null) {
                kwaiUtil.checkAdEvents(eventName, params);
            }
        }
    }

    private void collectAdRevenue(Map<String, Object> params) {
        if (params == null) return;

        Object revenueObj = params.get(EventParams.EVENT_PARAM_REVENUE);
        if (revenueObj instanceof Double) {
            double revenue = (Double) revenueObj;
            if (revenue > 10) return;
            double adValue = LocalStorage.Instance.decodeDouble(IKeys.KEY_AD_ROAS, 0.0) + revenue;
            if (adValue >= adPingThreshold) {
                Map<String, Object> eventParams = new HashMap<>();
                eventParams.put(EventParams.EVENT_PARAM_CURRENCY, "USD");
                eventParams.put(EventParams.EVENT_PARAM_VALUE, adValue);

                logEvent(EventIDs.GMS_AD_PAID, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, eventParams, null);
                LocalStorage.Instance.encodeDouble(IKeys.KEY_AD_ROAS, 0.0);
            } else {
                LocalStorage.Instance.encodeDouble(IKeys.KEY_AD_ROAS, adValue);
            }

            double totalRevenue = LocalStorage.Instance.decodeDouble(IKeys.KEY_TOTAL_AD_REVENUE, 0.0) + revenue;
            LocalStorage.Instance.encodeDouble(IKeys.KEY_TOTAL_AD_REVENUE, totalRevenue);

            checkUac((float) revenue, (float) totalRevenue);
        }
    }

    public void setUserProperty(String key, String value, TrackPlatform platform) {
        if (platform != null) {
            IvyABManager.getInstance().setHeaderInfo(key, value);
            IvyTrack.getInstance().setUserProperty(key, value, platform);
        } else {
            IvyABManager.getInstance().setHeaderInfo(key, value);
            IvyTrack.getInstance().setUserProperty(key, value);
        }
    }

    // UAC
    private void initUac() {
        if (uac == null) {
            String uacUrl = GridManager.getInstance().getGridConfig("uacApi", "");
            if (uacUrl == null || uacUrl.isEmpty()) return;

            String appId = Helper.getConfig(Helper.CONFIG_KEY_APP_ID);
            if (appId == null) appId = "";

            uac = new UAC(appId, uacUrl);
            uac.checkAndUpdateUacTop();
        }
    }

    private void checkUac(float revenue, float totalRevenue) {
        if (uac != null) {
            uac.checkUacLtvConversion(revenue, totalRevenue);
        }
    }

    // 快手
    private void initKwai() {
        // boolean kwaiEventsStatus = GridManager.Instance.getGridConfig("kwaiEvents", true);
        // if (kwaiEventsStatus) {
        //     kwaiUtil = new KwaiUtil();
        // }
    }

    private void loadMetaConfig() {
        try {
            ApplicationInfo applicationInfo = App.getInstance().getPackageManager().getApplicationInfo(App.getInstance().getPackageName(), PackageManager.GET_META_DATA);
            appId = applicationInfo.metaData.getString("ivy.app.id", "");
            debug = applicationInfo.metaData.getBoolean("ivy.debug", false);
            IToast.debug = debug;
            ILog.isDebug = debug;
            if (debug) {
                IToast.toast("debug mode! 非正式签名安装包");
            }
            ILog.w(TAG, "current is debug mode!!!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getAppId() {
        if (appId == null || appId.isEmpty() || appId.trim().isEmpty()) {
            loadMetaConfig();
        }
        return appId;
    }

    // =========================== 支付 ================================
    @Override
    public boolean isPaymentInitialized() {
        if (ivyPay != null) {
            return ivyPay.isPaymentInitialized();
        } else {
            ILog.i(TAG, "payment not setup yet!!!");
            return false;
        }
    }

    public void setPaymentUserId(String id) {
        if (!TextUtils.isEmpty(id)) {
            LocalStorage.Instance.encodeString("_pay_user_id_", id);
        }
    }

    @Override
    public void pay(int id, String payload, String clientInfo) {
        if (ivyPay != null) {
            TestManager.getInstance().addEvent("Payment", "开始付费，billid=" + id, EventState.NORMAL, "");
            ivyPay.pay(id, payload, clientInfo);
        } else {
            ILog.i(TAG, "pay::payment not setup yet!!!");
            TestManager.getInstance().addEvent("Payment", "计费未初始化，检查配置", EventState.ERROR, "");
            if (builder != null && builder.purchaseResult != null) {
                builder.purchaseResult.payResult(id, IPurchaseResult.PAY_FAILED, payload, null, null);
            }
        }
    }

    @Override
    public void shippingGoods(String merchantTransactionId) {
        if (ivyPay != null) {
            ivyPay.shippingGoods(merchantTransactionId);
        } else {
            ILog.w(TAG, "pay::payment shipping goods failed, not init");
        }
    }

    @Override
    public String getGoodsInfo(int id) {
        if (ivyPay != null) {
            return ivyPay.getGoodsInfo(id);
        } else {
            ILog.i(TAG, "getGoodsInfo::payment not setup yet!!! ");
            return "{}";
        }
    }

    @Override
    public void queryPurchase(int id) {
        if (ivyPay != null) {
            ivyPay.queryPurchase(id);
        } else {
            ILog.i(TAG, "queryPurchase::payment not setup yet!!!; " + id);
        }
    }

    @Override
    public boolean isEnjoyPayInitialized() {
        if (ivyPay != null) {
            return ivyPay.isEnjoyPayInitialized();
        } else {
            ILog.i(TAG, "payment not setup yet!!!");
            return false;
        }
    }

    @Override
    public void enjoyPay(int id, String payload, String clientInfo) {
        if (ivyPay != null) {
            TestManager.getInstance().addEvent("Payment", "enjoypay开始付费，billid=" + id, EventState.NORMAL, "");
            ivyPay.enjoyPay(id, payload, clientInfo);
        } else {
            ILog.i(TAG, "pay::payment not setup yet!!!");
            TestManager.getInstance().addEvent("Payment", "enjoypay计费未初始化，检查配置", EventState.ERROR, "");
            if (builder != null && builder.purchaseResult != null) {
                builder.purchaseResult.payResult(id, IPurchaseResult.PAY_FAILED, payload, null, null);
            }
        }
    }

    @Override
    public String getEnjoyPayGoodsInfo(int id) {
        if (ivyPay != null) {
            return ivyPay.getEnjoyPayGoodsInfo(id);
        } else {
            ILog.i(TAG, "getGoodsInfo::enjoypay not setup yet!!! ");
            return "{}";
        }
    }

    @Override
    public void queryEnjoyPurchase(int id) {
        if (ivyPay != null) {
            ivyPay.queryEnjoyPurchase(id);
        } else {
            ILog.i(TAG, "queryPurchase::enjoypay not setup yet!!!; " + id);
        }
    }

    private final IIPurchaseResult purchaseResponseListener = new IIPurchaseResult() {
        @Override
        public void payResult(int payId, int status, String payload, String merchantTransactionId, String purchase) {
            if (ivyPay != null) {
                ivyPay.endPay();
            }
            if (builder != null && builder.purchaseResult != null) {
                builder.purchaseResult.payResult(payId, status, payload, merchantTransactionId, purchase);
            }

            if (status == IPurchaseResult.PAY_SUCCEED) {
                LocalStorage.Instance.encodeString(IKeys.KEY_PAYING_USER, "1");
                setUserProperty("paying_user", "1", null);
            }

            if (ivyPayEvent == null) {
                ivyPayEvent = new IvyPayEvent();
            }

            if (status == IPurchaseResult.PAY_SUCCEED && ivyPay != null) {
                String data = ivyPay.getGoodsInfo(payId);
                if (data != null) {
                    try {
                        JSONObject obj = new JSONObject(data);
                        int count = obj.optInt("IvyPayEvent", 0);
                        String event = obj.optString("v25_event", "in_ivyapp_purchase");
                        ivyPayEvent.update(event, count);
                    } catch (Exception ignore) {
                    }
                }
                trackAxonEvent("purchase", null);
            }
        }

        @Override
        public void onShippingResult(String merchantTransactionId, boolean status) {
            if (builder != null && builder.purchaseResult != null) {
                builder.purchaseResult.onShippingResult(merchantTransactionId, status);
            }
        }

        @Override
        public void onStoreInitialized(boolean initState) {
            if (builder != null && builder.purchaseResult != null) {
                builder.purchaseResult.onStoreInitialized(initState);
            }
        }

        @Override
        public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms) {
            IvySdk.this.logEvent(eventName, eventType, eventSrc, params, platforms);
            try {
                if (eventName.equals(EventIDs.IAP_PRE_ORDER) && params != null && params.containsKey("sdk_state")) {
                    int state = (int) params.get("sdk_state");
                    if (state == 1) {
                        trackAxonEvent("begin_checkout", null);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    };
    // =========================== 支付 ================================

    // =========================== grid data ================================
    @Override
    public String getGridConfig(String key, String defaultValue) {
        return GridManager.getInstance().getGridConfig(key, defaultValue);
    }

    @Override
    public int getGridConfig(String key, int defaultValue) {
        return GridManager.getInstance().getGridConfig(key, defaultValue);
    }

    @Override
    public double getGridConfig(String key, double defaultValue) {
        return GridManager.getInstance().getGridConfig(key, defaultValue);
    }

    @Override
    public boolean getGridConfig(String key, boolean defaultValue) {
        return GridManager.getInstance().getGridConfig(key, defaultValue);
    }

    @Override
    public long getGridConfig(String key, long defaultValue) {
        return GridManager.getInstance().getGridConfig(key, defaultValue);
    }
    // =========================== grid data ================================

    // =========================== remote config ================================
    private void setupFirebase() {
        try {
            if ("yes".equals(LocalStorage.Instance.decodeString("low_device", "no"))) {
                return;
            }
            if (firebaseImpl != null) {
                return;
            }
            new SimpleRxJust<>("com.ivy.firebase.FirebaseImpl").map(new Func<String, IFirebase>() {
                @Override
                public IFirebase call(String value) throws Exception {
                    IFirebase _firebaseImpl = (IFirebase) Class.forName(value).getDeclaredConstructor().newInstance();
                    return _firebaseImpl;
                }
            }).subscribeOn(Schedulers.io()).observeOn(RxAndroid.main()).subscribe(new Observer<IFirebase>() {
                @Override
                public void onSuccess(IFirebase value) {
                    firebaseImpl = value;
                    firebaseImpl.initFirebaseApp();
//                    firebaseImpl.setupFirebaseDataAnalytics();
                    try {
                        String defaultRemoteConfig = GridManager.getInstance().getGridConfig("remoteConfig", null);
                        if (!TextUtils.isEmpty(defaultRemoteConfig)) {
                            setupDefaultRemoteData(defaultRemoteConfig);
                        }
                    } catch (Exception ignored) {
                    }
                    firebaseImpl.initRemoteConfig(debug, status -> ILog.i(TAG, "firebase remote config initialize result:" + status));
                    FCMMessager.reportFCMPushReachEvent();
                }

                @Override
                public void onError(Throwable e) {
                    ILog.e(TAG, "instance firebase impl failed:" + e.getMessage());
                }
            });
            //            firebaseImpl = (IFirebase) Class.forName("com.ivy.firebase.FirebaseImpl").getDeclaredConstructor().newInstance();
            //            firebaseImpl.initFirebaseApp();
            //            firebaseImpl.initRemoteConfig(debug, status -> ILog.i(TAG, "firebase remote config initialize result:" + status));
            //            reportFCMPushReachEvent();
        } catch (Exception e) {
            ILog.e(TAG, "setup firebase failed:" + e.getMessage());
        }
    }


    private void setupFirebaseDataAnalytics() {
        if (firebaseImpl != null) {
            firebaseImpl.setupFirebaseDataAnalytics();
        }
    }

    public void setupDefaultRemoteData(String defaultData) {
        if (firebaseImpl != null) {
            firebaseImpl.setupDefaultRemoteData(defaultData);
        }
    }

    public String getRemoteConfigString(String key) {
        return firebaseImpl != null ? firebaseImpl.getRemoteConfigString(key) : "";
    }

    public double getRemoteConfigDouble(String key) {
        return firebaseImpl != null ? firebaseImpl.getRemoteConfigDouble(key) : 0.0;
    }

    public boolean getRemoteConfigBoolean(String key) {
        return firebaseImpl != null && firebaseImpl.getRemoteConfigBoolean(key);
    }

    public long getRemoteConfigLong(String key) {
        return firebaseImpl != null ? firebaseImpl.getRemoteConfigLong(key) : 0L;
    }

    public boolean hasIvyRemoteConfigKey(String key) {
        return IvyRemoteConfig.getInstance().has(key);
    }

    public int getIvyRemoteConfigInt(String key) {
        return IvyRemoteConfig.getInstance().getInt(key);
    }

    public String getIvyRemoteConfigString(String key) {
        String result = IvyRemoteConfig.getInstance().getString(key);
        return result == null ? "" : result;
    }

    public double getIvyRemoteConfigDouble(String key) {
        return IvyRemoteConfig.getInstance().getDouble(key);
    }

    public boolean getIvyRemoteConfigBoolean(String key) {
        return IvyRemoteConfig.getInstance().getBoolean(key);
    }

    public long getIvyRemoteConfigLong(String key) {
        return IvyRemoteConfig.getInstance().getLong(key);
    }

    // =========================== firebase cloud function ================================
    public void firebaseCloudFunctions(String functionName, JSONObject parameters, IFirebase.SuccessCallback success, IFirebase.FailureCallback failure) {
        if (firebaseImpl != null) {
            firebaseImpl.runCloudFunctions(functionName, parameters, success, failure);
        } else {
            ILog.e("FirebaseCloudFunction", functionName + " failed:not ready");
            failure.onFailure("not_ready");
        }
    }


    // =========================== firebase cloud function ================================


    // ======================== 客服 ===================================
    private final IHelperCallback customerEvents = new IHelperCallback() {
        @Override
        public void onUnreadHelperMessageCount(int count) {
            if (builder != null && builder.helperListener != null) {
                builder.helperListener.onUnreadHelperMessageCount(count);
            }
        }

        @Override
        public void onHelperStatus(int status) {
            if (builder != null && builder.helperListener != null) {
                builder.helperListener.onHelperStatus(status);
            }
        }
    };

    @Override
    public boolean isHelperInitialized() {
        if (ivyHelper != null) {
            return ivyHelper.isHelperInitialized();
        } else {
            ILog.w(TAG, "customer had not initialized");
            return false;
        }
    }

    @Override
    public boolean hasNewHelperMessage() {
        if (ivyHelper != null) {
            return ivyHelper.hasNewHelperMessage();
        } else {
            ILog.w(TAG, "customer had not initialized");
            return false;
        }
    }

    @Override
    public void showHelper(String entranceId, String meta, String tags, String welcomeMessag) {
        if (ivyHelper != null) {
            ivyHelper.showHelper(entranceId, meta, tags, welcomeMessag);
        } else {
            ILog.w(TAG, "customer had not initialized");
        }
    }

    @Override
    public void showHelperSingleFAQ(String faqId, int moment) {
        if (ivyHelper != null) {
            ivyHelper.showHelperSingleFAQ(faqId, moment);
        } else {
            ILog.w(TAG, "customer had not initialized");
        }
    }

    @Override
    public void listenHelperUnreadMessageCount(boolean onlyOnce) {
        if (ivyHelper != null) {
            ivyHelper.listenHelperUnreadMessageCount(onlyOnce);
        } else {
            ILog.w(TAG, "customer had not initialized");
        }
    }

    @Override
    public void stopListenHelperUnreadMessageCount() {
        if (ivyHelper != null) {
            ivyHelper.stopListenHelperUnreadMessageCount();
        } else {
            ILog.w(TAG, "customer had not initialized");
        }
    }

    @Override
    public void updateHelperUserInfo(String data, String tags) {
        if (ivyHelper != null) {
            ivyHelper.updateHelperUserInfo(data, tags);
        } else {
            ILog.w(TAG, "customer had not initialized");
        }
    }

    @Override
    public void updateHelperLanguage(String l) {
        if (ivyHelper != null) {
            ivyHelper.updateHelperLanguage(l);
        } else {
            ILog.w(TAG, "customer had not initialized");
        }
    }

    @Override
    public void resetHelperUserInfo() {
        if (ivyHelper != null) {
            ivyHelper.resetHelperUserInfo();
        } else {
            ILog.w(TAG, "customer had not initialized");
        }
    }

    @Override
    public void closeHelper() {
        if (ivyHelper != null) {
            ivyHelper.closeHelper();
        } else {
            ILog.w(TAG, "customer had not initialized");
        }
    }
// ======================== 客服 ===================================


    // ======================== 游戏服务 ===================================
    private final IAuthResult authResult = new IAuthResult() {
        @Override
        public void onLoginResult(String platform, boolean status, String channel, String reason) {
            if (builder != null && builder.authResult != null) {
                builder.authResult.onLoginResult(platform, status, channel, reason);
            }
            LocalStorage.Instance.encodeString("_sdk_login_platform_", platform);
            switch (platform) {
                case AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES:
                    setUserProperty("platform", AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, TrackPlatform.THINKING_DATA);
                    String playId = ivyGames != null ? ivyGames.getPlayGamesUserId() : "";
                    setUserProperty("play_games_account_id", playId, TrackPlatform.THINKING_DATA);
                    break;
                case AuthPlatforms.LOGIN_PLATFORM_FACEBOOK:
                    setUserProperty("platform", AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, TrackPlatform.THINKING_DATA);
                    String fbId = ivyGames != null ? ivyGames.getFacebookUserId() : "";
                    setUserProperty("facebook_account_id", fbId, TrackPlatform.THINKING_DATA);
                    break;
                case AuthPlatforms.LOGIN_PLATFORM_GOOGLE:
                    setUserProperty("platform", AuthPlatforms.LOGIN_PLATFORM_GOOGLE, TrackPlatform.THINKING_DATA);
                    String googleId = ivyGames != null ? ivyGames.getFacebookUserId() : "";
                    setUserProperty("google_account_id", googleId, TrackPlatform.THINKING_DATA);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onLogout(String platform) {
            if (builder != null && builder.authResult != null) {
                builder.authResult.onLogout(platform);
            }
        }

        @Override
        public void onPlatformsInitialized() {
            authPlatformsInitializeStatus.set(true);
            if (builder != null && builder.authResult != null) {
                builder.authResult.onPlatformsInitialized();
            }
        }
    };

    private IPlayGameEvents playGamesListener = new IPlayGameEvents() {

        @Override
        public void onReadArchive(String archiveName, String result) {
            if (builder != null && builder.playGameEvents != null) {
                builder.playGameEvents.onReadArchive(archiveName, result);
            }
        }

        @Override
        public void onWriteArchive(String archiveName, String actionId, Boolean result) {
            if (builder != null && builder.playGameEvents != null) {
                builder.playGameEvents.onWriteArchive(archiveName, actionId, result);
            }
        }

        @Override
        public void onLoadEvents(String data) {
            if (builder != null && builder.playGameEvents != null) {
                builder.playGameEvents.onLoadEvents(data);
            }
        }

        @Override
        public void onLeaderBoardScore(String leaderboardId, String score, String rank) {
            if (builder != null && builder.playGameEvents != null) {
                builder.playGameEvents.onLeaderBoardScore(leaderboardId, score, rank);
            }
        }
    };

    public boolean isAuthPlatformReady() {
        return authPlatformsInitializeStatus.get();
    }

    public boolean hasPlayGamesSigned() {
        if (ivyGames != null) {
            return ivyGames.isPlayGamesLogged();
        } else {
            ILog.i(TAG, "ivy games unable to use");
            return false;
        }
    }

    public void loginPlayGames() {
        if (ivyGames != null) {
            TestManager.getInstance().addEvent("PlayGames_Auth", "请求登录PlayGames", EventState.NORMAL, "");
            ivyGames.loginPlayGames();
        } else {
            ILog.i(TAG, "ivy games unable to use");
            TestManager.getInstance().addEvent("PlayGames_Auth", "请求登录PlayGames,服务未初始化，请检查配置", EventState.ERROR, "");
            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, false, null, "ivy game not initialized");
        }
    }

    public void logoutPlayGames() {
        if (ivyGames != null) {
            ivyGames.logoutPlayGames();
        } else {
            ILog.i(TAG, "ivy games unable to use");
        }
    }

    public String getPlayGamesUserInfo() {
        if (ivyGames != null) {
            return ivyGames.getPlayGamesUserInfo();
        } else {
            ILog.i(TAG, "ivy games unable to use");
            return "{}";
        }
    }

    public void unlockAchievement(String achievementId) {
        if (ivyGames != null) {
            ivyGames.unlockAchievement(achievementId);
        } else {
            ILog.i(TAG, "ivy games unable to use");
            TestManager.getInstance().addEvent("PlayGames","解锁成就失败，PlayGames未初始化" , EventState.ERROR, "achievementId=" + achievementId);
        }
    }

    public void increaseAchievement(String achievementId, int step) {
        if (ivyGames != null) {
            ivyGames.increaseAchievement(achievementId, step);
        } else {
            ILog.i(TAG, "ivy games unable to use");
            TestManager.getInstance().addEvent("PlayGames", "更新成就进度失败，PlayGames未初始化" , EventState.ERROR, "achievementId=" + achievementId + ";step=" + step);
        }
    }

    public void setAchievementStep(String achievementId, int step) {
        if (ivyGames != null) {
            ivyGames.setAchievementStep(achievementId, step);
        } else {
            ILog.i(TAG, "ivy games unable to use");
            TestManager.getInstance().addEvent("PlayGames", "更新成就step失败，PlayGames未初始化" , EventState.ERROR, "achievementId=" + achievementId + ";step=" + step);
        }
    }

    public void showAchievement() {
        if (ivyGames != null) {
            ivyGames.showAchievement();
        } else {
            ILog.i(TAG, "ivy games unable to use");
        }
    }

    public void showLeaderboards() {
        if (ivyGames != null) {
            ivyGames.showLeaderboards();
        } else {
            ILog.i(TAG, "ivy games unable to use");
        }
    }

    public void showLeaderboard(String leaderboardId) {
        if (ivyGames != null) {
            ivyGames.showLeaderboard(leaderboardId);
        } else {
            ILog.i(TAG, "ivy games unable to use");
        }
    }

    public void updateLeaderboard(String leaderboardId, long score) {
        if (ivyGames != null) {
            ivyGames.updateLeaderboard(leaderboardId, score);
        } else {
            ILog.i(TAG, "ivy games unable to use");
        }
    }

    public void loadLeaderboardScore(String leaderboardId) {
        if (TextUtils.isEmpty(leaderboardId)) return;
        if (ivyGames != null) ivyGames.loadLeaderboardScore(leaderboardId);
        else ILog.w(TAG, "play-games auth invalid to use");
    }

    public void savePlayGamesArchive(String archiveName, String actionId, String payload) {
        if (ivyGames != null) {
            ivyGames.saveArchive(archiveName, actionId, payload);
        } else {
            ILog.i(TAG, "ivy games unable to use");
            playGamesListener.onWriteArchive(archiveName, actionId, false);
        }
    }

    public void readPlayGamesArchive(String archiveName) {
        if (ivyGames != null) {
            ivyGames.readArchive(archiveName);
        } else {
            ILog.i(TAG, "ivy games unable to use");
            playGamesListener.onReadArchive(archiveName, "");
        }
    }

    public void submitPlayGamesActivityEvent(String eventId, int increment) {
        if (ivyGames != null) {
            ivyGames.submitActivityEvent(eventId, increment);
        } else {
            ILog.i(TAG, "ivy games unable to use");
        }
    }

    public void loadPlayGamesActivityEventById(String eventId) {
        if (ivyGames != null) {
            ivyGames.loadActivityEventById(eventId);
        } else {
            ILog.i(TAG, "ivy games unable to use");
            playGamesListener.onLoadEvents("[]");
        }
    }

    public void loadPlayGamesActivityEvents() {
        if (ivyGames != null) {
            ivyGames.loadActivityEvents();
        } else {
            ILog.i(TAG, "ivy games unable to use");
            playGamesListener.onLoadEvents("[]");
        }
    }

    public void submitPlayGameEvent(String eventId, String data) {
        TestManager.getInstance().addEvent("PlayGames", "开始上传数据", EventState.NORMAL, "事件名:" + eventId + ";\n数据:\n" + data);
        if (ivyGames != null) {
            try {
                if (TextUtils.isEmpty(eventId)) {
                    ILog.e(TAG, "play game event upload failed: eventId == null");
                    TestManager.getInstance().addEvent("PlayGames", "上传数据失败", EventState.ERROR, "事件名无效");
                    return;
                }
                JSONObject property = null;
                if (!TextUtils.isEmpty(data)) {
                    property = new JSONObject(data);
                }
                ivyGames.submitPlayGameEvent(eventId, property);
            } catch (Exception e) {
                TestManager.getInstance().addEvent("PlayGames", "上传数据失败", EventState.ERROR, e.getMessage());
                ILog.e(TAG, "play game event upload failed:" + e.getMessage());
            }
        } else {
            TestManager.getInstance().addEvent("PlayGames", "上传数据失败", EventState.ERROR, "Game模块未初始化");
            ILog.i(TAG, "ivy games unable to use");
        }
    }


    public void loginFacebook() {
        if (ivyGames != null) {
            TestManager.getInstance().addEvent("Facebook_Auth", "请求登录Facebook", EventState.NORMAL, "");
            ivyGames.loginFacebook();
        } else {
            TestManager.getInstance().addEvent("Facebook_Auth", "请求登录Facebook,服务未初始化，请检查配置", EventState.ERROR, "");
            ILog.i(TAG, "ivy games unable to use");
            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "ivy games not initialized");
        }
    }

    public void logoutFacebook() {
        if (ivyGames != null) {
            ivyGames.logoutFacebook();
        } else {
            ILog.i(TAG, "ivy games unable to use");
        }
    }

    public boolean isFacebookLogged() {
        if (ivyGames != null) {
            return ivyGames.isFacebookLogged();
        } else {
            ILog.i(TAG, "ivy games unable to use");
            return false;
        }
    }

    public String getFacebookFriends() {
        if (ivyGames != null) {
            return ivyGames.getFacebookFriends();
        } else {
            ILog.i(TAG, "ivy games unable to use");
            return "[]";
        }
    }

    public String getFacebookUserInfo() {
        if (ivyGames != null) {
            return ivyGames.getFacebookUserInfo();
        } else {
            ILog.i(TAG, "ivy games unable to use");
            return "{}";
        }
    }

    public void loginGoogle() {
        if (ivyGames != null) {
            TestManager.getInstance().addEvent("Google_Auth", "请求登录Google", EventState.NORMAL, "");
            ivyGames.loginGoogle();
        } else {
            TestManager.getInstance().addEvent("Google_Auth", "请求登录Google,服务未初始化，请检查配置", EventState.ERROR, "");
            ILog.i(TAG, "ivy games unable to use");
            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, "ivy games unable to use");
        }
    }

    public void logoutGoogle() {
        if (ivyGames != null) {
            ivyGames.logoutGoogle();
        } else {
            ILog.i(TAG, "ivy games unable to use");
        }
    }

    public boolean isGoogleLogged() {
        if (ivyGames != null) {
            return ivyGames.isGoogleLogged();
        } else {
            ILog.i(TAG, "ivy games unable to use");
            return false;
        }
    }

    public String getGoogleUserInfo() {
        if (ivyGames != null) {
            return ivyGames.getGoogleUserInfo();
        } else {
            ILog.i(TAG, "ivy games unable to use");
            return "{}";
        }
    }

    public String getGoogleUserId() {
        if (ivyGames != null) {
            return ivyGames.getGoogleUserId();
        } else {
            ILog.i(TAG, "ivy games unable to use");
            return "";
        }
    }

    public void logoutFirebase() {
        if (ivyGames != null) {
            ivyGames.logoutFirebase();
        } else {
            ILog.i(TAG, "ivy games unable to use");
        }
    }

    public String getFirebaseUserInfo(String channel) {
        if (ivyGames != null) {
            return ivyGames.getFirebaseUserInfo(channel);
        } else {
            ILog.i(TAG, "ivy games unable to use");
            return "{}";
        }
    }

    public String getFirebaseUserId() {
        return ivyGames != null ? ivyGames.getFirebaseUserId() : "";
    }

    public boolean isFirebaseAnonymousLogged() {
        if (ivyGames != null) {
            return ivyGames.isFirebaseAnonymousLogged();
        } else {
            ILog.i(TAG, "ivy games unable to use");
            return true;
        }
    }

    public boolean isFirebaseLinkedWithChannel(String channel) {
        if (ivyGames != null) {
            return ivyGames.isFirebaseLinkedWithChannel(channel);
        } else {
            ILog.i(TAG, "ivy games unable to use");
            return false;
        }
    }

    public boolean canFirebaseUnlinkWithChannel(String channel) {
        if (ivyGames != null) {
            return ivyGames.canFirebaseUnlinkWithChannel(channel);
        } else {
            ILog.i(TAG, "ivy games unable to use");
            return false;
        }
    }

    public void unlinkFirebaseWithChannel(String channel, IFirebaseUnlink callback) {
        if (ivyGames != null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "请求解绑" + channel, EventState.NORMAL, "");
            ivyGames.unlinkFirebaseWithChannel(channel, callback);
        } else {
            TestManager.getInstance().addEvent("Firebase_Auth", "请求解绑" + channel + "失败，游戏服务未初始化", EventState.ERROR, "");
            ILog.i(TAG, "ivy games unable to use");
            if (callback != null) {
                callback.onUnlinked(channel, false, "ivy games unable to use");
            }
        }
    }

    public void reloadFirebaseLastSign(IFirebaseAuthReload authReload) {
        if (ivyGames != null) {
            ivyGames.reloadFirebaseLastSign(authReload);
        } else {
            ILog.i(TAG, "ivy games unable to use");
            authReload.onReload(false, "ivy games unable to use");
        }
    }

    public void loginAnonymous(IAuthResponse authResult) {
        if (ivyGames != null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "请求使用匿名渠道登录", EventState.NORMAL, "");
            ivyGames.loginAnonymous(authResult);
        } else {
            TestManager.getInstance().addEvent("Firebase_Auth", "请求使用匿名渠道登录,服务未初始化，请检查配置", EventState.ERROR, "");
            ILog.i(TAG, "ivy games unable to use");
            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.ANONYMOUS, "ivy games unable to use");
        }
    }

    public void loginWithGoogle(IAuthResponse authResult) {
        if (ivyGames != null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "请求使用Google渠道登录", EventState.NORMAL, "");
            ivyGames.loginWithGoogle(authResult);
        } else {
            TestManager.getInstance().addEvent("Firebase_Auth", "请求使用Google渠道登录,服务未初始化，请检查配置", EventState.ERROR, "");
            ILog.i(TAG, "ivy games unable to use");
            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, "ivy games unable to use");
        }
    }

    public void loginWithPlayGames(IAuthResponse authResult) {
        if (ivyGames != null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "请求使用PlayGames渠道登录", EventState.NORMAL, "");
            ivyGames.loginWithPlayGames(authResult);
        } else {
            ILog.i(TAG, "ivy games unable to use");
            TestManager.getInstance().addEvent("Firebase_Auth", "请求使用PlayGames渠道登录,服务未初始化，请检查配置", EventState.ERROR, "");
            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, "ivy games unable to use");
        }
    }

    public void loginWithFacebook(IAuthResponse authResult) {
        if (ivyGames != null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "请求使用Facebook渠道登录", EventState.NORMAL, "");
            ivyGames.loginWithFacebook(authResult);
        } else {
            TestManager.getInstance().addEvent("Firebase_Auth", "请求使用Facebook渠道登录,服务未初始化，请检查配置", EventState.ERROR, "");
            ILog.i(TAG, "ivy games unable to use");
            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, "ivy games unable to use");
        }
    }

    public void loginWithEmailAndPassword(String email, String password, IAuthResponse authResult) {
        if (ivyGames != null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "请求使用Email渠道登录", EventState.NORMAL, "");
            ivyGames.loginWithEmailAndPassword(email, password, authResult);
        } else {
            TestManager.getInstance().addEvent("Firebase_Auth", "请求使用Email渠道登录,服务未初始化，请检查配置", EventState.ERROR, "");
            ILog.i(TAG, "ivy games unable to use");
            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, "ivy games unable to use");
        }
    }

    // ======================== 存档 ===================================
    public void setArchive(String collection, String documentId, String jsonData, IArchiveResult callback) {
        if (ivyGames != null) {
            ivyGames.set(collection, documentId, jsonData, callback);
        } else {
            ILog.w(TAG, "invalid sign! check your firebase sign status");
            callback.onFailure(collection, null, "invalid sign");
        }
    }

    public void readArchive(String collection, String documentId, IArchiveResult callback) {
        if (ivyGames != null) {
            ivyGames.read(collection, documentId, callback);
        } else {
            ILog.w(TAG, "invalid sign! check your firebase sign status");
            callback.onFailure(collection, null, "invalid sign");
        }
    }

    public void mergeArchive(String collection, String documentId, String jsonData, IArchiveResult callback) {
        if (ivyGames != null) {
            ivyGames.merge(collection, documentId, jsonData, callback);
        } else {
            ILog.w(TAG, "invalid sign! check your firebase sign status");
            callback.onFailure(collection, null, "invalid sign");
        }
    }

    public void queryArchive(String collection, String documentId, IArchiveResult callback) {
        if (ivyGames != null) {
            ivyGames.query(collection, documentId, callback);
        } else {
            ILog.w(TAG, "invalid sign! check your firebase sign status");
            callback.onFailure(collection, null, "invalid sign");
        }
    }

    public void deleteArchive(String collection, String documentId, IArchiveResult callback) {
        if (ivyGames != null) {
            ivyGames.delete(collection, documentId, callback);
        } else {
            ILog.w(TAG, "invalid sign! check your firebase sign status");
            callback.onFailure(collection, null, "invalid sign");
        }
    }

    public void updateArchive(String collection, String documentId, String jsonData, IArchiveResult callback) {
        if (ivyGames != null) {
            ivyGames.update(collection, documentId, jsonData, callback);
        } else {
            ILog.w(TAG, "invalid sign! check your firebase sign status");
            callback.onFailure(collection, null, "invalid sign");
        }
    }

    public void snapshotArchive(String collection, String documentId, IArchiveResult callback) {
        if (ivyGames != null) {
            ivyGames.snapshot(collection, documentId, callback);
        } else {
            ILog.w(TAG, "invalid sign! check your firebase sign status");
            callback.onFailure(collection, null, "invalid sign");
        }
    }
// ======================== 存档 ===================================


    // af邀请
    public void appsflyerInviteUser(String channel, String campaign, String inviterId, String inviterAppId) {
        IvyTrack.getInstance().appsflyerInviteUser(channel, campaign, inviterId, inviterAppId);
    }

    public String getAppsflyerInviterId() {
        return IvyTrack.getInstance().getAppsflyerInviterId();
    }
// af邀请


// ======================== 游戏服务 ===================================

    public boolean hasNotch() {
        return InsetsHelper.hasCutout();
    }

    public int getNotchHeight() {
        return InsetsHelper.getCutoutHeight();
    }

    public boolean hasGestureBar() {
        return InsetsHelper.hasGestureNavigation();
    }

    public int getGestureBarHeight() {
        return InsetsHelper.getGestureBarHeight();
    }

    public String getLastNotificationAction() {
        try {
            String action = LocalStorage.Instance.decodeString(IKeys.KEY_NOTIFICATION_ACTION, "");
            LocalStorage.Instance.encodeString(IKeys.KEY_NOTIFICATION_ACTION, "");
            return action;
        } catch (Exception ignored) {
        }
        return "";
    }

    public void displayInNotch() {
        try {
            Activity activity = ActivityUtil.getInstance().getActivity();
            InsetsHelper.OnInsetsCallback onInsetsCallback = new InsetsHelper.OnInsetsCallback() {
                @Override
                public void onInsets(int statusBarHeight, int navigationBarHeight, int gestureBarHeight, int cutoutHeight, boolean gestureNavigation, boolean hasCutout) {
                    ILog.d(TAG, "statusBarHeight=" + statusBarHeight);
                    ILog.d(TAG, "navigationBarHeight=" + navigationBarHeight);
                    ILog.d(TAG, "gestureBarHeight=" + gestureBarHeight);
                    ILog.d(TAG, "cutoutHeight=" + cutoutHeight);
                    ILog.d(TAG, "has gesture navigation=" + gestureNavigation);
                    ILog.d(TAG, "has Cutout=" + hasCutout);
                }
            };
            InsetsHelper.loadInsets(activity, onInsetsCallback);
            InsetsHelper.observeInsets(activity, onInsetsCallback);

            ApplicationInfo applicationInfo = activity.getPackageManager().getApplicationInfo(App.getInstance().getPackageName(), PackageManager.GET_META_DATA);
            boolean useNotch;
            if (applicationInfo.metaData.containsKey("ivy.notch")) {
                useNotch = applicationInfo.metaData.getBoolean("ivy.notch", true);
            } else {
                useNotch = true;
            }
            ILog.d(TAG, "use notch=" + useNotch);
            final View decorView = activity.getWindow().getDecorView();
            decorView.getViewTreeObserver().addOnWindowFocusChangeListener(hasFocus -> {
                ILog.d(TAG, "OnWindowFocusChangeListener, hasFocus=" + hasFocus);
                if (hasFocus) {
                    try {
                        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                        decorView.setSystemUiVisibility(flags);
                        ScreenLayoutController.configureScreen(activity, true, true, useNotch);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                            decorView.requestApplyInsets();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            ScreenLayoutController.configureScreen(activity, true, true, useNotch);
        } catch (Exception e) {
            ILog.e(TAG, "set display notch failed: " + e.getMessage());
        }
    }

    public boolean isDebug() {
        return debug;
    }

    private static IAdManager adManager = null;

    public void triggerBannerAd(String placement, String clientInfo) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("ad_format", AdType.BANNER.value);
        params.put("sdk_state", 1);
        IvyTrack.getInstance().logEvent(EventIDs.AD_TRIGGER_EVENT, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params);
    }

    private int triggerInterCount = 0;
    private long lastTriggerInterTime = 0;
    private long lastTriggerVideoTime = 0;

    public void triggerInterstitialAd(String placement, String clientInfo) {
        if (adManager != null && !adManager.isInterstitialAdAllowed()) {
            ILog.d(TAG, "trigger interstitial ad blocked by cooldown");
            return;
        }
        triggerInterCount += 1;
        HashMap<String, Object> params = new HashMap<>();
        params.put("ad_format", AdType.INTERSTITIAL.value);
        params.put("sdk_state", hasInterstitialAd(placement, clientInfo) ? 1 : 0);
        params.put("session_trigger_ad_count", triggerInterCount);
        long now = SystemClock.elapsedRealtime();
        params.put("session_trigger_delta_time", now - lastTriggerInterTime);
        lastTriggerInterTime = now;
        IvyTrack.getInstance().logEvent(EventIDs.AD_TRIGGER_EVENT, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params);
    }

    private int triggerVideoCount = 0;

    public void triggerRewardedAd(String placement, String clientInfo) {
        triggerVideoCount += 1;
        HashMap<String, Object> params = new HashMap<>();
        params.put("ad_format", AdType.REWARDED.value);
        params.put("sdk_state", hasRewardedAd(placement, clientInfo) ? 1 : 0);
        params.put("session_trigger_ad_count", triggerVideoCount);
        long now = SystemClock.elapsedRealtime();
        params.put("session_trigger_delta_time", now - lastTriggerVideoTime);
        lastTriggerVideoTime = now;
        IvyTrack.getInstance().logEvent(EventIDs.AD_TRIGGER_EVENT, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_SDK, params);
        trackAxonEvent("rewarded_ad_opportunity", null);
    }

    private IAdListener buildAdListener() {
        return new IAdListener() {
            @Override
            public void onInited() {
                try {
                    // Set consent types.
//                        FirebaseAnalytics.ConsentStatus status = FirebaseAnalytics.ConsentStatus.GRANTED;
//                        Map<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> consentMap = new EnumMap<>(FirebaseAnalytics.ConsentType.class);
//                        consentMap.put(FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE, status);
//                        consentMap.put(FirebaseAnalytics.ConsentType.AD_STORAGE, status);
//                        consentMap.put(FirebaseAnalytics.ConsentType.AD_USER_DATA, status);
//                        consentMap.put(FirebaseAnalytics.ConsentType.AD_PERSONALIZATION, status);
//                        FirebaseAnalytics.getInstance(activity.getApplicationContext()).setConsent(consentMap);
//                    setupFirebase();
//                    if (firebaseImpl != null) {
//                        firebaseImpl.initFirebaseApp();
//                        firebaseImpl.setupFirebaseDataAnalytics();
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAdLoadSuccess(AdType adType) {
                //builder.adListener.onAdLoadSuccess(adType);
            }

            @Override
            public void onAdLoadFailure(AdType adType, String reason) {
                //builder.adListener.onAdLoadFailure(adType, reason);
            }

            @Override
            public void onAdShowSuccess(AdType adType, String tag, String placement) {
                builder.adListener.onAdShowSuccess(adType, tag, placement);
//                trackAxonEvent("ad_view");
            }

            @Override
            public void onAdShowFailed(AdType adType, String reason, String tag, String placement) {
                builder.adListener.onAdShowFailed(adType, reason, tag, placement);
            }

            @Override
            public void onAdClicked(AdType adType, String tag, String placement) {
                builder.adListener.onAdClicked(adType, tag, placement);
//                trackAxonEvent("ad_clicked");
            }

            @Override
            public void onAdClosed(AdType adType, boolean gotReward, String tag, String placement) {
                builder.adListener.onAdClosed(adType, gotReward, tag, placement);
            }

            @Override
            public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, boolean af) {
                // param af is deprecated now
                IvySdk.this.logEvent(eventName, eventType, eventSrc, params, null);
            }
        };
    }

    /**
     * 调度广告初始化：
     * - 若之前已经归因过（非首次启动），直接初始化，无需等待。
     * - 否则设置超时兜底（BLAST_ADS_AF_TIMEOUT_MS 后执行），
     * 若 AF 归因在超时内回来，则由 triggerInitBlastAds() 提前初始化并取消超时。
     */
    private void scheduleInitBlastAds() {
        if (LocalStorage.Instance.containsKey("af_attributed")) {
            // 非首次启动，已归因，直接初始化
            ILog.i("hoho", "AF already attributed, initBlastAds immediately");
            triggerInitBlastAds();
            return;
        }
        // 首次启动，等待 AF 归因或超时兜底
        blastAdsTimeoutRunnable = () -> {
            ILog.i("hoho", "AF attribution timeout, triggering initBlastAds as fallback");
            triggerInitBlastAds();
        };

        JSONObject config = GridManager.getInstance().getGridData();
        JSONObject ads = config.optJSONObject("ads");
        int timeout = 1;
        if (ads != null) {
            timeout = ads.optInt("timeout", 1);
        }
        ILog.i("hoho", "wait af timeout " + timeout);
        mainHandler.postDelayed(blastAdsTimeoutRunnable, timeout * 1000L);
    }

    /**
     * 确保 initBlastAds 只被调用一次：
     * 由 AF 归因回调或超时兜底触发，取消对方的调度。
     */
    private void triggerInitBlastAds() {
        if (blastAdsInitialized.compareAndSet(false, true)) {
            // 移除超时兜底（如果是 AF 归因提前触发）
            if (blastAdsTimeoutRunnable != null) {
                mainHandler.removeCallbacks(blastAdsTimeoutRunnable);
            }
            mainHandler.post(this::initBlastAds);
        }
    }

    private void initBlastAds() {
        try {
            adManager = (IAdManager) Class.forName("com.ivy.ads.AdManagerImpl").newInstance();
            adManager.setup(ActivityUtil.getInstance().getActivity(), IvyTrack.getInstance(), IvyABManager.getInstance(), buildAdListener(), debug);
            if (pendingBannerAd) {
                pendingBannerAd = false;
                showBannerAd(pendingPos, "", "", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initIvyAds() {
        try {
            adManager = (IAdManager) Class.forName("com.ivy.ads.IvyAdsManager").newInstance();
            adManager.setup(ActivityUtil.getInstance().getActivity(), IvyTrack.getInstance(), IvyABManager.getInstance(), buildAdListener(), debug);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void trackAxonEvent(String eventName, Map<String, Object> map) {
        if (adManager != null) {
            adManager.logEvent(eventName, map);
        }
    }

    public boolean hasRewardedAd(String placement, String clientInfo) {
        return adManager != null && adManager.isAvailable(AdType.REWARDED);
    }

    public void loadRewardedAd() {
        if (adManager == null) {
            return;
        }
        adManager.loadAd(AdType.REWARDED);
    }

    public void showRewardedAd(String tag, String placement, String clientInfo) {
        TestManager.getInstance().addEvent("AD", "客户端请求展示激励视频", EventState.NORMAL, placement);
        if (adManager == null) {
            TestManager.getInstance().addEvent("AD", "客户端请求展示激励视频,但广告模块未初始化", EventState.ERROR, placement);
            return;
        }
        adManager.showAd(AdType.REWARDED, tag, placement, clientInfo);
    }

    public boolean hasInterstitialAd(String placement, String clientInfo) {
        return adManager != null && adManager.isAvailable(AdType.INTERSTITIAL);
    }

    public void loadInterstitialAd() {
        if (adManager == null) {
            return;
        }
        adManager.loadAd(AdType.INTERSTITIAL);
    }

    public void showInterstitialAd(String tag, String placement, String clientInfo) {
        TestManager.getInstance().addEvent("AD", "客户端请求展示插屏", EventState.NORMAL, placement);
        if (adManager == null) {
            TestManager.getInstance().addEvent("AD", "客户端请求展示插屏,但广告模块未初始化", EventState.ERROR, placement);
            return;
        }
        adManager.showAd(AdType.INTERSTITIAL, tag, placement, clientInfo);
    }

    public boolean hasBannerAd(String placement, String clientInfo) {
        return true;
    }

    public void loadBannerAd() {

    }

    private boolean pendingBannerAd = false;
    private int pendingPos = 0;

    public void showBannerAd(int position, String tag, String placement, String clientInfo) {
        TestManager.getInstance().addEvent("AD", "客户端请求展示Banner", EventState.NORMAL, placement);
        if (adManager == null) {
            pendingBannerAd = true;
            pendingPos = position;
            TestManager.getInstance().addEvent("AD", "客户端请求展示Banner,但广告模块未初始化", EventState.ERROR, placement);
            return;
        }

        closeBannerAd(placement);
        adManager.showBannerAd(position, tag, placement, clientInfo);
    }

    public void closeBannerAd(String placement) {
        TestManager.getInstance().addEvent("AD", "客户端请求关闭Banner", EventState.NORMAL, placement);
        pendingBannerAd = false;
        if (adManager != null) {
            adManager.closeBannerAd(placement);
        }
    }

    private void appOpenEvent(int step) {
        Bundle b = new Bundle();
        b.putInt("step", step);
        b.putString("event_type", EventType.EVENT_TYPE_COMMON);
        b.putString("event_src", EventSrc.EVENT_SRC_SDK);
        logEvent("app_open", b);
    }

    public void setBXId(String id) {
//        setUserProperty("bx", id, null);
//        LocalStorage.Instance.encodeString("bxId", id);
////        appOpenEvent(1);
//        if (!"old".equals(id)) {
////            appOpenEvent(2);
//            initBlastAds();
//        } else {
////            appOpenEvent(4);
//            initIvyAds();
//        }
    }

    public float getAdPrice(AdType adType) {
        if (adManager != null) {
            return adManager.getPrice(adType);
        } else {
            return 0;
        }
    }
}
