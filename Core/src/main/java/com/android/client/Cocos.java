package com.android.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ivy.base.ILog;
import com.ivy.base.ad.AdType;
import com.ivy.base.ad.IAdListener;
import com.ivy.base.billing.IPurchaseResult;
import com.ivy.base.games.IPlayGameEvents;
import com.ivy.base.games.archive.IArchiveResult;
import com.ivy.base.games.auth.IAuthResponse;
import com.ivy.base.games.auth.IAuthResult;
import com.ivy.base.games.auth.IFirebaseAuthReload;
import com.ivy.base.games.auth.IFirebaseUnlink;
import com.ivy.base.helper.IHelperCallback;
import com.ivy.base.util.AdvertisingIdManager;
import com.ivy.core.Builder;
import com.ivy.core.IvySdk;

import org.json.JSONObject;

import java.util.Map;

public class Cocos {

    //ad was clicked
    public static native void awc(int adType, String placement);

    //ad was closed(dismiss)
    public static native void awd(int adType, String placement);

    //ad shows success
    public static native void aws(int adType, String placement);

    //ad shows failed
    public static native void awf(int adType, String placement);

    // receive reward
    public static native void rr(boolean success, String id, String placement, boolean skip);

    // user center message
    public static native void sns(int msg, boolean success, int extra);

    public static native void lar(String tag, boolean success);

    // google result listener
    public static native void gr(int tag, boolean success);

    // home ad loading
    public static native void hal(boolean yes);

    //消费查询
    public static native void ph(String purchaseHistory);

    //订阅是否有效
    public static native void isa(boolean status);

    /**
     * @param bill
     * @param success 0: success , 1: cancel ,2:failure
     */
    public static native void pr(int bill, int success);

    // payment system is valid
    public static native void pv();

    // payment system error
    public static native void pe(String message);

    // bill prices and currency type
    // {"currency":"USD", "1":"0.99", "2":"1.99"}
    public static native void ps(String prices);

    // server result
    public static native void sr(int msg, boolean success, String extra);

    // cache url
    public static native void url(int tag, boolean success, String extra);

    public static native void nd(String data);

    public static native void networkstatus(boolean online);

    public static native void adloaded(int adtype);

    public static native void installreward(String extra);

    public static native void eventoccurred(String eventName);

    public static native void deeplink(String uri);

    // leader board
    public static native void lb(boolean isSubmit, boolean success, String leaderBoardId, String data);

    public static native void deliciouseiconclicked(String bannerLocalUrl, String appstoreUrl);

    public static native void wvsuccess();

    public static native void wvfail();

    public static native void wvclose();

    public static native void wvcall(String params);

    public static native void onAbTestConfigsSynced();

    public static native void rcs();

    public static native void cc(String code);

    public static final int AD_FULL = 1;
    public static final int AD_VIDEO = 2;
    public static final int AD_BANNER = 3;
    public static final int AD_NATIVE = 5;
    public static final int AD_GIF_ICON = 6;
    public static final int AD_APPOPEN = 7;

    private static final String TAG = "cocos";


