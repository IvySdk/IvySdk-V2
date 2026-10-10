package com.ivy.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.analytics.FirebaseAnalytics.ConsentType;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.firebase.IFirebase;
import com.ivy.base.grid.GridManager;

import org.json.JSONObject;

import java.util.EnumMap;
import java.util.Map;

// 如果再次出现firebase在启动阶段的anr，尝试禁用firebase自启动
public class FirebaseImpl implements IFirebase, IUnProguard {

    private FbRemoteConfig remoteConfig;

    public FirebaseImpl() {

    }

    @Override
    public void initFirebaseApp() {
        FirebaseInit.initApp();
        createFCMChannel();
        listenerInstallReferrer();
    }

    private void createFCMChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 这里的 ID 必须和 Manifest 中的 value 一致
                String channelId = "notice_channel";
                String channelName = "Important Notice";
                int importance = NotificationManager.IMPORTANCE_HIGH;

                NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
                channel.setDescription("Used to display important in-game system notifications");

                NotificationManager notificationManager = App.getInstance().getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
                ILog.d("firebase", "default push channel created");
            }
        } catch (Exception e) {
            ILog.e("firebase", "create default push channel failed:" + e.getMessage());
        }
    }

    private void listenerInstallReferrer(){
//        FirebaseAnalytics.getInstance(App.getInstance()).getAppInstanceId().addOnCompleteListener(new OnCompleteListener<String>() {
//            @Override
//            public void onComplete(@NonNull Task<String> task) {
//                if (task.isSuccessful()){
//                    String result = task.getResult();
//                    ILog.d("firebase", "安装来源:" + result);
//                } else {
//                    ILog.e("firebase", "获取安装来源失败:" + (task.getException() == null ? "unknown" : task.getException().getMessage()));
//                }
//            }
//        });
    }

    @Override
    public void initRemoteConfig(final boolean debug, final RemoteConfigCallback callback) {
        if (remoteConfig == null) {
            remoteConfig = new FbRemoteConfig();
        }
        remoteConfig.setup(debug, callback);
    }

    @Override
    public void setupDefaultRemoteData(String defaultData) {
        if (remoteConfig != null) {
            remoteConfig.setupDefaultRemoteData(defaultData);
        }
    }

    @Override
    public String getRemoteConfigString(String key) {
        if (remoteConfig != null) {
            String value = remoteConfig.getRemoteConfigString(key);
            if (value != null) return value;
        }
        return FirebaseRemoteConfig.DEFAULT_VALUE_FOR_STRING;
    }

    @Override
    public double getRemoteConfigDouble(String key) {
        if (remoteConfig != null) {
            return remoteConfig.getRemoteConfigDouble(key);
        }
        return FirebaseRemoteConfig.DEFAULT_VALUE_FOR_DOUBLE;
    }

    @Override
    public boolean getRemoteConfigBoolean(String key) {
        if (remoteConfig != null) {
            return remoteConfig.getRemoteConfigBoolean(key);
        }
        return FirebaseRemoteConfig.DEFAULT_VALUE_FOR_BOOLEAN;
    }

    @Override
    public long getRemoteConfigLong(String key) {
        if (remoteConfig != null) {
            return remoteConfig.getRemoteConfigLong(key);
        }
        return FirebaseRemoteConfig.DEFAULT_VALUE_FOR_LONG;
    }

    @Override
    public void setupFirebaseDataAnalytics() {
        ILog.d("firebase", "setupFirebaseDataAnalytics moved to background init task.");
//        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
//            try {
//                FirebaseAnalytics.ConsentStatus status = FirebaseAnalytics.ConsentStatus.GRANTED;
//                Map<ConsentType, FirebaseAnalytics.ConsentStatus> consentMap = new EnumMap<>(ConsentType.class);
//                consentMap.put(ConsentType.ANALYTICS_STORAGE, status);
//                consentMap.put(ConsentType.AD_STORAGE, status);
//                consentMap.put(ConsentType.AD_USER_DATA, status);
//                consentMap.put(ConsentType.AD_PERSONALIZATION, status);
//
//                // 子线程首次 getInstance 是安全的，SharedPreferences 读写不会卡死 UI
//                FirebaseAnalytics.getInstance(App.getInstance()).setConsent(consentMap);
//                ILog.d("firebase", "Firebase Analytics Consent set successfully in background.");
//            } catch (Exception e) {
//                ILog.e("firebase", "set Consent failed: " + e.getMessage());
//            }
//        });
    }

    @Override
    public void runCloudFunctions(final String functionName, JSONObject parameters, final SuccessCallback success, final FailureCallback failure) {
        String regionOrCustomDomain = GridManager.getInstance().getGridConfig("cf.location", "us-central1");
        if (regionOrCustomDomain == null) {
            ILog.e("FirebaseCloudFunction", "invalid regionOrCustomDomain");
            failure.onFailure("invalid region or custom domain");
            return;
        }

        FirebaseFunctions.getInstance(regionOrCustomDomain).getHttpsCallable(functionName).call(parameters).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                try {
                    HttpsCallableResult result = task.getResult();
                    Object data = result.getData();
                    ILog.i("FirebaseCloudFunction", functionName + " got response:" + data);
                    String jsonObject = JSON.toJSONString(data);
                    if (jsonObject != null) {
                        ILog.e("FirebaseCloudFunction", functionName + " success with response:" + jsonObject);
                        success.onSuccess(jsonObject);
                    } else {
                        ILog.e("FirebaseCloudFunction", functionName + " failed:invalid response");
                        failure.onFailure("invalid response");
                    }
                } catch (Exception e) {
                    ILog.e("FirebaseCloudFunction", functionName + " failed:invalid response");
                    failure.onFailure("invalid response");
                }
            } else {
                String reason = (task.getException() != null) ? task.getException().getMessage() : "fail";
                ILog.e("FirebaseCloudFunction", functionName + " failed:" + reason);
                failure.onFailure(reason);
            }
        });
    }


}
