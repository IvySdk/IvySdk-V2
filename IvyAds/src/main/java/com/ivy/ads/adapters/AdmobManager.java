package com.ivy.ads.adapters;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdInspectorError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnAdInspectorClosedListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.ivy.ads.interfaces.IvyLoadStatus;
import com.ivy.base.ILog;

import java.util.Arrays;
import java.util.Map;

public class AdmobManager {

    private static final String TAG = "AdmobManager";
    private static boolean initialized = false;
    private static final AdmobManager INSTANCE = new AdmobManager();


    public static synchronized AdmobManager getInstance() {
        return INSTANCE;
    }

    public synchronized void initialize(Activity activity, boolean debug, OnInitializationCompleteListener initializationCompleteListener) {
        if (!initialized) {
            try {
if (debug) {
    MobileAds.setRequestConfiguration(new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("EF71480F7E1C3EEC0D113651166A4080")).build());
}
                MobileAds.initialize(activity, new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                        if (initializationCompleteListener != null){
                            initializationCompleteListener.onInitializationComplete(initializationStatus);
                        }
                        if (debug){
                            MobileAds.openAdInspector(activity, new OnAdInspectorClosedListener() {
                                public void onAdInspectorClosed(@Nullable AdInspectorError error) {
                                    // Error will be non-null if ad inspector closed due to an error.
                                    ILog.e(TAG, "error show ad inspector:" + (error == null ? "" : error.getMessage()));
                                }
                            });
                            Map<String, AdapterStatus> adapterStatusMap = initializationStatus.getAdapterStatusMap();
                            for (Map.Entry<String, AdapterStatus> entry : adapterStatusMap.entrySet()) {
                                ILog.w(TAG, "adapter status: " + entry.getKey() + " >>> " + entry.getValue().getInitializationState());
                            }
                        }

                    }
                });
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
            initialized = true;
        }
    }

    public static final String errorCodeToMessage(int errorCode) {
        String reason = "unknow";
        switch (errorCode) {
            case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                reason = "internal_error";
                break;
            case AdRequest.ERROR_CODE_INVALID_REQUEST:
                reason = "invalid_request";
                break;
            case AdRequest.ERROR_CODE_NETWORK_ERROR:
                reason = "network_error";
                break;
            case AdRequest.ERROR_CODE_NO_FILL:
                reason = IvyLoadStatus.NO_FILL;
                break;
        }
        return reason;
    }
}