    public static void onCreate(Activity activity) {
        Log.i(TAG, "onCreate");
        Builder builder = new Builder.Build().setAdListener(new IAdListener() {
            private int getAdType(AdType type) {
                switch (type) {
                    case INTERSTITIAL:
                        return AD_FULL;
                    case BANNER:
                        return AD_BANNER;
                    case REWARDED:
                        return AD_VIDEO;
                    default:
                        return AD_NATIVE;
                }
            }

            @Override
            public void onAdClosed(@NonNull AdType adType, boolean gotReward, @NonNull String tag, @NonNull String placement) {
                try {
                    ILog.d(TAG, "ad closed callback from sdk:" + adType.getValue());
                    int newAdType = getAdType(adType);
                    awd(newAdType, placement);
                    if (adType == AdType.REWARDED) {
                        rr(gotReward, placement, placement, !gotReward);
                        ILog.d(TAG, "ad reward user callback from sdk:" + adType.getValue());
                    }
                } catch (Throwable t) {
                    ILog.d(TAG, "ad closed,but type not defined");
                }
            }

            @Override
            public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, boolean af) {

            }

            @Override
            public void onAdClicked(@NonNull AdType adType, @NonNull String tag, @NonNull String placement) {
                try {
                    int newAdType = getAdType(adType);
                    ILog.d(TAG, "ad click success callback from sdk:" + adType.getValue());
                    awc(newAdType, placement + "");
                } catch (Throwable t) {
                    ILog.d(TAG, "ad click success,but type not defined");
                }
            }

            @Override
            public void onAdShowFailed(@NonNull AdType adType, @Nullable String reason, @NonNull String tag, @NonNull String placement) {
                try {
                    int newAdType = getAdType(adType);
                    awf(newAdType, placement);
                    //                            if (adType == AdType.REWARDED) {
                    //                                rr(false, placement, tag, false);
                    //                                Logger.debug(TAG, "ad reward user callback from sdk:" + adType.getValue());
                    //                            }
                } catch (Throwable t) {
                    ILog.d(TAG, "ad closed,but type not defined");
                }
            }

            @Override
            public void onAdShowSuccess(@NonNull AdType adType, @NonNull String tag, String placement) {
                try {
                    int newAdType = getAdType(adType);
                    ILog.d(TAG, "ad show success callback from sdk:" + adType.getValue());
                    aws(newAdType, placement + "");
                } catch (Throwable t) {
                    ILog.d(TAG, "ad show success,but type not defined");
                }
            }

            @Override
            public void onInited() {

            }

            @Override
            public void onAdLoadSuccess(@Nullable AdType adType) {
                try {
                    int newAdType = getAdType(adType);
                    ILog.d(TAG, "ad load success callback from sdk:" + (adType == null ? "" : adType.getValue()));
                    adloaded(newAdType);
                } catch (Throwable t) {
                    ILog.d(TAG, "ad loaded,but type not defined");
                }
            }

            @Override
            public void onAdLoadFailure(@Nullable AdType adType, @Nullable String reason) {

            }
        }).setPurchaseListener(new IPurchaseResult() {
            @Override
            public void payResult(int payId, int status, String payload, String merchantTransactionId, String purchase) {
                Log.i(TAG, "payResult");
                if (status == IPurchaseResult.PAY_SUCCEED) {
                    pr(payId, 0);
                } else {
                    pr(payId, 2);
                }
            }

            @Override
            public void onShippingResult(@NonNull String merchantTransactionId, boolean status) {

            }

            @Override
            public void onStoreInitialized(boolean initState) {
                try {
                    pv();
                } catch (Throwable t) {
                    try {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    pv();
                                } catch (Throwable t) {
                                }
                            }
                        }, 2000);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }).setHelperListener(new IHelperCallback() {
            @Override
            public void onUnreadHelperMessageCount(int count) {
                //sendMessage("unreadMessageCount", String.valueOf(msgCount));
            }

            @Override
            public void onHelperStatus(int status) {
                //status == 1, 打开会话，否则关闭
            }
        }).setAuthListener(new IAuthResult() {

            @Override
            public void onPlatformsInitialized() {

            }

            @Override
            public void onLoginResult(@NonNull String platform, boolean status, @Nullable String channel, @Nullable String reason) {

            }

            @Override
            public void onLogout(@NonNull String platform) {

            }
        }).setNotificationEventListener(new INotificationEvent() {
            @Override
            public void onReceivedNotificationAction(@NonNull String action) {

            }
        }).setRemoteConfigSyncListener(new IRemoteConfigSyncListener() {
            @Override
            public void onAbTestConfigsSynced() {
                Log.i(TAG, "onAbTestConfigsSynced");
                try {
                    Cocos.onAbTestConfigsSynced();
                } catch (Throwable t) {
                    try {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Cocos.onAbTestConfigsSynced();
                                } catch (Throwable t) {
                                }
                            }
                        }, 2000);
                    } catch (Throwable ignored) {
                    }
                }
            }

            @Override
            public void onRemoteConfigSynced() {
                Log.i(TAG, "onRemoteConfigSynced");
                try {
                    rcs();
                } catch (Throwable t) {
                    try {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    rcs();
                                } catch (Throwable t) {
                                }
                            }
                        }, 2000);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }).setPlayGameEventsListener(new IPlayGameEvents() {
            @Override
            public void onReadArchive(String archiveName, String result) {

            }

            @Override
            public void onWriteArchive(String archiveName, String actionId, Boolean result) {

            }

            @Override
            public void onLoadEvents(String data) {

            }

            @Override
            public void onLeaderBoardScore(String leaderboardId, String score, String rank) {

            }
        }).build();
//        AndroidSdk.onCreate(activity, builder);
        IvySdk.getInstance().onCreate(activity);
        IvySdk.getInstance().onCreate(activity, builder);
    }

