package com.android.client;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;

import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.games.archive.IArchiveResult;
import com.ivy.base.games.auth.IAuthResponse;
import com.ivy.base.games.auth.IFirebaseAuthReload;
import com.ivy.base.games.auth.IFirebaseUnlink;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;
import com.ivy.base.track.TrackPlatform;
import com.ivy.base.util.AdvertisingIdManager;
import com.ivy.base.util.AppStoreUtil;
import com.ivy.base.util.IToast;
import com.ivy.base.util.IvyUtil;
import com.ivy.base.util.ShortcutUtil;
import com.ivy.base.util.Util;
import com.ivy.base.util.VibrationUtil;
import com.ivy.core.Builder;
import com.ivy.core.IvySdk;
import com.ivy.core.ab.IvyABManager;
import com.ivy.core.ab.LocalAB;
import com.ivy.core.trajectory.TrajectoryManager;
import com.ivy.core.translate.TranslateUtil;
import com.ivy.core.util.Helper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AndroidSdk {
    private static final String TAG = "";
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static void onCreate(Activity activity, Builder builder) {
        runUiThread(() -> {
            IvySdk.getInstance().onCreate(activity, builder);
        });
    }

    public static void onStart() {
        runUiThread(() -> {
            IvySdk.getInstance().onStart();
        });
    }

    public static void onResume() {
        runUiThread(() -> {
            IvySdk.getInstance().onResume();
        });
    }

    public static void onPause() {
        runUiThread(() -> {
            IvySdk.getInstance().onPause();
        });
    }

    public static void onStop() {
        runUiThread(() -> {
            IvySdk.getInstance().onStop();
        });
    }

    public static void onDestroy() {
        runUiThread(() -> {
            IvySdk.getInstance().onDestroy();
        });
    }

    public static void onNewIntent(Intent intent) {
        runUiThread(() -> {
            IvySdk.getInstance().onNewIntent(intent);
        });
    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        runUiThread(() -> {
            IvySdk.getInstance().onActivityResult(requestCode, resultCode, data);
        });
    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        runUiThread(() -> {
            IvySdk.getInstance().onRequestPermissionsResult(requestCode, permissions, grantResults);
        });
    }

    public static void setSessionInterval(long interval) {
        runUiThread(() -> {
            IvySdk.getInstance().setSessionInterval(interval);
        });
    }

    //广告媒体平台
    public static String getMediaSource() {
        return IvySdk.getInstance().getMediaSource();
    }

    //广告系列名称
    public static String getCampaignName() {
        return IvySdk.getInstance().getCampaignName();
    }

    //广告组名称
    public static String getAdSetName() {
        return IvySdk.getInstance().getAdSetName();
    }

    public static void enableAbTest(int channel) {
        runUiThread(() -> {
            IvySdk.getInstance().enableAbTest(channel);
        });
    }

    public static void setHeaderInfo(String json) {
        runUiThread(() -> {
            IvySdk.getInstance().setHeaderInfo(json);
        });
    }

    public static void setUserInfo(String json) {
        runUiThread(() -> {
            IvySdk.getInstance().setUserInfo(json);
        });
    }

    public static void startTrackEvent() {
        runUiThread(() -> {
            IvySdk.getInstance().startTrackEvent();
        });
    }

    public static void pullAbTestConfigs() {
        runUiThread(() -> {
            IvySdk.getInstance().pullAbTestConfigs();
        });
    }

    public static String getAbtestSSID() {
        return IvySdk.getInstance().getAbtestSSID();
    }

    public static String getAllAbConfigs() {
        return IvyABManager.getInstance().getAllAbConfigs();
    }

    public static boolean getAbConfigBoolean(String key, boolean defaultValue) {
        return IvySdk.getInstance().getAbConfigBoolean(key, defaultValue);
    }

    public static int getAbConfigInt(String key, int defaultValue) {
        return IvySdk.getInstance().getAbConfigInt(key, defaultValue);
    }

    public static long getAbConfigLong(String key, long defaultValue) {
        return IvySdk.getInstance().getAbConfigLong(key, defaultValue);
    }

    public static float getAbConfigFloat(String key, float defaultValue) {
        return IvySdk.getInstance().getAbConfigFloat(key, defaultValue);
    }

    public static double getAbConfigDouble(String key, double defaultValue) {
        return IvySdk.getInstance().getAbConfigDouble(key, defaultValue);
    }

    public static String getAbConfigString(String key, String defaultValue) {
        return IvySdk.getInstance().getAbConfigString(key, defaultValue);
    }

    public static void loadCountryCode(Callback callback) {
        runUiThread(() -> {
            IvySdk.getInstance().loadCountryCode(s -> {
                if (callback != null) {
                    callback.onResponse(s == null ? "" : s);
                }
            });
        });
    }

    public static void setBXId(String id) {
        runUiThread(() -> {
            IvySdk.getInstance().setBXId(id);
        });
    }

    // 广告
    public static void triggerBannerAd(String placement, String clientInfo) {
        runUiThread(() -> {
            IvySdk.getInstance().triggerBannerAd(placement, clientInfo);
        });
    }

    public static boolean hasBannerAd(String placement, String clientInfo) {
        return IvySdk.getInstance().hasBannerAd(placement, clientInfo);
    }

    public static void loadBannerAd() {
        runUiThread(() -> {
            IvySdk.getInstance().loadBannerAd();
        });
    }

    public static void showBannerAd(String tag, int pos, String placement, String clientInfo) {
        runUiThread(() -> {
            IvySdk.getInstance().showBannerAd(pos, tag, placement, clientInfo);
        });
    }

    public static void closeBannerAd(String placement) {
        runUiThread(() -> {
            IvySdk.getInstance().closeBannerAd(placement);
        });
    }

    public static void triggerInterstitialAd(String placement, String clientInfo) {
        runUiThread(() -> {
            IvySdk.getInstance().triggerInterstitialAd(placement, clientInfo);
        });
    }

    public static boolean hasInterstitialAd(String placement, String clientInfo) {
        return IvySdk.getInstance().hasInterstitialAd(placement, clientInfo);
    }

    public static void loadInterstitialAd() {
        runUiThread(() -> {
            IvySdk.getInstance().loadInterstitialAd();
        });
    }

    public static void showInterstitialAd(String tag, String placement, String clientInfo) {
        runUiThread(() -> {
            IvySdk.getInstance().showInterstitialAd(tag, placement, clientInfo);
        });
    }

    public static void triggerRewardedAd(String placement, String clientInfo) {
        runUiThread(() -> {
            IvySdk.getInstance().triggerRewardedAd(placement, clientInfo);
        });
    }

    public static boolean hasRewardedAd(String placement, String clientInfo) {
        return IvySdk.getInstance().hasRewardedAd(placement, clientInfo);
    }

    public static void loadRewardedAd() {
        runUiThread(() -> {
            IvySdk.getInstance().loadRewardedAd();
        });
    }

    public static void showRewardedAd(String tag, String placement, String clientInfo) {
        runUiThread(() -> {
            IvySdk.getInstance().showRewardedAd(tag, placement, clientInfo);
        });
    }
    // 广告

    //计费
    public static void pay(int id, String payload, String clientInfo) {
        runUiThread(() -> {
            IvySdk.getInstance().pay(id, payload, clientInfo);
        });
    }

    public static void shippingGoods(String merchantTransactionId) {
        runUiThread(() -> {
            IvySdk.getInstance().shippingGoods(merchantTransactionId);
        });
    }

    public static void query(int id) {
        runUiThread(() -> {
            IvySdk.getInstance().queryPurchase(id);
        });
    }

    public static String getPurchaseInfo(int id) {
        return IvySdk.getInstance().getGoodsInfo(id);
    }

    public static boolean isPaymentValid() {
        return IvySdk.getInstance().isPaymentInitialized();
    }

    public static void setPaymentUserId(String id) {
        runUiThread(() -> {
            IvySdk.getInstance().setPaymentUserId(id);
        });
    }

    public static boolean isEnjoyPayValid() {
        return IvySdk.getInstance().isEnjoyPayInitialized();
    }

    public static void enjoyPay(int id, String payload, String clientInfo) {
        runUiThread(() -> {
            IvySdk.getInstance().enjoyPay(id, payload, clientInfo);
        });
    }

    public static String getEnjoyPayPurchaseInfo(int id) {
        return IvySdk.getInstance().getEnjoyPayGoodsInfo(id);
    }

    public static void queryEnjoyPurchase(int id) {
        runUiThread(() -> {
            IvySdk.getInstance().queryEnjoyPurchase(id);
        });
    }

    //计费

    //统计
    public static void trackEvent(String event, String data, int platform) {
        runUiThread(() -> {
            Map<String, Object> params = new HashMap<>();
            if (TextUtils.isEmpty(data)) {
                params = null;
            } else {
                try {
                    params = new HashMap<>();
                    JSONObject obj = new JSONObject(data);
                    Iterator<String> keys = obj.keys();
                    while (keys.hasNext()) {
                        String itemKey = keys.next();
                        Object itemValue = obj.get(itemKey);
                        if (itemValue instanceof Boolean) {
                            params.put(itemKey, ((Boolean) itemValue ? 1 : 0));
                        } else {
                            params.put(itemKey, itemValue);
                        }
                    }
//                    String[] args = data.split(",");
//                    if (args.length > 0 && args.length % 2 == 0) {
//                        for (int i = 0; i < args.length; i += 2) {
//                            String vvkey = args[i].toLowerCase();
//                            Object value = args[i + 1];
//                            try {
//                                params.put(vvkey, Double.parseDouble(value.toString()));
//                                continue;
//                            } catch (Exception ignored) {
//                            }
//
//                            try {
//                                params.put(vvkey, Float.parseFloat(value.toString()));
//                                continue;
//                            } catch (Exception ignored) {
//                            }
//
//                            try {
//                                params.put(vvkey, Long.parseLong(value.toString()));
//                                continue;
//                            } catch (Exception ignored) {
//                            }
//
//                            try {
//                                params.put(vvkey, Integer.parseInt(value.toString()));
//                                continue;
//                            } catch (Exception ignored) {
//                            }
//
//                            try {
//                                params.put(vvkey, value);
//                            } catch (Exception ignored) {
//                            }
//                        }
//                    }
                } catch (Exception e) {
                    ILog.e(TAG, "track event:" + event + " failed!!!" + e.getMessage());
                    e.printStackTrace();
                    params = null;
                }
            }
            List<TrackPlatform> platforms = new ArrayList<>();
            if (platform == 5) {
//                platforms.add(TrackPlatform.FIREBASE);
//                platforms.add(TrackPlatform.APPSFLYER);
//                platforms.add(TrackPlatform.FACEBOOK);
//                platforms.add(TrackPlatform.THINKING_DATA);
                platforms = null;
            } else if (platform == 4) {
                platforms.add(TrackPlatform.THINKING_DATA);
            } else if (platform == 3) {
                platforms.add(TrackPlatform.APPSFLYER);
            } else if (platform == 2) {
                platforms.add(TrackPlatform.FACEBOOK);
            } else if (platform == 1) {
                platforms.add(TrackPlatform.FIREBASE);
            }
            IvySdk.getInstance().logEvent(event, EventType.EVENT_TYPE_COMMON, EventSrc.EVENT_SRC_CLIENT, params, platforms);
        });
    }

    public static void trackEventToAxon(String event, String data) {
        runUiThread(() -> {
            try {
                Map<String, Object> params = new HashMap<>();
                JSONObject obj = new JSONObject(data);
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String itemKey = keys.next();
                    Object itemValue = obj.get(itemKey);
                    if (itemValue instanceof Boolean) {
                        params.put(itemKey, ((Boolean) itemValue ? 1 : 0));
                    } else {
                        params.put(itemKey, itemValue);
                    }
                }
                IvySdk.getInstance().trackAxonEvent(event, params);
            } catch (Exception ignored) {
            }
        });
    }

    public static void setUserProperty(String key, String value, int platform) {
        runUiThread(() -> {
            if (platform == 6) {
                IvySdk.getInstance().setUserProperty(key, value, TrackPlatform.AXON);
            } else if (platform == 5) {
                IvySdk.getInstance().setUserProperty(key, value, null);
            } else if (platform == 4) {
                IvySdk.getInstance().setUserProperty(key, value, TrackPlatform.THINKING_DATA);
            } else if (platform == 3) {
                IvySdk.getInstance().setUserProperty(key, value, TrackPlatform.APPSFLYER);
            } else if (platform == 2) {
                IvySdk.getInstance().setUserProperty(key, value, TrackPlatform.FACEBOOK);
            } else if (platform == 1) {
                IvySdk.getInstance().setUserProperty(key, value, TrackPlatform.FIREBASE);
            }
        });
    }

    //统计

    // firebase cloud function
    public static void firebaseCloudFunctions(String functionName, JSONObject parameters, Callback success, Callback failure) {
        runUiThread(() -> {
            IvySdk.getInstance().firebaseCloudFunctions(functionName, parameters, (data) -> {
                if (success != null) {
                    success.onResponse(data);
                }
            }, (String reason) -> {
                if (failure != null) {
                    failure.onResponse(reason);
                }
            });
        });
    }

    // firebase cloud function

    //remote config
    public static int getRemoteConfigInt(String key) {
        return (int) IvySdk.getInstance().getRemoteConfigLong(key);
    }

    public static long getRemoteConfigLong(String key) {
        return IvySdk.getInstance().getRemoteConfigLong(key);
    }

    public static double getRemoteConfigDouble(String key) {
        return IvySdk.getInstance().getRemoteConfigDouble(key);
    }

    public static boolean getRemoteConfigBoolean(String key) {
        return IvySdk.getInstance().getRemoteConfigBoolean(key);
    }

    public static String getRemoteConfigString(String key) {
        return IvySdk.getInstance().getRemoteConfigString(key);
    }

    public static boolean hasIvyRemoteConfigKey(String key) {
        return IvySdk.getInstance().hasIvyRemoteConfigKey(key);
    }

    public static int getIvyRemoteConfigInt(String key) {
        return IvySdk.getInstance().getIvyRemoteConfigInt(key);
    }

    public static String getIvyRemoteConfigString(String key) {
        return IvySdk.getInstance().getIvyRemoteConfigString(key);
    }

    public static double getIvyRemoteConfigDouble(String key) {
        return IvySdk.getInstance().getIvyRemoteConfigDouble(key);
    }

    public static boolean getIvyRemoteConfigBoolean(String key) {
        return IvySdk.getInstance().getIvyRemoteConfigBoolean(key);
    }

    public static long getIvyRemoteConfigLong(String key) {
        return IvySdk.getInstance().getIvyRemoteConfigLong(key);
    }

    //remote config

    //game services
    public static boolean isAuthPlatformReady() {
        return IvySdk.getInstance().isAuthPlatformReady();
    }

    public static boolean hasPlayGamesSigned() {
        return IvySdk.getInstance().hasPlayGamesSigned();
    }

    public static void loginPlayGames() {
        runUiThread(() -> {
            IvySdk.getInstance().loginPlayGames();
        });
    }

    public static void logoutPlayGames() {
        runUiThread(() -> {
            IvySdk.getInstance().logoutPlayGames();
        });
    }

    public static String getPlayGamesUserInfo() {
        return IvySdk.getInstance().getPlayGamesUserInfo();
    }

    public static void unlockAchievement(String achievementId) {
        runUiThread(() -> {
            IvySdk.getInstance().unlockAchievement(achievementId);
        });
    }

    public static void increaseAchievement(String achievementId, int step) {
        runUiThread(() -> {
            IvySdk.getInstance().increaseAchievement(achievementId, step);
        });
    }

    public static void setAchievementStep(String achievementId, int step) {
        runUiThread(() -> {
            IvySdk.getInstance().setAchievementStep(achievementId, step);
        });
    }

    public static void showAchievement() {
        runUiThread(() -> {
            IvySdk.getInstance().showAchievement();
        });
    }

    public static void showLeaderboards() {
        runUiThread(() -> {
            IvySdk.getInstance().showLeaderboards();
        });
    }

    public static void showLeaderboard(String leaderboardId) {
        runUiThread(() -> {
            IvySdk.getInstance().showLeaderboard(leaderboardId);
        });
    }

    public static void updateLeaderboard(String leaderboardId, long score) {
        runUiThread(() -> {
            IvySdk.getInstance().updateLeaderboard(leaderboardId, score);
        });
    }

    public static void loadLeaderboardScore(String leaderboardId) {
        runUiThread(() -> {
            IvySdk.getInstance().loadLeaderboardScore(leaderboardId);
        });
    }

    public static void savePlayGamesArchive(String archiveName, String actionId, String payload) {
        runUiThread(() -> {
            IvySdk.getInstance().savePlayGamesArchive(archiveName, actionId, payload);
        });
    }

    public static void readPlayGamesArchive(String archiveName) {
        runUiThread(() -> {
            IvySdk.getInstance().readPlayGamesArchive(archiveName);
        });
    }

    public static void submitPlayGamesActivityEvent(String eventId, int increment) {
        runUiThread(() -> {
            IvySdk.getInstance().submitPlayGamesActivityEvent(eventId, increment);
        });
    }

    public static void loadPlayGamesActivityEventById(String eventId) {
        runUiThread(() -> {
            IvySdk.getInstance().loadPlayGamesActivityEventById(eventId);
        });
    }

    public static void loadPlayGamesActivityEvents() {
        runUiThread(() -> {
            IvySdk.getInstance().loadPlayGamesActivityEvents();
        });
    }

    public static void submitPlayGameEvent(String eventId, String data) {
        runUiThread(() -> {
            IvySdk.getInstance().submitPlayGameEvent(eventId, data);
        });
    }

    public static void loginFacebook() {
        runUiThread(() -> {
            IvySdk.getInstance().loginFacebook();
        });
    }

    public static void logoutFacebook() {
        runUiThread(() -> {
            IvySdk.getInstance().logoutFacebook();
        });
    }

    public static boolean hasFacebookSigned() {
        return IvySdk.getInstance().isFacebookLogged();
    }

    public static String getFacebookFriends() {
        return IvySdk.getInstance().getFacebookFriends();
    }

    public static String getFacebookUserInfo() {
        return IvySdk.getInstance().getFacebookUserInfo();
    }

    public static void loginGoogle() {
        runUiThread(() -> {
            IvySdk.getInstance().loginGoogle();
        });
    }

    public static void logoutGoogle() {
        runUiThread(() -> {
            IvySdk.getInstance().logoutGoogle();
        });
    }

    public static boolean isGoogleLogged() {
        return IvySdk.getInstance().isGoogleLogged();
    }

    public static String getGoogleUserInfo() {
        return IvySdk.getInstance().getGoogleUserInfo();
    }

    public static String getGoogleUserId() {
        return IvySdk.getInstance().getGoogleUserId();
    }

    public static void logoutFirebase() {
        runUiThread(() -> {
            IvySdk.getInstance().logoutFirebase();
        });
    }

    public static String getFirebaseUserInfo(String channel) {
        return IvySdk.getInstance().getFirebaseUserInfo(channel);
    }

    public static String getFirebaseUserId() {
        return IvySdk.getInstance().getFirebaseUserId();
    }

    public static boolean isFirebaseAnonymousSign() {
        return IvySdk.getInstance().isFirebaseAnonymousLogged();
    }

    public static boolean isFirebaseLinkedWithChannel(String channel) {
        return IvySdk.getInstance().isFirebaseLinkedWithChannel(channel);
    }

    public static boolean canFirebaseUnlinkWithChannel(String channel) {
        return IvySdk.getInstance().canFirebaseUnlinkWithChannel(channel);
    }

    public static void unlinkFirebaseWithChannel(String channel, IFirebaseUnlink callback) {
        runUiThread(() -> {
            IvySdk.getInstance().unlinkFirebaseWithChannel(channel, callback);
        });
    }

    public static void reloadFirebaseLastSign(IFirebaseAuthReload authReload) {
        runUiThread(() -> {
            IvySdk.getInstance().reloadFirebaseLastSign(authReload);
        });
    }

    public static void loginAnonymous(IAuthResponse authResult) {
        runUiThread(() -> {
            IvySdk.getInstance().loginAnonymous(authResult);
        });
    }

    public static void loginWithGoogle(IAuthResponse authResult) {
        runUiThread(() -> {
            IvySdk.getInstance().loginWithGoogle(authResult);
        });
    }

    public static void loginWithPlayGames(IAuthResponse authResult) {
        runUiThread(() -> {
            IvySdk.getInstance().loginWithPlayGames(authResult);
        });
    }

    public static void loginWithFacebook(IAuthResponse authResult) {
        runUiThread(() -> {
            IvySdk.getInstance().loginWithFacebook(authResult);
        });
    }

    public static void loginWithEmailAndPassword(String email, String password, IAuthResponse authResult) {
        runUiThread(() -> {
            IvySdk.getInstance().loginWithEmailAndPassword(email, password, authResult);
        });
    }

    //存档
    public static void setArchive(String collection, String documentId, String jsonData, IArchiveResult callback) {
        runUiThread(() -> {
            IvySdk.getInstance().setArchive(collection, documentId, jsonData, callback);
        });
    }

    public static void readArchive(String collection, String documentId, IArchiveResult callback) {
        runUiThread(() -> {
            IvySdk.getInstance().readArchive(collection, documentId, callback);
        });
    }

    public static void mergeArchive(String collection, String documentId, String jsonData, IArchiveResult callback) {
        runUiThread(() -> {
            IvySdk.getInstance().mergeArchive(collection, documentId, jsonData, callback);
        });
    }

    public static void queryArchive(String collection, String documentId, IArchiveResult callback) {
        runUiThread(() -> {
            IvySdk.getInstance().queryArchive(collection, documentId, callback);
        });
    }

    public static void deleteArchive(String collection, String documentId, IArchiveResult callback) {
        runUiThread(() -> {
            IvySdk.getInstance().deleteArchive(collection, documentId, callback);
        });
    }

    public static void updateArchive(String collection, String documentId, String jsonData, IArchiveResult callback) {
        runUiThread(() -> {
            IvySdk.getInstance().updateArchive(collection, documentId, jsonData, callback);
        });
    }

    public static void snapshotArchive(String collection, String documentId, IArchiveResult callback) {
        runUiThread(() -> {
            IvySdk.getInstance().snapshotArchive(collection, documentId, callback);
        });
    }
    //存档
    //game services

    //客服
    public static boolean isHelperInitialized() {
        return IvySdk.getInstance().isHelperInitialized();
    }

    public static boolean hasNewHelperMessage() {
        return IvySdk.getInstance().hasNewHelperMessage();
    }

    public static void showHelper(String entranceId, String meta, String tags, String welcomeMessag) {
        runUiThread(() -> {
            IvySdk.getInstance().showHelper(entranceId, meta, tags, welcomeMessag);
        });
    }

    public static void showHelperSingleFAQ(String faqId, int moment) {
        runUiThread(() -> {
            IvySdk.getInstance().showHelperSingleFAQ(faqId, moment);
        });
    }

    public static void listenHelperUnreadMsgCount(boolean onlyOnce) {
        runUiThread(() -> {
            IvySdk.getInstance().listenHelperUnreadMessageCount(onlyOnce);
        });
    }

    public static void stopListenHelperUnreadMsgCount() {
        runUiThread(() -> {
            IvySdk.getInstance().stopListenHelperUnreadMessageCount();
        });
    }

    public static void updateHelperUserInfo(String data, String tags) {
        runUiThread(() -> {
            IvySdk.getInstance().updateHelperUserInfo(data, tags);
        });
    }

    public static void resetHelperUserInfo() {
        runUiThread(() -> {
            IvySdk.getInstance().resetHelperUserInfo();
        });
    }

    public static void updateHelperLanguage(String l) {
        runUiThread(() -> {
            IvySdk.getInstance().updateHelperLanguage(l);
        });
    }

    public static void closeHelper() {
        runUiThread(() -> {
            IvySdk.getInstance().closeHelper();
        });
    }

    //客服

    //其它
    public static String getConfig(int configKey) {
        String result = Helper.getConfig(configKey);
        if (result == null) return "";
        return result;
    }

    public static boolean isNetworkConnected() {
        return Util.isNetworkConnected();
    }

    public static void rateUs() {
        runUiThread(() -> {
            AppStoreUtil.rateUs(5, false);
        });
    }

    public static void openAppStore(String url, String referrer) {
        runUiThread(() -> {
            AppStoreUtil.openAppStore(url, referrer);
        });
    }

    //通知
    public static int loadNotificationPermissionState() {
        return IvySdk.getInstance().loadNotificationPermissionState();
    }

    public static void requestNotificationPermission() {
        runUiThread(() -> {
            IvySdk.getInstance().requestNotificationPermission();
        });
    }

    public static void openNotificationSettings() {
        runUiThread(() -> {
            IvySdk.getInstance().openNotificationSettings();
        });
    }

    public static void pushNotificationTask(String tag, String title, String subtitle, String bigText, String smallIcon, String largeIcon, String bigPicture, long pushTime, long interval, boolean autoCancel, String action, boolean repeat) {
        if (TextUtils.isEmpty(tag)) {
            ILog.e(TAG, "invalid notification tag! cannot be null");
            return;
        }
        if (TextUtils.isEmpty(title)) {
            ILog.e(TAG, "invalid notification title! cannot be null");
            return;
        }
        runUiThread(() -> {
            IvySdk.getInstance().pushNotificationTask(tag, title, subtitle, bigText, smallIcon, largeIcon, bigPicture, pushTime, interval, autoCancel, action, repeat);
        });
    }

    public static void cancelNotification(String tag) {
        runUiThread(() -> {
            IvySdk.getInstance().cancelNotification(tag);
        });
    }

    public static void addBootNotification(String tag, String title, String subtitle, String bigText, String smallIcon, String largeIcon, String bigPicture, String action) {
        runUiThread(() -> {
            IvySdk.getInstance().addBootNotification(tag, title, subtitle, bigText, smallIcon, largeIcon, bigPicture, action);
        });
    }

    public static void removeBootNotification(String tag) {
        runUiThread(() -> {
            IvySdk.getInstance().removeBootNotification(tag);
        });
    }

    //通知
    public static void appsflyerInviteUser(String inviterId, String inviterAppId) {
        runUiThread(() -> {
            IvySdk.getInstance().appsflyerInviteUser("android_user_invite", "user_invite", inviterId, inviterAppId);
        });
    }

    public static String getAppsflyerInviterId() {
        return IvySdk.getInstance().getAppsflyerInviterId();
    }

    //其它
    public static String encodeData(String data) {
        try {
            return Util.encodeParams(App.getInstance(), data);
        } catch (Exception e) {
            ILog.e("AndroidSDK", "encode failed:" + e.getMessage());
        }
        return null;
    }

    public static String decodeData(String data) {
        try {
            return Util.decodeParams(App.getInstance(), data);
        } catch (Exception e) {
            ILog.e("AndroidSDK", "decode failed:" + e.getMessage());
        }
        return null;
    }

    public static void copyTxt(String txt) {
        runUiThread(() -> {
            if (txt != null) {
                boolean result = Util.copyTxt(txt);
                ILog.i(TAG, "copy txt result:" + result);
//                return result;
            } else {
                ILog.i(TAG, "copy txt failed");
//                return false;
            }
        });
    }

    public static int getTotalMemory() {
        return Util.byte2MB(Util.totalMemory());
    }

    public static int getFreeMemory() {
        return Util.byte2MB(Util.availableMemory());
    }

    public static int getDiskSize() {
        return Util.byte2MB(Util.totalDiskSize());
    }

    public static int getFreeDiskSize() {
        return Util.byte2MB(Util.availableDiskSize());
    }

    public static void sendEmail(String email, String title, String extra) {
        runUiThread(() -> {
            try {
                Activity activity = ActivityUtil.getInstance().getActivity();
                if (activity != null) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:" + email));
                    if (title != null) {
                        intent.putExtra(Intent.EXTRA_SUBJECT, title);
                    }
                    if (extra != null) {
                        intent.putExtra(Intent.EXTRA_TEXT, extra);
                    }
                    activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else {
                    ILog.e(TAG, "send email failed: invalid activity");
                }
            } catch (Exception e) {
                ILog.e(TAG, "send email failed: " + e.getMessage());
            }
        });
    }

    public static void systemShareText(String txt) {
        runUiThread(() -> {
            if (txt == null || txt.isEmpty()) {
                ILog.e(TAG, "share msg can not be null");
                return;
            }
            Activity activity = ActivityUtil.getInstance().getActivity();
            if (activity != null) {
                IvyUtil.systemShareText(activity, txt);
            }
        });
    }

    public static void systemShareImage(String title, String imagePath) {
        runUiThread(() -> {
            if (imagePath == null || imagePath.isEmpty()) {
                ILog.e(TAG, "share image path can not be null");
                return;
            }
            String shareTitle = "Share";
            if (title != null && !title.isEmpty()) {
                shareTitle = title;
            }
            Activity activity = ActivityUtil.getInstance().getActivity();
            if (activity != null) {
                IvyUtil.systemShareImage(shareTitle, imagePath);
            }
        });
    }

    public static void openUrl(String url) {
        runUiThread(() -> {
            IvyUtil.openUrlWithBrowser(url);
        });
    }

    public static void showWebView(String title, String url) {
        runUiThread(() -> {
            IvyUtil.showWebView(title, url);
        });
    }

    public static boolean hasNotch() {
        return IvySdk.getInstance().hasNotch();
    }

    public static void displayInNotch() {
        runUiThread(() -> {
            IvySdk.getInstance().displayInNotch();
        });
    }

    public static int getNotchHeight() {
        return IvySdk.getInstance().getNotchHeight();
    }

    public static boolean hasGestureBar() {
        return IvySdk.getInstance().hasGestureBar();
    }

    public static int getGestureBarHeight() {
        return IvySdk.getInstance().getGestureBarHeight();
    }

    public static void toast(String msg) {
        if (msg != null) {
            runUiThread(() -> {
                IToast.toastAnyway(msg);
            });
        }
    }

    public static long getAppInstallTimestamp() {
        try {
            return Util.installTime();
        } catch (Exception ignored) {

        }
        return System.currentTimeMillis();
    }

    public static void openFacebookPage(String pageId) {
        runUiThread(() -> {
            Util.openFacebookPage(pageId);
        });
    }

    public static void forceQuit() {
        Process.killProcess(Process.myPid());
    }

    public static boolean isDebug() {
        return IvySdk.getInstance().isDebug();
    }

    public static void goLauncher() {
        runUiThread(new Runnable() {
            @Override
            public void run() {
                Util.goLauncher();
            }
        });
    }

    public static boolean isNightMode() {
        return Util.isNightMode();
    }

    public static void addShortcut(String id, String title, String label, String icon) {
        runUiThread(() -> {
            ShortcutUtil.addShortcut(id, title, label, icon);
        });
    }

    public static void deleteShortcut(String id) {
        runUiThread(() -> {
            ShortcutUtil.deleteShortcut(id);
        });
    }

    public static String getShortcutAction() {
        return ShortcutUtil.getShortcutAction();
    }

    public static String getNotificationAction() {
        return IvySdk.getInstance().getLastNotificationAction();
    }

    /**
     * @param duration  震动时长，单位毫秒
     * @param amplitude 震动强度， 1 ~ 255
     */
    public static void vibrate(long duration, int amplitude) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (amplitude > 0 && duration > 0) {
                VibrationUtil.getInstance().vibrateOnce(App.getInstance(), duration, amplitude);
            }
        }
    }

    /**
     * @param stepDuration 每个震动点的震动时长， 逗号分隔的字符串； 例如 1，2，3
     * @param curve        每个震动点的震动强度，范围在 1~255； 逗号分隔的字符串，例如 1，2，3
     * @implNote stepDuration 和 curve 的长度必须一致
     */
    public static void vibrateWithCure(String stepDuration, String curve) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String[] parts_step = stepDuration.split(",");
            long[] durations = new long[parts_step.length];

            for (int i = 0; i < parts_step.length; i++) {
                durations[i] = Long.parseLong(parts_step[i].trim());
            }

            String[] parts_cure = curve.split(",");
            int[] curves = new int[parts_cure.length];

            for (int i = 0; i < parts_cure.length; i++) {
                curves[i] = Integer.parseInt(parts_cure[i].trim());
            }
            if (durations.length != curves.length) {
                return;
            }
            VibrationUtil.getInstance().vibrateWaveform(App.getInstance(), durations, curves, -1);
        }
    }

    public static void cancelVibrate() {
        VibrationUtil.getInstance().cancel();
    }

    public static void getAdId(AdvertisingIdManager.Callback callback) {
        try {
            AdvertisingIdManager.get(App.getInstance(), callback);
        } catch (Exception ignored) {
            if (callback != null) callback.onResult(Util.roleId());
        }
    }

    public static void uploadTrajectory(int levelId, String userTier, String data) {
        if (levelId < 0) return;
        if (TextUtils.isEmpty(data)) return;
        TrajectoryManager.getInstance().uploadTrajectory(levelId, userTier, data);
    }

    public static void translate(String from_lang, String dst_lang, String data, TranslateUtil.Action action) {
        TranslateUtil.translate(from_lang, dst_lang, data, action);
    }

    private static void runUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    public static void enterVariants(String varid) {
        setUserProperty("ab_test_user", "true", 5);
        LocalAB.activateVariant(App.getInstance(), varid);
    }

    public static String getABHitResult() {
        return IvyABManager.getInstance().getAbHitResults();
    }
}
