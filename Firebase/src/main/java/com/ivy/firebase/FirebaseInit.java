package com.ivy.firebase;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
//import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;

import java.util.EnumMap;
import java.util.Map;

public class FirebaseInit {

    private static volatile boolean isInitialized = false;

    public static void initApp() {
        if (isInitialized) return;
        try {
            Context context = App.getInstance();
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
                ILog.d("firebase", "firebase called initialize");
                TestManager.getInstance().addEvent("Firebase", "初始化成功", EventState.NORMAL, "");
                AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                    try {
//                        try {
//                            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
//                            TestManager.getInstance().addEvent("Firebase","Crashlytics初始化成功", EventState.NORMAL, "");
//                        } catch (Throwable t) {
//                            TestManager.getInstance().addEvent("Firebase","Crashlytics初始化失败", EventState.ERROR, t.getMessage());
//                            ILog.e("firebase", "Crashlytics critical error (Build ID missing etc.), skipped: " + t.getMessage());
//                        }

                        FirebaseAnalytics.ConsentStatus status = FirebaseAnalytics.ConsentStatus.GRANTED;
                        Map<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> consentMap = new EnumMap<>(FirebaseAnalytics.ConsentType.class);
                        consentMap.put(FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE, status);
                        consentMap.put(FirebaseAnalytics.ConsentType.AD_STORAGE, status);
                        consentMap.put(FirebaseAnalytics.ConsentType.AD_USER_DATA, status);
                        consentMap.put(FirebaseAnalytics.ConsentType.AD_PERSONALIZATION, status);
                        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
                        analytics.setConsent(consentMap);
                        analytics.setAnalyticsCollectionEnabled(true);
                        TestManager.getInstance().addEvent("Firebase","analytics初始化成功", EventState.NORMAL, "");
                        try {
                            FirebaseMessaging.getInstance().setAutoInitEnabled(true);
                            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    String currentToken = task.getResult();
                                    ILog.d("firebase", "FCM 激活成功，当前有效 fb_push_token: " + currentToken);
                                    LocalStorage.Instance.checkInit();
                                    if (!TextUtils.isEmpty(currentToken)) {
                                        LocalStorage.Instance.encodeString("fb_push_token", currentToken);
                                    }
                                }
                            });
                            TestManager.getInstance().addEvent("Firebase","messaging初始化成功", EventState.NORMAL, "");
                        } catch (Throwable t) {
                            TestManager.getInstance().addEvent("Firebase","messaging初始化失败", EventState.ERROR, t.getMessage());
                            ILog.e("firebase", "激活 FCM 异常: " + t.getMessage());
                        }

                        isInitialized = true;
                        ILog.d("firebase", "firebase integration completely enabled in background thread.");
                    } catch (Exception e) {
                        ILog.e("firebase", "firebase bg enable error: " + e.getMessage());
                        TestManager.getInstance().addEvent("Firebase","模块初始化失败", EventState.ERROR, e.getMessage());
                    }
                });

            } else {
                // 如果已经初始化过了，直接认为就绪
                isInitialized = true;
            }
        } catch (Exception e) {
            ILog.e("firebase", "firebase app call init err:" + e.getMessage());
            TestManager.getInstance().addEvent("Firebase","初始化失败!,检查原因", EventState.ERROR, e.getMessage());
            e.printStackTrace();
        }
    }


    public static boolean isReady() {
        return isInitialized;
    }

}