//    private static int getAdType(AdType adType) {
//        int newAdType = 0;
//        if (adType == AdType.INTERSTITIAL) {
//            newAdType = AD_FULL;
//        } else if (adType == AdType.REWARDED) {
//            newAdType = AD_VIDEO;
//        } else if (adType == AdType.BANNER) {
//            newAdType = AD_BANNER;
//        }
//        return newAdType;
//    }

    public static void onStart() {
        AndroidSdk.onStart();
    }

    public static void onResume(Activity activity) {
        AndroidSdk.onResume();
    }

    public static void onPause() {
        AndroidSdk.onPause();
    }

    public static void onStop() {
        AndroidSdk.onStop();
    }

    public static void onDestroy() {
        AndroidSdk.onDestroy();
    }

    public static void onNewIntent(Intent intent) {
        AndroidSdk.onNewIntent(intent);
    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        AndroidSdk.onActivityResult(requestCode, resultCode, data);
    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        AndroidSdk.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void setSessionInterval(long interval) {
        AndroidSdk.setSessionInterval(interval);
    }

    public static String getMediaSource() {
        return AndroidSdk.getMediaSource();
    }

    public static String getCampaignName() {
        return AndroidSdk.getCampaignName();
    }

    public static String getAdSetName() {
        return AndroidSdk.getAdSetName();
    }

    public static void enableAbTest(int channel) {
        Log.i(TAG, "enableAbTest:" + channel);
        AndroidSdk.enableAbTest(channel);
    }

    public static void enterVariant(String varid) {
        AndroidSdk.enterVariants(varid);
    }

    public static String getABHitResult() {
        return AndroidSdk.getABHitResult();
    }

    public static String getAbtestSSID() {
        return AndroidSdk.getAbtestSSID();
    }

    public static void setHeaderInfo(String json) {
        AndroidSdk.setHeaderInfo(json);
    }

    public static void setHeaderInfo(String key, String value) {
        try {
            JSONObject json = new JSONObject();
            json.put(key, value);
            setHeaderInfo(json.toString());
        } catch (Exception ignored) {
        }
    }

    public static void setUserInfo(String json) {
        AndroidSdk.setUserInfo(json);
    }

    public static void setHeaderInfoToFirebase(String key, String value) {
        AndroidSdk.setUserProperty(key, value, 1);
    }

    public static void setHeaderInfoToAppsflyer(String key, String value) {
        AndroidSdk.setUserProperty(key, value, 3);
    }

    public static void setHeaderInfoToIvy(String key, String value) {
        AndroidSdk.setUserProperty(key, value, 4);
    }

    public static void startTrackEvent() {
        Log.i(TAG, "startTrackEvent");
        AndroidSdk.startTrackEvent();
    }

    public static void pullAbTestConfigs() {
        Log.i(TAG, "pullAbTestConfigs");
        AndroidSdk.pullAbTestConfigs();
    }

    public static String getAllAbConfigs() {
        return AndroidSdk.getAllAbConfigs();
    }

    public static boolean getAbConfigBoolean(String key, boolean defaultValue) {
        return AndroidSdk.getAbConfigBoolean(key, defaultValue);
    }

    public static int getAbConfigInt(String key, int defaultValue) {
        return AndroidSdk.getAbConfigInt(key, defaultValue);
    }

    public static long getAbConfigLong(String key, long defaultValue) {
        return AndroidSdk.getAbConfigLong(key, defaultValue);
    }

    public static float getAbConfigFloat(String key, float defaultValue) {
        return AndroidSdk.getAbConfigFloat(key, defaultValue);
    }

    public static double getAbConfigDouble(String key, double defaultValue) {
        return AndroidSdk.getAbConfigDouble(key, defaultValue);
    }

    public static String getAbConfigString(String key, String defaultValue) {
        return AndroidSdk.getAbConfigString(key, defaultValue);
    }

    public static void loadCountryCode() {
        AndroidSdk.loadCountryCode(new Callback() {
            @Override
            public void onResponse(String data) {
                cc(data);
            }
        });
    }

    public static void setBXId(String id) {
        Log.i(TAG, "setBXId:" + id);
        AndroidSdk.setBXId(id);
    }

    public static float getInterstitialAdPrice() {
        return IvySdk.getInstance().getAdPrice(AdType.INTERSTITIAL);
    }

    public static float getRewardedAdPrice() {
        return IvySdk.getInstance().getAdPrice(AdType.REWARDED);
    }

    // 广告
    public static void triggerBannerAd(String placement) {
        triggerBannerAd(placement, null);
    }

    public static void triggerBannerAd(String placement, String clientInfo) {
        AndroidSdk.triggerBannerAd(placement, clientInfo);
    }

    public static boolean hasBannerAd() {
        return hasBannerAd("0");
    }

    public static boolean hasBannerAd(String placement) {
        return hasBannerAd(placement, null);
    }

    public static boolean hasBannerAd(String placement, String clientInfo) {
        return AndroidSdk.hasBannerAd(placement, clientInfo);
    }

    public static void loadBannerAd() {
        AndroidSdk.loadBannerAd();
    }

    /**
     * banner 必须传递广告位，否则多banner展示无法操作指定广告位banner
     *
     * @param tag
     * @param pos
     * @param placement
     */
    public static void showBannerAd(String tag, int pos, String placement) {
        showBannerAd(tag, pos, placement, null);
    }

    public static void showBannerAd(String tag, int pos, String placement, String clientInfo) {
        if (tag == null) {
            tag = "default";
        }
        if (TextUtils.isEmpty(placement)) {
            placement = "0";
        }
        AndroidSdk.showBannerAd(tag, pos, placement, clientInfo);
    }

    /**
     * 根据广告位关闭banner
     *
     * @param placement
     */
    public static void closeBannerAd(String placement) {
        AndroidSdk.closeBannerAd(placement);
    }

    public static void triggerInterstitialAd(String placement) {
        triggerInterstitialAd(placement, null);
    }

    public static void triggerInterstitialAd(String placement, String clientInfo) {
        AndroidSdk.triggerInterstitialAd(placement, clientInfo);
    }

    public static boolean hasInterstitialAd() {
        return hasInterstitialAd("0");
    }

    public static boolean hasInterstitialAd(String placement) {
        return hasInterstitialAd(placement, null);
    }

    public static boolean hasInterstitialAd(String placement, String clientInfo) {
        return AndroidSdk.hasInterstitialAd(placement, clientInfo);
    }

    public static void loadInterstitialAd() {
        AndroidSdk.loadInterstitialAd();
    }

    public static void showInterstitialAd(String tag) {
        showInterstitialAd(tag, "0");
    }

    public static void showInterstitialAd(String tag, String placement) {
        showInterstitialAd(tag, placement, null);
    }

    /**
     * @param tag        暂无作用
     * @param placement  广告位 id
     * @param clientInfo 客户端携带信息，要求格式为JSONObject
     */
    public static void showInterstitialAd(String tag, String placement, String clientInfo) {
        if (tag == null) {
            tag = "default";
        }
        if (TextUtils.isEmpty(placement)) {
            placement = "0";
        }
        AndroidSdk.showInterstitialAd(tag, placement, clientInfo);
    }

    public static void triggerRewardedAd(String placement) {
        triggerRewardedAd(placement, null);
    }

    public static void triggerRewardedAd(String placement, String clientInfo) {
        AndroidSdk.triggerRewardedAd(placement, clientInfo);
    }

    public static boolean hasRewardedAd() {
        return hasRewardedAd("0");
    }

    public static boolean hasRewardedAd(String placement) {
        return hasRewardedAd(placement, null);
    }

    public static boolean hasRewardedAd(String placement, String clientInfo) {
        return AndroidSdk.hasRewardedAd(placement, clientInfo);
    }

    public static void loadRewardedAd() {
        AndroidSdk.loadRewardedAd();
    }

    public static void showRewardedAd(String tag) {
        showRewardedAd(tag, "0");
    }

    public static void showRewardedAd(String tag, String placement) {
        showRewardedAd(tag, placement, null);
    }

    /**
     * @param tag        暂无作用
     * @param placement  广告位 id
     * @param clientInfo 客户端携带信息，要求格式为JSONObject
     */
    public static void showRewardedAd(String tag, String placement, String clientInfo) {
        if (tag == null) {
            tag = "default";
        }
        if (TextUtils.isEmpty(placement)) {
            placement = "0";
        }
        AndroidSdk.showRewardedAd(tag, placement, clientInfo);
    }
    // 广告

    //计费
    public static void pay(int id) {
        pay(id, null);
    }

    public static void pay(int id, String payload) {
        pay(id, payload, null);
    }

    public static void pay(int id, String payload, String clientInfo) {
        AndroidSdk.pay(id, payload, clientInfo);
    }

    public static void shippingGoods(String merchantTransactionId) {
        AndroidSdk.shippingGoods(merchantTransactionId);
    }

    public static void queryPaymentOrders() {
        queryPaymentOrder(-1);
    }

    public static void queryPaymentOrder(int id) {
        AndroidSdk.query(id);
    }

    public static String getPaymentData(int id) {
        String result = AndroidSdk.getPurchaseInfo(id);
        return result;
    }

    public static String getPaymentDatas() {
        return AndroidSdk.getPurchaseInfo(-1);
    }

    public static boolean isPaymentValid() {
        return AndroidSdk.isPaymentValid();
    }

    public static void setPaymentUserId(String id) {
        AndroidSdk.setPaymentUserId(id);
    }

    //enjoy pay
    public static boolean isEnjoyPayValid() {
        return AndroidSdk.isEnjoyPayValid();
    }

    public static void enjoyPay(int id) {
        enjoyPay(id, null);
    }

    public static void enjoyPay(int id, String payload) {
        enjoyPay(id, payload, null);
    }

    public static void enjoyPay(int id, String payload, String clientInfo) {
        AndroidSdk.enjoyPay(id, payload, clientInfo);
    }

    public static String getEnjoyPayData(int id) {
        return AndroidSdk.getEnjoyPayPurchaseInfo(id);
    }

    public static String getEnjoyPayDatas() {
        return AndroidSdk.getEnjoyPayPurchaseInfo(-1);
    }

    public static void queryEnjoyPayOrder(int id) {
        AndroidSdk.queryEnjoyPurchase(id);
    }

    public static void queryEnjoyPayOrders() {
        AndroidSdk.queryEnjoyPurchase(-1);
    }

    //计费

    //统计
    public static void trackEvent(String event, String data) {
        AndroidSdk.trackEvent(event, data, 5);
    }

    public static void trackEventToConversion(String event, String data) {
        AndroidSdk.trackEvent(event, data, 5);
    }

    public static void trackEventToFirebase(String event, String data) {
        AndroidSdk.trackEvent(event, data, 1);
    }

    public static void trackEventToFacebook(String event, String data) {
        AndroidSdk.trackEvent(event, data, 2);
    }

    public static void trackEventToAppsflyer(String event, String data) {
        AndroidSdk.trackEvent(event, data, 3);
    }

    public static void trackEventToIvy(String event, String data) {
        AndroidSdk.trackEvent(event, data, 4);
    }

    public static void trackEventToAxon(String event, String data) {
        AndroidSdk.trackEventToAxon(event, data);
    }

    public static void setUserProperty(String key, String value) {
        AndroidSdk.setUserProperty(key, value, 5);
    }

    public static void setUserPropertyToFirebase(String key, String value) {
        AndroidSdk.setUserProperty(key, value, 1);
    }

    public static void setUserPropertyToAppsflyer(String key, String value) {
        AndroidSdk.setUserProperty(key, value, 3);
    }

    public static void setUserPropertyToIvy(String key, String value) {
        AndroidSdk.setUserProperty(key, value, 4);
    }

    public static void setUserPropertyToAxon(String key, String value) {
        AndroidSdk.setUserProperty(key, value, 6);
    }

    public static void setCustomUserId(String value) {
        setUserProperty("customer_user_id", value);
    }
    //统计

    //remote config
    public static int getRemoteConfigInt(String key) {
        return AndroidSdk.getRemoteConfigInt(key);
    }

    public static long getRemoteConfigLong(String key) {
        return AndroidSdk.getRemoteConfigLong(key);
    }

    public static double getRemoteConfigDouble(String key) {
        return AndroidSdk.getRemoteConfigDouble(key);
    }

    public static boolean getRemoteConfigBoolean(String key) {
        return AndroidSdk.getRemoteConfigBoolean(key);
    }

    public static String getRemoteConfigString(String key) {
        return AndroidSdk.getRemoteConfigString(key);
    }

    public static boolean hasIvyRemoteConfigKey(String key) {
        return AndroidSdk.hasIvyRemoteConfigKey(key);
    }

    public static int getIvyRemoteConfigInt(String key) {
        return AndroidSdk.getIvyRemoteConfigInt(key);
    }

    public static String getIvyRemoteConfigString(String key) {
        String value = AndroidSdk.getIvyRemoteConfigString(key);
        ILog.i(TAG, "rc key=" + key + " value=" + value);
        return value;
    }

    public static double getIvyRemoteConfigDouble(String key) {
        return AndroidSdk.getIvyRemoteConfigDouble(key);
    }

    public static boolean getIvyRemoteConfigBoolean(String key) {
        return AndroidSdk.getIvyRemoteConfigBoolean(key);
    }

    public static long getIvyRemoteConfigLong(String key) {
        return AndroidSdk.getIvyRemoteConfigLong(key);
    }
    //remote config

    public static boolean hasPlayGamesSigned() {
        return AndroidSdk.hasPlayGamesSigned();
    }

    public static void signPlayGames() {
        AndroidSdk.loginPlayGames();
    }

    public static void signOutPlayGames() {
        AndroidSdk.logoutPlayGames();
    }

    public static String getPlayGamesUserInfo() {
        return AndroidSdk.getPlayGamesUserInfo();
    }

    public static void unlockAchievement(String achievementId) {
        AndroidSdk.unlockAchievement(achievementId);
    }

    public static void increaseAchievement(String achievementId, int step) {
        AndroidSdk.increaseAchievement(achievementId, step);
    }

    public static void setAchievementStep(String achievementId, int step) {
        AndroidSdk.setAchievementStep(achievementId, step);
    }

    public static void showAchievement() {
        AndroidSdk.showAchievement();
    }

    public static void showLeaderboards() {
        AndroidSdk.showLeaderboards();
    }

    public static void showLeaderboard(String leaderboardId) {
        AndroidSdk.showLeaderboard(leaderboardId);
    }

    public static void updateLeaderboard(String leaderboardId, long score) {
        AndroidSdk.updateLeaderboard(leaderboardId, score);
    }

    public static void loadLeaderboardScore(String leaderboardId) {
        AndroidSdk.loadLeaderboardScore(leaderboardId);
    }

    public static void setPlayGamesArchive(String archiveName, String actionId, String payload) {
        AndroidSdk.savePlayGamesArchive(archiveName, actionId, payload);
    }

    public static void readPlayGamesArchive(String archiveName) {
        AndroidSdk.readPlayGamesArchive(archiveName);
    }

    public static void submitPGActivityEvent(String eventId, int increment) {
        AndroidSdk.submitPlayGamesActivityEvent(eventId, increment);
    }

    public static void loadPGActivityEventById(String eventId) {
        AndroidSdk.loadPlayGamesActivityEventById(eventId);
    }

    public static void loadPGActivityEvents() {
        AndroidSdk.loadPlayGamesActivityEvents();
    }

    public static void submitPlayGameEvent(String eventId, String data) {
        AndroidSdk.submitPlayGameEvent(eventId, data);
    }

    public static void signFacebook() {
        AndroidSdk.loginFacebook();
    }

    public static void signOutFacebook() {
        AndroidSdk.logoutFacebook();
    }

    public static boolean hasFacebookSigned() {
        return AndroidSdk.hasFacebookSigned();
    }

    public static String getFacebookFriends() {
        return AndroidSdk.getFacebookFriends();
    }

    public static String getFacebookUserInfo() {
        return AndroidSdk.getFacebookUserInfo();
    }

    public static void loginGoogle() {
        AndroidSdk.loginGoogle();
    }

    public static void logoutGoogle() {
        AndroidSdk.logoutGoogle();
    }

    public static boolean isGoogleLogged() {
        return AndroidSdk.isGoogleLogged();
    }

    public static String getGoogleUserInfo() {
        return AndroidSdk.getGoogleUserInfo();
    }

    public static String getGoogleUserId() {
        return AndroidSdk.getGoogleUserId();
    }

    public static void signOutFirebase() {
        AndroidSdk.logoutFirebase();
    }

    public static String getFirebaseUserInfo(String channel) {
        return AndroidSdk.getFirebaseUserInfo(channel);
    }

    public static boolean isFirebaseAnonymousSign() {
        return AndroidSdk.isFirebaseAnonymousSign();
    }

    public static boolean isFirebaseLinkedWithChannel(String channel) {
        return AndroidSdk.isFirebaseLinkedWithChannel(channel);
    }

    public static boolean canFirebaseUnlinkWithChannel(String channel) {
        return AndroidSdk.canFirebaseUnlinkWithChannel(channel);
    }

    public static void unlinkFirebaseWithChannel(String channel) {
        AndroidSdk.unlinkFirebaseWithChannel(channel, new IFirebaseUnlink() {
            @Override
            public void onUnlinked(@NonNull String unlinkChannel, boolean status, @Nullable String reason) {
                ILog.i(TAG, "unlink result: " + status + ";channel:" + channel + ";reason:" + (reason == null ? "" : reason));
            }
        });
    }

    public static void reloadFirebaseLastSign() {
        AndroidSdk.reloadFirebaseLastSign(new IFirebaseAuthReload() {
            @Override
            public void onReload(boolean status, @Nullable String reason) {
                ILog.i(TAG, "reload firebase sign status: " + status + " ;reason:" + (reason == null ? "" : reason));
            }
        });
    }

    public static void signAnonymous() {
        AndroidSdk.loginAnonymous(new IAuthResponse() {

            @Override
            public void onLoginResult(@NonNull String platform, boolean status, @Nullable String channel, @Nullable String reason) {
                ILog.i(TAG, "signAnonymous status: " + status + " ;reason:" + (reason == null ? "" : reason));
            }

        });
    }

    public static void signWithGoogle() {
        AndroidSdk.loginWithGoogle(new IAuthResponse() {

            @Override
            public void onLoginResult(@NonNull String platform, boolean status, String channel, @Nullable String reason) {
                ILog.i(TAG, "signWithGoogle status: " + status + " ;reason:" + (reason == null ? "" : reason));
            }
        });
    }

    public static void signWithPlayGames() {
        AndroidSdk.loginWithPlayGames(new IAuthResponse() {

            @Override
            public void onLoginResult(@NonNull String platform, boolean status, String channel, @Nullable String reason) {
                ILog.i(TAG, "signWithPlayGames status: " + status + " ;reason:" + (reason == null ? "" : reason));
            }
        });
    }

    public static void signWithFacebook() {
        AndroidSdk.loginWithFacebook(new IAuthResponse() {

            @Override
            public void onLoginResult(@NonNull String platform, boolean status, String channel, @Nullable String reason) {
                ILog.i(TAG, "signWithFacebook status: " + status + " ;reason:" + (reason == null ? "" : reason));
            }
        });
    }

    public static void signWithEmailAndPassword(String email, String password) {
        AndroidSdk.loginWithEmailAndPassword(email, password, new IAuthResponse() {

            @Override
            public void onLoginResult(@NonNull String platform, boolean status, String channel, @Nullable String reason) {
                ILog.i(TAG, "signWithEmailAndPassword status: " + status + " ;reason:" + (reason == null ? "" : reason));
            }
        });
    }

    //存档
    public static void setArchive(String collection, String documentId, String jsonData) {
        AndroidSdk.setArchive(collection, documentId, jsonData, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String document, @Nullable String data) {

            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String document, @Nullable String reason) {

            }
        });
    }

    public static void readArchive(String collection, String documentId) {
        AndroidSdk.readArchive(collection, documentId, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String document, @Nullable String data) {

            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String document, @Nullable String reason) {

            }
        });
    }

    public static void mergeArchive(String collection, String documentId, String jsonData) {
        AndroidSdk.mergeArchive(collection, documentId, jsonData, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String document, @Nullable String data) {

            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String document, @Nullable String reason) {

            }
        });
    }

    public static void queryArchive(String collection, String docuemntId) {
        AndroidSdk.queryArchive(collection, docuemntId, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String document, @Nullable String data) {

            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String document, @Nullable String reason) {

            }
        });
    }

    public static void deleteArchive(String collection, String documentId) {
        AndroidSdk.deleteArchive(collection, documentId, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String document, @Nullable String data) {

            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String document, @Nullable String reason) {

            }
        });
    }

    public static void updateArchive(String collection, String documentId, String jsonData) {
        AndroidSdk.updateArchive(collection, documentId, jsonData, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String document, @Nullable String data) {

            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String document, @Nullable String reason) {

            }
        });
    }

    public static void snapshotArchive(String collection, String documentId) {
        AndroidSdk.snapshotArchive(collection, documentId, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String document, @Nullable String data) {

            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String document, @Nullable String reason) {

            }
        });
    }
    //存档
    //game services

    //客服
    public static boolean isCustomerInitialized() {
        return AndroidSdk.isHelperInitialized();
    }

    public static boolean hasNewMessage() {
        return AndroidSdk.hasNewHelperMessage();
    }

    public static void showCustomer(String entranceId, String meta, String tags, String welcomeMessag) {
        AndroidSdk.showHelper(entranceId, meta, tags, welcomeMessag);
    }

    public static void showSingleFAQ(String faqId, int moment) {
        AndroidSdk.showHelperSingleFAQ(faqId, moment);
    }

    public static void listenUnreadMessageCount(boolean onlyOnce) {
        AndroidSdk.listenHelperUnreadMsgCount(onlyOnce);
    }

    public static void stopListenUnreadMessageCount() {
        AndroidSdk.stopListenHelperUnreadMsgCount();
    }

    public static void updateCustomerUserInfo(String data, String tags) {
        AndroidSdk.updateHelperUserInfo(data, tags);
    }

    public static void resetCustomerUserInfo() {
        AndroidSdk.resetHelperUserInfo();
    }

    public static void updateCustomerLanguage(String l) {
        AndroidSdk.updateHelperLanguage(l);
    }

    public static void closeCustomer() {
        AndroidSdk.closeHelper();
    }

    //客服

    //通知
    public static int loadNotificationPermissionState() {
        return AndroidSdk.loadNotificationPermissionState();
    }

    public static void requestNotificationPermission() {
        AndroidSdk.requestNotificationPermission();
    }

    public static void openNotificationSettings() {
        AndroidSdk.openNotificationSettings();
    }

    public static void pushNotificationTask(String tag, String title, String subtitle, String bigText, String smallIcon, String largeIcon, String bigPicture, long pushTime, long interval, boolean autoCancel, String action, boolean repeat) {
        AndroidSdk.pushNotificationTask(tag, title, subtitle, bigText, smallIcon, largeIcon, bigPicture, pushTime, interval, autoCancel, action, repeat);
    }

    public static void cancelNotification(String tag) {
        AndroidSdk.cancelNotification(tag);
    }

    public static void cancelAllNotification() {
        AndroidSdk.cancelNotification(null);
    }

    public static void addBootNotification(String tag, String title, String subtitle, String bigText, String smallIcon, String largeIcon, String bigPicture, String action) {
        AndroidSdk.addBootNotification(tag, title, subtitle, bigText, smallIcon, largeIcon, bigPicture, action);
    }

    public static void removeBootNotification(String tag) {
        AndroidSdk.removeBootNotification(tag);
    }

    public static void clearBootNotification() {
        AndroidSdk.removeBootNotification(null);
    }
    //通知

    //其它
    public static String getConfig(int configKey) {
        Log.i(TAG, "getConfig:" + configKey);
        return AndroidSdk.getConfig(configKey);
    }

    public static String getUserInfo(String key) {
        return IvySdk.getInstance().getUserInfo(key);
    }

    public static boolean isNetworkConnected() {
        return AndroidSdk.isNetworkConnected();
    }

    public static void rate() {
        AndroidSdk.rateUs();
    }

    public static void openAppStore(String url, String referrer) {
        AndroidSdk.openAppStore(url, referrer);
    }

    public static void openAppStore() {
        AndroidSdk.openAppStore(null, null);
    }

    public static void copyTxt(String txt) {
        AndroidSdk.copyTxt(txt);
    }

    public static int getTotalMemory() {
        return AndroidSdk.getTotalMemory();
    }

    public static int getFreeMemory() {
        return AndroidSdk.getFreeMemory();
    }

    public static int getDiskSize() {
        return AndroidSdk.getDiskSize();
    }

    public static int getFreeDiskSize() {
        return AndroidSdk.getFreeDiskSize();
    }

    public static void sendEmail(String email, String extra) {
        sendEmail(email, null, extra);
    }

    public static void sendEmail(String email, String title, String extra) {
        AndroidSdk.sendEmail(email, title, extra);
    }

    public static void systemShareText(String txt) {
        AndroidSdk.systemShareText(txt);
    }

    public static void systemShareImage(String title, String imagePath) {
        AndroidSdk.systemShareImage(title, imagePath);
    }

    public static void openUrl(String url) {
        AndroidSdk.openUrl(url);
    }

    public static void showWebView(String title, String url) {
        AndroidSdk.showWebView(title, url);
    }

    public static boolean hasNotch() {
        return AndroidSdk.hasNotch();
    }

    public static int getNotchHeight() {
        return AndroidSdk.getNotchHeight();
    }

    public static void setDisplayInNotch() {
        AndroidSdk.displayInNotch();
    }

    public static boolean hasGestureBar() {
        return AndroidSdk.hasGestureBar();
    }

    public static int getGestureBarHeight() {
        return AndroidSdk.getGestureBarHeight();
    }

    public static void toast(String msg) {
        AndroidSdk.toast(msg);
    }

    public static long getAppInstallTimestamp() {
        return AndroidSdk.getAppInstallTimestamp();
    }

    public static void openFacebookPage(String pageId) {
        AndroidSdk.openFacebookPage(pageId);
    }

    public static boolean isDebug() {
        return AndroidSdk.isDebug();
    }

    public static void goLauncher() {
        AndroidSdk.goLauncher();
    }

    public static boolean isNightMode() {
        return AndroidSdk.isNightMode();
    }

    public static void addShortcut(String id, String title, String label, String icon) {
        AndroidSdk.addShortcut(id, title, label, icon);
    }

    public static void deleteShortcut(String id) {
        AndroidSdk.deleteShortcut(id);
    }

    public static String getShortcutAction() {
        return AndroidSdk.getShortcutAction();
    }

    public static String getNotificationAction() {
        return AndroidSdk.getNotificationAction();
    }

    /**
     * @param duration  震动时长，单位毫秒
     * @param amplitude 震动强度， 1 ~ 255
     */
    public static void vibrate(long duration, int amplitude) {
        AndroidSdk.vibrate(duration, amplitude);
    }

    public static void cancelVibrate() {
        AndroidSdk.cancelVibrate();
    }

    /**
     * @param stepDuration 每个震动点的震动时长， 逗号分隔的字符串； 例如 1，2，3
     * @param curve        每个震动点的震动强度，范围在 1~255； 逗号分隔的字符串，例如 1，2，3
     * @implNote stepDuration 和 curve 的长度必须一致
     */
    public static void vibrateWithCure(String stepDuration, String curve) {
        AndroidSdk.vibrateWithCure(stepDuration, curve);
    }

    public static void getAdId() {
        AndroidSdk.getAdId(new AdvertisingIdManager.Callback() {
            @Override
            public void onResult(String id) {

            }
        });
    }

    /**
     * 上传轨迹
     *
     * @param levelId  关卡id
     * @param userTier 用户分层
     * @param data     轨迹数据
     */
    public static void uploadTrajectory(int levelId, String userTier, String data) {
        AndroidSdk.uploadTrajectory(levelId, userTier, data);
    }


}
