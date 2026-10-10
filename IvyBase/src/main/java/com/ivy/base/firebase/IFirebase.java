package com.ivy.base.firebase;

import org.json.JSONObject;

public interface IFirebase {

    void initFirebaseApp();

    void initRemoteConfig(boolean debug, RemoteConfigCallback callback);

    void setupDefaultRemoteData(String defaultData);

    String getRemoteConfigString(String key);

    double getRemoteConfigDouble(String key);

    boolean getRemoteConfigBoolean(String key);

    long getRemoteConfigLong(String key);

    void setupFirebaseDataAnalytics();

    void runCloudFunctions(String functionName, JSONObject parameters, SuccessCallback success, FailureCallback failure);


    interface RemoteConfigCallback {
        void onResult(boolean state);
    }

    interface SuccessCallback {
        void onSuccess(String data);
    }

    interface FailureCallback {
        void onFailure(String reason);
    }
}
