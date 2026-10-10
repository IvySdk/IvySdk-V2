package com.ivy.ads.adapters;


import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.DTBAdNetwork;
import com.amazon.device.ads.DTBAdNetworkInfo;
import com.amazon.device.ads.MRAIDPolicy;
import com.applovin.mediation.MaxSegment;
import com.applovin.mediation.MaxSegmentCollection;
import com.applovin.sdk.AppLovinCmpError;
import com.applovin.sdk.AppLovinCmpService;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkInitializationConfiguration;
import com.ivy.ads.SegmentManager;
import com.ivy.ads.interfaces.IvyLoadStatus;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.track.ITrack;
import com.ivy.base.util.Logger;

import java.util.List;
import java.util.Map;
//import com.pubmatic.sdk.common.OpenWrapSDK;
//import com.pubmatic.sdk.common.models.POBApplicationInfo;
//
//import java.net.URL;


public class ApplovinManager {

    public interface IApplovinInitCallback {
        void onInit();
    }

    private static final String TAG = "ApplovinManager";
    private static boolean testMode = false;
    private static boolean initialized = false;
    private static final ApplovinManager INSTANCE = new ApplovinManager();

    ApplovinManager() {
    }

    public synchronized void initialize(Activity activity, SegmentManager segmentManager, ITrack tracker, IApplovinInitCallback callback) {
        if (activity == null) {
            return;
        }
        if (!initialized) {
            try {
                Bundle bundle = new Bundle();
                bundle.putString("event_src", "sdk");
                bundle.putString("event_type", "common");
                tracker.logEvent("sdk_ads_init", bundle);
                AppLovinSdk sdk = AppLovinSdk.getInstance(activity.getApplicationContext());

                if (!sdk.isInitialized()) {
                    try {
                        ApplicationInfo ai = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
                        if (ai != null && ai.metaData != null) {
                            testMode = ai.metaData.getBoolean("ivy.debug", false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String applovin_sdk_key = "";
                    try {
                        // Amazon requires an 'Activity' instance
                        ApplicationInfo ai = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
                        if (ai != null && ai.metaData != null) {
                            applovin_sdk_key = ai.metaData.getString("sdk.applovin.sdk.key");
                        }
                        String amazonAppId = null;
                        if (ai != null && ai.metaData != null) {
                            Object o = ai.metaData.get("aps.id");
                            if (o instanceof String) {
                                amazonAppId = String.valueOf(o);
                            }
                        }
                        if (TextUtils.isEmpty(amazonAppId))
                            throw new NullPointerException("Amazon app Id can't be null!!!");
                        AdRegistration.getInstance(amazonAppId, activity.getApplicationContext());
//                        AdRegistration.setAdNetworkInfo(new DTBAdNetworkInfo(DTBAdNetwork.MAX));
                        AdRegistration.setMRAIDSupportedVersions(new String[]{"1.0", "2.0", "3.0"});
                        AdRegistration.setMRAIDPolicy(MRAIDPolicy.CUSTOM);
                        /** amazon ad 测试 */
                        if (testMode) {
                            AdRegistration.enableTesting(true);
                            AdRegistration.enableLogging(true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

//                    try {
//                        POBApplicationInfo appInfo = new POBApplicationInfo();
//                        String pkg = activity.getApplicationContext().getApplicationInfo().packageName;
//                        appInfo.setStoreURL(new URL("https://play.google.com/store/apps/details?id=" + pkg));
//                        OpenWrapSDK.setApplicationInfo(appInfo);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }

                    if (!AppLovinPrivacySettings.isUserConsentSet(activity.getApplicationContext())) {
                        AppLovinPrivacySettings.setHasUserConsent(true, activity.getApplicationContext());
                    }

//                    if (!AppLovinPrivacySettings.isAgeRestrictedUser(activity.getApplicationContext())) {
//                        AppLovinPrivacySettings.setIsAgeRestrictedUser(false, activity.getApplicationContext());
//                    }

                    if (!AppLovinPrivacySettings.isDoNotSellSet(activity.getApplicationContext())) {
                        AppLovinPrivacySettings.setDoNotSell(true, activity.getApplicationContext());
                    }

                    AppLovinSdk.getInstance(activity).getSettings().setCreativeDebuggerEnabled(false);

                    AppLovinSdkInitializationConfiguration.Builder initConfig = AppLovinSdkInitializationConfiguration.builder(applovin_sdk_key, activity)
                            .setMediationProvider(AppLovinMediationProvider.MAX);
                    if (segmentManager != null) {
                        Map<Integer, List<Integer>> segments = segmentManager.getSegments();
                        if (segments != null && segments.size() > 0) {
                            MaxSegmentCollection.Builder segmentCollectionBuilder = MaxSegmentCollection.builder();
                            for (Integer key : segments.keySet()) {
                                List<Integer> value = segments.get(key);
                                if (value != null) {
                                    segmentCollectionBuilder.addSegment(new MaxSegment(key, value));
                                }
                            }

                            initConfig.setSegmentCollection(segmentCollectionBuilder.build());
                        }
                    }

                AppLovinSdk.getInstance(activity.getApplicationContext()).initialize(initConfig.build(), new AppLovinSdk.SdkInitializationListener() {
                        @Override
                        public void onSdkInitialized(AppLovinSdkConfiguration appLovinSdkConfiguration) {
                            Logger.debug(TAG, "Applovin initialized");
                            if (testMode) {
                                /** 以下部分需要放在初始化成功回调内部，否则debugger内测试广告会出现 max sdk未初始化的错误  */
                                AppLovinSdk.getInstance(activity.getApplicationContext()).showMediationDebugger();
                                AppLovinSdk.getInstance(activity.getApplicationContext()).getSettings().setVerboseLogging(true);
                            }
                            Bundle bundle1 = new Bundle();
                            bundle1.putString("event_src", "sdk");
                            bundle1.putString("event_type", "common");
                            tracker.logEvent("sdk_ads_complete", bundle1);

                            boolean hadShown = LocalStorage.Instance.decodeBoolean("CMPHadShown", false);
                            if (hadShown) {
                                if (callback != null) {
                                    callback.onInit();
                                }
                            } else {
                                AppLovinCmpService cmpService = AppLovinSdk.getInstance(activity).getCmpService();
                                if (cmpService.hasSupportedCmp()) {
                                    cmpService.showCmpForExistingUser(activity, new AppLovinCmpService.OnCompletedListener() {
                                        @Override
                                        public void onCompleted(@Nullable AppLovinCmpError appLovinCmpError) {
                                            if (callback != null) {
                                                callback.onInit();
                                            }
                                            if (appLovinCmpError == null) {
                                                LocalStorage.Instance.encodeBoolean("CMPHadShown", true);
                                            }
                                        }
                                    });
                                } else {
                                    if (callback != null) {
                                        callback.onInit();
                                    }
                                }
                            }
                        }
                    });
                }
                initialized = true;
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }

    public static synchronized ApplovinManager getInstance() {
        return INSTANCE;
    }

    public boolean isInitialized() {
        return initialized;
    }


    public static String errorCodeToMessage(int errorCode) {
        String reason = "unknow";
        switch (errorCode) {
            case AppLovinErrorCodes.SDK_DISABLED:
                reason = "sdk_disabled";
                break;
            case AppLovinErrorCodes.FETCH_AD_TIMEOUT:
                reason = "fetch_ad_timeout";
                break;
            case AppLovinErrorCodes.NO_FILL:
                reason = IvyLoadStatus.NO_FILL;
                break;
            case AppLovinErrorCodes.INVALID_ZONE:
                reason = "invalid_zone";
                break;
            case AppLovinErrorCodes.NO_NETWORK:
                reason = "no_network";
                break;
            case AppLovinErrorCodes.UNABLE_TO_RENDER_AD:
                reason = "unable_to_render_ad";
                break;
            case AppLovinErrorCodes.UNSPECIFIED_ERROR:
                reason = "unspecified_error";
                break;
        }
        return reason;
    }
}
