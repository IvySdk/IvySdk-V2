package com.ivy.ads.cmp;

import android.app.Activity;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class CMPUtil {

    private static volatile CMPUtil instance = null;

    private final List<ICMPCallback> pendingCallbacks = new ArrayList<>();

    private volatile boolean ableLoadAds = false;
    private AtomicBoolean isChecked = new AtomicBoolean(false);

    private AtomicBoolean isChecking = new AtomicBoolean(false);

    private CMPUtil() {
    }

    public static CMPUtil getInstance() {
        if (instance == null) {
            synchronized (CMPUtil.class) {
                if (instance == null) {
                    instance = new CMPUtil();
                }
            }
        }
        return instance;
    }

    private ConsentInformation consentInformation;
//    private final String TAG = "CMP";

    public void init(Activity activity, boolean debug, ICMPCallback callback) {
        if (isChecked.get()) {
            if (callback != null) callback.onResult(ableLoadAds);
            return;
        }
        synchronized (pendingCallbacks) {
            if (callback != null) {
                pendingCallbacks.add(callback);
            }
        }
        if (!isChecking.compareAndSet(false, true)) {
            return;
        }
        ConsentRequestParameters.Builder builder = new ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false);
        if (debug) {
            ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(activity).setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA).addTestDeviceHashedId("F3EDE78A2C3C4127A07CA5E97F0FDD02").build();
            builder.setConsentDebugSettings(debugSettings);
        }
        ConsentRequestParameters params = builder.build();
        consentInformation = UserMessagingPlatform.getConsentInformation(activity);
//        if (consentInformation.canRequestAds()) {
//            onResult(true);
//            return;
//        }
        consentInformation.requestConsentInfoUpdate(activity, params, () -> {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, (loadAndShowError) -> {
                onResult(consentInformation.canRequestAds());
            });
        }, (requestFormError) -> {
            onResult(consentInformation.canRequestAds());
        });

    }

    private void onResult(boolean result) {
        isChecking.set(false);
        ableLoadAds = result;
        isChecked.set(true);
        dispatchResult();
    }

    private void dispatchResult() {
        List<ICMPCallback> callbacksCopy;
        synchronized (pendingCallbacks) {
            callbacksCopy = new ArrayList<>(pendingCallbacks);
            pendingCallbacks.clear();
        }

        for (ICMPCallback cb : callbacksCopy) {
            cb.onResult(ableLoadAds);
        }
    }

    public void reset() {
        if (consentInformation != null) {
            consentInformation.reset();
            isChecked.set(false);
            isChecking.set(false);
            ableLoadAds = false;
        }
    }

}
