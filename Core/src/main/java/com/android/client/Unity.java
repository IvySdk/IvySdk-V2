package com.android.client;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

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
import com.ivy.core.translate.TranslateUtil;
import com.unity3d.player.UnityPlayer;

import org.json.JSONObject;

import java.util.Map;

public class Unity {

    public static final int AD_FULL = 1;
    public static final int AD_VIDEO = 2;
    public static final int AD_BANNER = 3;
    public static final int AD_NATIVE = 5;
    public static final int AD_GIF_ICON = 6;
    public static final int AD_APPOPEN = 7;

    public static final String TRUE = "1";
    public static final String FALSE = "0";

    private static int getAdType(@Nullable AdType adType) throws Exception {
        int newAdType = 0;
        if (adType == AdType.INTERSTITIAL) {
            newAdType = AD_FULL;
        } else if (adType == AdType.REWARDED) {
            newAdType = AD_VIDEO;
        } else if (adType == AdType.BANNER) {
            newAdType = AD_BANNER;
        }
        return newAdType;
    }

    public static void onCreate(Activity activity) {

        Builder builder = new Builder.Build().setAdListener(new IAdListener() {

            @Override
            public void onInited() {

            }

            @Override
            public void onAdLoadSuccess(@Nullable AdType adType) {
                try {
                    int newAdType = getAdType(adType);
                    sendMessage("onAdLoaded", String.valueOf(newAdType));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            @Override
            public void onAdLoadFailure(@Nullable AdType adType, @Nullable String reason) {
                try {
                    int newAdType = getAdType(adType);
                    sendMessage("onAdLoadFailed", String.valueOf(newAdType));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            @Override
            public void onAdShowSuccess(@NonNull AdType adType, @NonNull String tag, String placement) {
                try {
                    int newAdType = getAdType(adType);
                    String params = newAdType + "|" + tag + "|" + placement;
                    sendMessage("onAdShowSuccess", params);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            @Override
            public void onAdClosed(@NonNull AdType adType, boolean gotReward, @NonNull String tag, String placement) {
                try {
                    int newAdType = getAdType(adType);
                    String params = newAdType + "|" + tag + "|" + placement;
                    sendMessage("onAdClosed", params);
                    if (adType == AdType.REWARDED) {
                        String rewardParams = (gotReward ? TRUE : FALSE) + "|" + tag + "|" + placement;
                        sendMessage("onAdRewardUser", rewardParams);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            @Override
            public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, boolean af) {

            }

            @Override
            public void onAdClicked(@NonNull AdType adType, @NonNull String tag, String placement) {
                try {
                    int newAdType = getAdType(adType);
                    String params = newAdType + "|" + tag + "|" + placement;
                    sendMessage("onAdClicked", params);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            @Override
            public void onAdShowFailed(@NonNull AdType adType, @Nullable String reason, @NonNull String tag, String placement) {
                try {
                    int newAdType = getAdType(adType);
                    String params = newAdType + "|" + tag + "|" + placement;
                    sendMessage("onAdShowFailed", params);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }).setPurchaseListener(new IPurchaseResult() {
            @Override
            public void payResult(int payId, int status, String payload, String merchantTransactionId, String purchase) {
                if (status == IPurchaseResult.PAY_SUCCEED) {
                    if (payload == null) {
                        String params = payId + "|" + (merchantTransactionId == null ? "" : merchantTransactionId);
                        sendMessage("onPaymentSuccess", params);
                    } else {
                        String params = payId + "|" + payload + "|" + (merchantTransactionId == null ? "" : merchantTransactionId);
                        sendMessage("onPaymentSuccessWithPayload", params);
                    }
                } else {
                    String params = payId + "|" + (merchantTransactionId == null ? "" : merchantTransactionId);
                    sendMessage("onPaymentFail", params);
                }
            }

            @Override
            public void onShippingResult(@NonNull String merchantTransactionId, boolean status) {
                sendMessage("onShippingResult", merchantTransactionId + "|" + (status ? TRUE : FALSE));
            }

            @Override
            public void onStoreInitialized(boolean initState) {
                if (initState) {
                    sendMessage("onPaymentSystemValid", "");
                } else {
                    sendMessage("onPaymentSystemError", "");
                }
            }
        }).setHelperListener(new IHelperCallback() {
            @Override
            public void onUnreadHelperMessageCount(int count) {
                sendMessage("unreadHelperMsgCount", String.valueOf(count));
            }

            @Override
            public void onHelperStatus(int status) {
                //status == 1, 打开会话，否则关闭
                sendMessage("onHelperStatus", String.valueOf(status));
            }
        }).setAuthListener(new IAuthResult() {
            @Override
            public void onPlatformsInitialized() {
                sendMessage("onAuthPlatformsInitialized", "");
            }

            @Override
            public void onLoginResult(@NonNull String platform, boolean status, String channel, @Nullable String reason) {
                if (status) {
                    sendMessage("onLoginSuccess", platform);
                } else {
                    sendMessage("onLoginFailed", platform + "|" + (reason == null ? "" : reason));
                }
            }

            @Override
            public void onLogout(@NonNull String platform) {
                sendMessage("onLogout", platform);
            }
        }).setNotificationEventListener(new INotificationEvent() {
            @Override
            public void onReceivedNotificationAction(@NonNull String action) {
                sendMessage("onReceivedNotifyAction", action);
            }
        }).setRemoteConfigSyncListener(new IRemoteConfigSyncListener() {
            @Override
            public void onAbTestConfigsSynced() {
                sendMessage("onAbTestConfigsSynced", "");
            }

            @Override
            public void onRemoteConfigSynced() {
                sendMessage("onRemoteConfigSynced", "");
            }
        }).setPlayGameEventsListener(new IPlayGameEvents() {
            @Override
            public void onReadArchive(String archiveName, String result) {
                sendMessage("onPGArchiveRead", archiveName + "|" + (result == null ? "" : result));
            }

            @Override
            public void onWriteArchive(String archiveName, String actionId, Boolean result) {
                sendMessage("onPGArchiveSet", archiveName + "|" + actionId + "|" + (result ? TRUE : FALSE));
            }

            @Override
            public void onLoadEvents(String data) {
                sendMessage("onPGEventsLoaded", data == null ? "" : data);
            }

            @Override
            public void onLeaderBoardScore(String leaderboardId, String score, String rank) {
                sendMessage("onPGLeaderboardScore", leaderboardId + "|" + score + "|" + rank);
            }
        }).build();
        AndroidSdk.onCreate(activity, builder);
    }

    public static void onStart() {
        AndroidSdk.onStart();
    }

    public static void onResume() {
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

    //广告媒体平台
    public static String getMediaSource() {
        return AndroidSdk.getMediaSource();
    }

    //广告系列名称
    public static String getCampaignName() {
        return AndroidSdk.getCampaignName();
    }

    //广告组名称
    public static String getAdSetName() {
        return AndroidSdk.getAdSetName();
    }

    public static void enableAbTest(int channel) {
        AndroidSdk.enableAbTest(channel);
    }

    public static void enterVariant(String varid) {
        AndroidSdk.enterVariants(varid);
    }

    public static String getABHitResult() {
        return AndroidSdk.getABHitResult();
    }

    public static String getAllAbConfigs() {
        return AndroidSdk.getAllAbConfigs();
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

    public static void startTrackEvent() {
        AndroidSdk.startTrackEvent();
    }

    public static void pullAbTestConfigs() {
        AndroidSdk.pullAbTestConfigs();
    }

    public static String getAbtestSSID() {
        return AndroidSdk.getAbtestSSID();
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

    // 广告

    public static float getInterstitialAdPrice() {
        return IvySdk.getInstance().getAdPrice(AdType.INTERSTITIAL);
    }

    public static float getRewardedAdPrice() {
        return IvySdk.getInstance().getAdPrice(AdType.REWARDED);
    }

    public static void setBXId(String id) {
        AndroidSdk.setBXId(id);
    }

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
        try {
            JSONObject json = new JSONObject();
            json.put("customer_user_id", value);
            setHeaderInfo(json.toString());
        } catch (Exception ignored) {
        }
    }

    //统计

    // firebase cloud function
    public static void firebaseCloudFunction(String functionName) {
        firebaseCloudFunction(functionName, null);
    }

    public static void firebaseCloudFunction(String functionName, String parameters) {
        JSONObject param = null;
        try {
            param = new JSONObject(parameters);
        } catch (Exception e) {
            ILog.e("FirebaseCloudFunction", "unable format cloud function parameter for " + functionName);
        }
        AndroidSdk.firebaseCloudFunctions(functionName, param, new Callback() {
            @Override
            public void onResponse(String data) {
                sendMessage("onCloudFunctionSuccess", functionName + "|" + data);
            }
        }, new Callback() {
            @Override
            public void onResponse(String data) {
                sendMessage("onCloudFunctionFailure", functionName + "|" + data);
            }
        });
    }
    // firebase cloud function

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
        return AndroidSdk.getIvyRemoteConfigString(key);
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

    public static boolean isAuthPlatformReady() {
        return AndroidSdk.isAuthPlatformReady();
    }

    public static boolean isPlayGamesLoggedIn() {
        return AndroidSdk.hasPlayGamesSigned();
    }

    public static void loginPlayGames() {
        AndroidSdk.loginPlayGames();
    }

    public static void logoutPlayGames() {
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

    public static void logInFacebook() {
        AndroidSdk.loginFacebook();
    }

    public static void logoutFacebook() {
        AndroidSdk.logoutFacebook();
    }

    public static boolean isFacebookLoggedIn() {
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

    public static void logoutFirebase() {
        AndroidSdk.logoutFirebase();
    }

    public static String getFirebaseUserInfo(String channel) {
        return AndroidSdk.getFirebaseUserInfo(channel);
    }

    public static String getFirebaseUserId() {
        return AndroidSdk.getFirebaseUserId();
    }

    public static boolean isFirebaseAnonymousLoggedIn() {
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
                if (status) {
                    sendMessage("onFirebaseUnlinkSuccess", unlinkChannel);
                } else {
                    sendMessage("onFirebaseUnlinkFailed", unlinkChannel + "|" + (reason == null ? "" : reason));
                }
            }
        });
    }

    public static void reloadFirebaseLogStatus() {
        AndroidSdk.reloadFirebaseLastSign(new IFirebaseAuthReload() {
            @Override
            public void onReload(boolean status, @Nullable String reason) {
                sendMessage("onReloadFBLoginStatus", (status ? TRUE : FALSE) + "|" + (reason == null ? "" : reason));
            }
        });
    }

    public static void loginFBWithAnonymous() {
        AndroidSdk.loginAnonymous(new IAuthResponse() {

            @Override
            public void onLoginResult(@NonNull String platform, boolean status, String channel, @Nullable String reason) {
                sendMessage("onFirebaseLoginResult", (channel == null ? "" : channel) + "|" + (status ? TRUE : FALSE) + "|" + (reason == null ? "" : reason));
            }
        });
    }

    public static void loginFBWithGoogle() {
        AndroidSdk.loginWithGoogle(new IAuthResponse() {

            @Override
            public void onLoginResult(@NonNull String platform, boolean status, String channel, @Nullable String reason) {
                sendMessage("onFirebaseLoginResult", (channel == null ? "" : channel) + "|" + (status ? TRUE : FALSE) + "|" + (reason == null ? "" : reason));
            }
        });
    }

    public static void loginFBWithPlayGames() {
        AndroidSdk.loginWithPlayGames(new IAuthResponse() {

            @Override
            public void onLoginResult(@NonNull String platform, boolean status, String channel, @Nullable String reason) {
                sendMessage("onFirebaseLoginResult", (channel == null ? "" : channel) + "|" + (status ? TRUE : FALSE) + "|" + (reason == null ? "" : reason));
            }
        });
    }

    public static void loginFBWithFacebook() {
        AndroidSdk.loginWithFacebook(new IAuthResponse() {

            @Override
            public void onLoginResult(@NonNull String platform, boolean status, String channel, @Nullable String reason) {
                sendMessage("onFirebaseLoginResult", (channel == null ? "" : channel) + "|" + (status ? TRUE : FALSE) + "|" + (reason == null ? "" : reason));
            }
        });
    }

    public static void loginFBWithEmailAndPwd(String email, String password) {
        AndroidSdk.loginWithEmailAndPassword(email, password, new IAuthResponse() {

            @Override
            public void onLoginResult(@NonNull String platform, boolean status, String channel, @Nullable String reason) {
                sendMessage("onFirebaseLoginResult", (channel == null ? "" : channel) + "|" + (status ? TRUE : FALSE) + "|" + (reason == null ? "" : reason));
            }
        });
    }

    //存档
    public static void saveCloudData(String collection, String documentId, String jsonData) {
        AndroidSdk.setArchive(collection, documentId, jsonData, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String documentId, @Nullable String data) {
                sendMessage("onCDSaveSuccess", collection + "|" + (documentId == null ? "" : documentId));
            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String documentId, @Nullable String reason) {
                sendMessage("onCDSaveFailed", collection + "|" + (documentId == null ? "" : documentId));
            }
        });
    }

    public static void readCloudData(String collection, String documentId) {
        AndroidSdk.readArchive(collection, documentId, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String documentId, @Nullable String data) {
                sendMessage("onCDReadSuccess", collection + "|" + (documentId == null ? "" : documentId) + "|" + (data == null ? "" : data));
            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String documentId, @Nullable String reason) {
                sendMessage("onCDReadFailed", collection + "|" + (documentId == null ? "" : documentId));
            }
        });
    }

    public static void mergeCloudData(String collection, String documentId, String jsonData) {
        AndroidSdk.mergeArchive(collection, documentId, jsonData, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String documentId, @Nullable String data) {
                sendMessage("onCDMergeSuccess", collection + "|" + (documentId == null ? "" : documentId));
            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String documentId, @Nullable String reason) {
                sendMessage("onCDMergeFailed", collection + "|" + (documentId == null ? "" : documentId));
            }
        });
    }

    public static void queryCloudData(String collection, String documentId) {
        AndroidSdk.queryArchive(collection, documentId, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String documentId, @Nullable String data) {
                sendMessage("onCDQuerySuccess", collection + "|" + (documentId == null ? "" : documentId) + "|" + (data == null ? "" : data));
            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String documentId, @Nullable String reason) {
                sendMessage("onCDQueryFailed", collection + "|" + (documentId == null ? "" : documentId));
            }
        });
    }

    public static void deleteCloudData(String collection, String documentId) {
        AndroidSdk.deleteArchive(collection, documentId, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String documentId, @Nullable String data) {
                sendMessage("onCDDeleteSuccess", collection + "|" + (documentId == null ? "" : documentId));
            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String documentId, @Nullable String reason) {
                sendMessage("onCDDeleteFailed", collection + "|" + (documentId == null ? "" : documentId));
            }
        });
    }

    public static void updateCloudData(String collection, String documentId, String transactionId, String jsonData) {
        AndroidSdk.updateArchive(collection, documentId, jsonData, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String documentId, @Nullable String data) {
                sendMessage("onCDUpdateSuccess", collection + "|" + (documentId == null ? "" : documentId) + "|" + transactionId);
            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String documentId, @Nullable String reason) {
                sendMessage("onCDUpdateFailed", collection + "|" + (documentId == null ? "" : documentId) + "|" + transactionId);
            }
        });
    }

    public static void snapshotCloudData(String collection, String documentId) {
        AndroidSdk.snapshotArchive(collection, documentId, new IArchiveResult() {
            @Override
            public void onSuccess(@NonNull String collection, @Nullable String documentId, @Nullable String data) {
                sendMessage("onCDSnapshotSuccess", collection + "|" + (documentId == null ? "" : documentId));
            }

            @Override
            public void onFailure(@NonNull String collection, @Nullable String document, @Nullable String reason) {
                sendMessage("onCDSnapshotFailed", collection + "|" + (documentId == null ? "" : documentId));
            }
        });
    }
    //存档
    //game services

    //客服 helper
    public static boolean isHelperInitialized() {
        return AndroidSdk.isHelperInitialized();
    }

    public static boolean hasNewHelperMessage() {
        return AndroidSdk.hasNewHelperMessage();
    }

    public static void showHelper(String entranceId, String meta, String tags, String welcomeMessage) {
        AndroidSdk.showHelper(entranceId, meta, tags, welcomeMessage);
    }

    public static void showHelperSingleFAQ(String faqId, int moment) {
        AndroidSdk.showHelperSingleFAQ(faqId, moment);
    }

    public static void listenHelperUnreadMsgCount(boolean onlyOnce) {
        AndroidSdk.listenHelperUnreadMsgCount(onlyOnce);
    }

    public static void stopListenHelperUnreadMsgCount() {
        AndroidSdk.stopListenHelperUnreadMsgCount();
    }

    public static void updateHelperUserInfo(String data, String tags) {
        AndroidSdk.updateHelperUserInfo(data, tags);
    }

    public static void resetHelperUserInfo() {
        AndroidSdk.resetHelperUserInfo();
    }

    public static void updateHelperLanguage(String l) {
        AndroidSdk.updateHelperLanguage(l);
    }

    public static void closeHelper() {
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

    public static void appsflyerInviteUser(String inviterId, String inviterAppId) {
        AndroidSdk.appsflyerInviteUser(inviterId, inviterAppId);
    }

    public static String getAppsflyerInviterId() {
        return AndroidSdk.getAppsflyerInviterId();
    }

    //其它
    public static String getConfig(int configKey) {
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

    public static String encodeData(String data) {
        return AndroidSdk.encodeData(data);
    }

    public static String decodeData(String data) {
        return AndroidSdk.decodeData(data);
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

    public static void forceQuit() {
        AndroidSdk.forceQuit();
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

    /**
     * @param stepDuration 每个震动点的震动时长， 逗号分隔的字符串； 例如 1，2，3
     * @param curve        每个震动点的震动强度，范围在 1~255； 逗号分隔的字符串，例如 1，2，3
     * @implNote stepDuration 和 curve 的长度必须一致
     */
    public static void vibrateWithCure(String stepDuration, String curve) {
        AndroidSdk.vibrateWithCure(stepDuration, curve);
    }

    public static void cancelVibrate() {
        AndroidSdk.cancelVibrate();
    }

    public static void getAdId() {
        AndroidSdk.getAdId(new AdvertisingIdManager.Callback() {
            @Override
            public void onResult(String id) {
                sendMessage("onReceivedAdId", id);
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

    public static void translate(String from_lang, String dst_lang, String data) {
        AndroidSdk.translate(from_lang, dst_lang, data, new TranslateUtil.Action() {
            @Override
            public void onResult(String from, String dst) {
                try {
                    JSONObject json = new JSONObject();
                    json.put("src", from);
                    json.put("dst", dst);
                    sendMessage("onTranslateResult", json.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void sendMessage(String method, String data) {
        try {
            ILog.i("Unity", "send message:");
            ILog.i("Unity", "method:" + method);
            ILog.i("Unity", "data:" + data);
            UnityPlayer.UnitySendMessage("IvySdkListener", method, data);
        } catch (Exception e) {
            ILog.e("Unity", "send message err:" + e.getMessage());
            e.printStackTrace();
        }
    }


}
