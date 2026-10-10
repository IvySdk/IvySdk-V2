package com.ivy.games;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.ivy.base.Callback;
import com.ivy.base.ILog;
import com.ivy.base.games.IGame;
import com.ivy.base.games.IPlayGameEvents;
import com.ivy.base.games.archive.IArchiveResult;
import com.ivy.base.games.archive.IIArchive;
import com.ivy.base.games.auth.AuthPlatforms;
import com.ivy.base.games.auth.FirebaseLinkChannel;
import com.ivy.base.games.auth.IAuthResponse;
import com.ivy.base.games.auth.IAuthResult;
import com.ivy.base.games.auth.IFacebookAuth;
import com.ivy.base.games.auth.IFirebaseAuth;
import com.ivy.base.games.auth.IFirebaseAuthReload;
import com.ivy.base.games.auth.IFirebaseUnlink;
import com.ivy.base.games.auth.IGoogleAuth;
import com.ivy.base.games.auth.IPlayGameAuth;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IvyGames implements IGame {

    public static final String TAG = "games";

    private IFacebookAuth facebookAuth;
    private IPlayGameAuth playGamesAuth;
    private IFirebaseAuth firebaseAuth;
    private IGoogleAuth googleAuth;

    private IIArchive archiveImpl;

    private IAuthResult authResult;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Handler mainHandler = null;

    private IPlayGameEvents iPlayGameEvents;

    public void setup(final String appId, String data, final IAuthResult authResult, final IPlayGameEvents playGamesListener, final boolean debug) {
        this.authResult = authResult;
        this.iPlayGameEvents = playGamesListener;
        executor.submit(() -> {
            try {
                JSONObject json = new JSONObject(data);
                if (json.has("playGames")) {
                    JSONObject config = json.optJSONObject("playGames");
                    if (config != null) {
                        try {
                            Class<?> clazz = Class.forName("com.ivy.playgames.PlayGamesAuth");
                            Constructor<?> constructor = clazz.getDeclaredConstructor();
                            playGamesAuth = (IPlayGameAuth) constructor.newInstance();
                            if (playGamesAuth == null) {
                                throw new Exception("unable create play game impl");
                            }
                            runOnMainThread(() -> {
                                if (playGamesAuth.isServiceAvailable()) {
                                    playGamesAuth.setup(appId, config, debug, authResult);
                                    playGamesAuth.setListener(playGamesListener);
                                } else {
                                    playGamesAuth = null;
                                    ILog.e(TAG, "create play-game impl failed: api not available");
                                }
                            });
                        } catch (Exception e) {
                            ILog.e(TAG, "create play-game impl failed:" + e.getMessage());
                        }
                    }
                }

                if (json.has("google+")) {
                    JSONObject config = json.optJSONObject("google+");
                    if (config != null) {
                        try {
                            Class<?> clazz = Class.forName("com.ivy.playgames.GoogleAuth");
                            Constructor<?> constructor = clazz.getDeclaredConstructor();
                            googleAuth = (IGoogleAuth) constructor.newInstance();
                            if (googleAuth == null) {
                                throw new Exception("unable create google+ impl");
                            }
                            runOnMainThread(() -> googleAuth.setup(appId, config, debug, authResult));
                        } catch (Exception e) {
                            ILog.e(TAG, "create google+ impl failed:" + e.getMessage());
                        }
                    }
                }

                if (json.has("firebase")) {
                    JSONObject config = json.optJSONObject("firebase");
                    if (config != null) {
                        try {
                            Class<?> clazz = Class.forName("com.ivy.firebase.FBAuth");
                            Constructor<?> constructor = clazz.getDeclaredConstructor();
                            firebaseAuth = (IFirebaseAuth) constructor.newInstance();
                            if (firebaseAuth == null) {
                                throw new Exception("unable create firebase impl");
                            }
                            runOnMainThread(() -> firebaseAuth.setup(config, debug, () -> playGamesAuth != null ? playGamesAuth.requireServerSideAuthCode() : null, () -> facebookAuth != null ? facebookAuth.requireAccessToken() : null, () -> googleAuth != null ? googleAuth.getIdToken() : null));
                        } catch (Exception e) {
                            ILog.e(TAG, "create firebase impl failed:" + e.getMessage());
                        }
                    }
                }

                if (json.has("facebook")) {
                    JSONObject config = json.optJSONObject("facebook");
                    if (config != null) {
                        try {
                            Class<?> clazz = Class.forName("com.ivy.facebook.FacebookAuth");
                            Constructor<?> constructor = clazz.getDeclaredConstructor();
                            facebookAuth = (IFacebookAuth) constructor.newInstance();
                            if (facebookAuth == null) {
                                throw new Exception("unable create facebook impl");
                            }
                            runOnMainThread(() -> facebookAuth.setup(appId, config, debug, authResult));
                        } catch (Exception e) {
                            ILog.e(TAG, "create facebook impl failed:" + e.getMessage());
                        }
                    }
                }

                if (json.has("firestore")) {
                    JSONObject config = json.optJSONObject("firestore");
                    if (config != null) {
                        try {
                            Class<?> clazz = Class.forName("com.ivy.firebase.FirestoreImpl");
                            Constructor<?> constructor = clazz.getDeclaredConstructor();
                            archiveImpl = (IIArchive) constructor.newInstance();
                            if (archiveImpl == null) {
                                throw new Exception("unable create firestore impl");
                            }
                            runOnMainThread(() -> archiveImpl.setup(config, debug));
                        } catch (Exception e) {
                            ILog.e(TAG, "create firestore impl failed:" + e.getMessage());
                        }
                    }
                }

            } catch (Exception e) {
                ILog.e(TAG, "parse games config err:" + e.getMessage());
            } finally {
                ILog.i(TAG, "platforms initialized");
                runOnMainThread(() -> authResult.onPlatformsInitialized());
            }
        });
    }

    private void runOnMainThread(Runnable runnable) {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        mainHandler.post(runnable);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (facebookAuth != null) {
            facebookAuth.onActivityResult(requestCode, resultCode, data);
        }
        if (playGamesAuth != null) {
            playGamesAuth.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean isPlayGamesLogged() {
        if (playGamesAuth != null) return playGamesAuth.getLoginStatus();
        ILog.w(TAG, "play-games auth invalid to use");
        return false;
    }

    @Override
    public void loginPlayGames() {
        if (playGamesAuth != null) {
            TestManager.getInstance().addEvent("PlayGames_Auth", "请求登录PlayGames", EventState.NORMAL, "");
            playGamesAuth.login(new IAuthResponse() {
                @Override
                public void onLoginResult(String platform, boolean status, String channel, String reason) {
                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, status, channel, reason);
                }
            });
        } else {
            TestManager.getInstance().addEvent("PlayGames_Auth", "请求登录PlayGames,服务未初始化，请检查配置", EventState.ERROR, "");
            ILog.w(TAG, "play-games not initialized");
            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_PLAY_GAMES, false, null, "not_initialized");
        }
    }

    @Override
    public void logoutPlayGames() {
        if (playGamesAuth != null) playGamesAuth.logout();
        else ILog.w(TAG, "play-games auth invalid to use");
    }

    @Override
    public String getPlayGamesUserInfo() {
        if (playGamesAuth != null) return playGamesAuth.getUserInfo();
        ILog.w(TAG, "play-games auth invalid to use");
        return "{}";
    }

    @Override
    public String getPlayGamesUserId() {
        return playGamesAuth != null ? playGamesAuth.getUserId() : "";
    }

    @Override
    public void unlockAchievement(String achievementId) {
        if (playGamesAuth != null) playGamesAuth.unlockAchievement(achievementId);
        else {
            ILog.w(TAG, "play-games auth invalid to use");
            TestManager.getInstance().addEvent("PlayGames", "解锁成就失败，PlayGames未配置", EventState.ERROR, "achievementId=" + achievementId);
        }
    }

    @Override
    public void increaseAchievement(String achievementId, int step) {
        if (playGamesAuth != null) playGamesAuth.increaseAchievement(achievementId, step);
        else {
            ILog.w(TAG, "play-games auth invalid to use");
            TestManager.getInstance().addEvent("PlayGames", "更新成就进度失败，PlayGames未配置", EventState.ERROR, "achievementId=" + achievementId + ";step=" + step);
        }
    }

    @Override
    public void setAchievementStep(String achievementId, int step) {
        if (playGamesAuth != null) playGamesAuth.setAchievementStep(achievementId, step);
        else {
            ILog.w(TAG, "play-games auth invalid to use");
            TestManager.getInstance().addEvent("PlayGames", "更新成就step失败，PlayGames未配置", EventState.ERROR, "achievementId=" + achievementId + ";step=" + step);
        }
    }

    @Override
    public void showAchievement() {
        if (playGamesAuth != null) playGamesAuth.showAchievement();
        else ILog.w(TAG, "play-games auth invalid to use");
    }

    @Override
    public void showLeaderboards() {
        if (playGamesAuth != null) playGamesAuth.showLeaderboards();
        else ILog.w(TAG, "play-games auth invalid to use");
    }

    @Override
    public void showLeaderboard(String leaderboardId) {
        if (playGamesAuth != null) playGamesAuth.showLeaderboard(leaderboardId);
        else ILog.w(TAG, "play-games auth invalid to use");
    }

    @Override
    public void updateLeaderboard(String leaderboardId, long score) {
        if (playGamesAuth != null) playGamesAuth.updateLeaderboard(leaderboardId, score);
        else ILog.w(TAG, "play-games auth invalid to use");
    }

    @Override
    public void loadLeaderboardScore(String leaderboardId) {
        if (playGamesAuth != null) playGamesAuth.loadLeaderboardScore(leaderboardId);
        else ILog.w(TAG, "play-games auth invalid to use");
    }

    @Override
    public void saveArchive(String archiveName, String actionId, String payload) {
        if (playGamesAuth != null) playGamesAuth.save(archiveName, actionId, payload);
        else {
            ILog.w(TAG, "play-games auth invalid to use");
            if (iPlayGameEvents != null) {
                iPlayGameEvents.onWriteArchive(archiveName, actionId, false);
            }
        }
    }

    @Override
    public void readArchive(String archiveName) {
        if (playGamesAuth != null) playGamesAuth.read(archiveName);
        else {
            ILog.w(TAG, "play-games auth invalid to use");
            if (iPlayGameEvents != null) {
                iPlayGameEvents.onReadArchive(archiveName, "");
            }
        }
    }

    @Override
    public void submitActivityEvent(String eventId, int increment) {
        if (playGamesAuth != null) playGamesAuth.submitEvent(eventId, increment);
        else ILog.w(TAG, "play-games auth invalid to use");
    }

    @Override
    public void loadActivityEventById(String eventId) {
        if (playGamesAuth != null) playGamesAuth.loadEvent(eventId);
        else {
            ILog.w(TAG, "play-games auth invalid to use");
            if (iPlayGameEvents != null) {
                iPlayGameEvents.onLoadEvents("[]");
            }
        }
    }

    @Override
    public void loadActivityEvents() {
        if (playGamesAuth != null) playGamesAuth.loadEvents();
        else {
            ILog.w(TAG, "play-games auth invalid to use");
            if (iPlayGameEvents != null) {
                iPlayGameEvents.onLoadEvents("[]");
            }
        }
    }

    @Override
    public void submitPlayGameEvent(String eventId, JSONObject property) {
        if (playGamesAuth != null) {
            playGamesAuth.submitGameEvent(eventId, property);
        } else {
            TestManager.getInstance().addEvent("PlayGames", "上传数据失败", EventState.ERROR, "PlayGames未初始化");
            ILog.w(TAG, "play-games auth invalid to use");
        }
    }

    // Facebook 登录相关
    @Override
    public void loginFacebook() {
        if (facebookAuth != null) {
            TestManager.getInstance().addEvent("Facebook_Auth", "开始登录Facebook", EventState.NORMAL, "");
            facebookAuth.login(new IAuthResponse() {
                @Override
                public void onLoginResult(String platform, boolean status, String channel, String reason) {
                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, status, channel, reason);
                }
            });
        } else {
            TestManager.getInstance().addEvent("Facebook_Auth", "请求登录Facebook,平台未初始化，请检查配置", EventState.ERROR, "");
            ILog.w(TAG, "facebook auth not initialized");
            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "not_initialized");
        }
    }

    @Override
    public void logoutFacebook() {
        if (facebookAuth != null) facebookAuth.logout();
        else ILog.w(TAG, "facebook auth invalid to use");
    }

    @Override
    public boolean isFacebookLogged() {
        if (facebookAuth != null) return facebookAuth.getLoginStatus();
        ILog.w(TAG, "facebook auth invalid to use");
        return false;
    }

    @Override
    public String getFacebookFriends() {
        if (facebookAuth != null) return facebookAuth.getFriends();
        ILog.w(TAG, "facebook auth invalid to use");
        return "[]";
    }

    @Override
    public String getFacebookUserInfo() {
        if (facebookAuth != null) return facebookAuth.getUserInfo();
        ILog.w(TAG, "facebook auth invalid to use");
        return "{}";
    }

    @Override
    public String getFacebookUserId() {
        return facebookAuth != null ? facebookAuth.getUserId() : "";
    }

    // Google 登录相关
    @Override
    public void loginGoogle() {
        if (googleAuth != null) {
            TestManager.getInstance().addEvent("Google_Auth", "开始登录Google", EventState.NORMAL, "");
            googleAuth.login(new IAuthResponse() {
                @Override
                public void onLoginResult(String platform, boolean status, String channel, String reason) {
                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, status, channel, reason);
                }
            });
        } else {
            TestManager.getInstance().addEvent("Google_Auth", "请求登录Google,平台未初始化，请检查配置", EventState.ERROR, "");
            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, "not_initialized");
        }
    }

    @Override
    public void logoutGoogle() {
        if (googleAuth != null) googleAuth.logout();
        else ILog.w(TAG, "logout google failed: not initialized");
    }

    @Override
    public boolean isGoogleLogged() {
        if (googleAuth != null) return googleAuth.getLoginStatus();
        ILog.w(TAG, "get google login status failed: not initialized");
        return false;
    }

    public boolean isGoogleTokenValid() {
        if (googleAuth != null) return googleAuth.isTokenValid();
        ILog.w(TAG, "get google login status failed: not initialized");
        return false;
    }

    @Override
    public String getGoogleUserInfo() {
        if (googleAuth != null) return googleAuth.getUserInfo();
        ILog.w(TAG, "get google user info failed: not initialized");
        return "{}";
    }

    @Override
    public String getGoogleUserId() {
        return googleAuth != null ? googleAuth.getUserId() : "";
    }

    // Firebase 相关
    @Override
    public void logoutFirebase() {
        if (firebaseAuth != null) firebaseAuth.logout();
        else ILog.w(TAG, "firebase auth invalid to use");
    }

    @Override
    public String getFirebaseUserInfo(String channel) {
        if (firebaseAuth != null) return firebaseAuth.getUserInfo(channel);
        ILog.w(TAG, "firebase auth invalid to use");
        return "{}";
    }

    @Override
    public String getFirebaseUserId() {
        return firebaseAuth != null ? firebaseAuth.getUserId() : "";
    }

    @Override
    public boolean isFirebaseAnonymousLogged() {
        if (firebaseAuth != null) return firebaseAuth.isAnonymous();
        ILog.w(TAG, "firebase auth invalid to use");
        return true;
    }

    @Override
    public boolean isFirebaseLinkedWithChannel(String channel) {
        if (firebaseAuth != null) return firebaseAuth.isChannelLinked(channel);
        ILog.w(TAG, "firebase auth invalid to use");
        return false;
    }

    @Override
    public boolean canFirebaseUnlinkWithChannel(String channel) {
        if (firebaseAuth != null) return firebaseAuth.canChannelUnlink(channel);
        ILog.w(TAG, "firebase auth invalid to use");
        return false;
    }

    @Override
    public void unlinkFirebaseWithChannel(String channel, IFirebaseUnlink callback) {
        if (firebaseAuth != null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "开始解绑" + channel, EventState.NORMAL, "");
            firebaseAuth.unlinkChannel(channel, callback);
        } else {
            TestManager.getInstance().addEvent("Firebase_Auth", "解绑" + channel + "失败，Firebase服务未初始化", EventState.ERROR, "");
            ILog.w(TAG, "firebase auth invalid to use");
            if (callback != null) callback.onUnlinked(channel, false, "not_initialized");
        }
    }

    @Override
    public void reloadFirebaseLastSign(IFirebaseAuthReload authReload) {
        if (firebaseAuth != null) firebaseAuth.reloadLastLoginStatus(authReload);
        else {
            ILog.w(TAG, "firebase auth invalid to use");
            authReload.onReload(false, "not_initialized");
        }
    }

    @Override
    public void loginAnonymous(IAuthResponse authResult) {
        if (firebaseAuth != null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "开始匿名登录", EventState.NORMAL, "");
            firebaseAuth.loginAnonymous(authResult);
        } else {
            TestManager.getInstance().addEvent("Firebase_Auth", "匿名登录失败,服务未初始化，请检查配置", EventState.ERROR, "");
            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.ANONYMOUS, "not_initialized");
        }
    }

    @Override
    public void loginWithGoogle(IAuthResponse authResult) {
        if (isGoogleLogged() && isGoogleTokenValid()) {
            if (firebaseAuth != null) {
                TestManager.getInstance().addEvent("Firebase_Auth", "Google已登陆，开始绑定Google渠道", EventState.NORMAL, "");
                firebaseAuth.loginWithGoogle(authResult, true);
            } else {
                ILog.e(TAG, "login with google failed:not_initialized");
                TestManager.getInstance().addEvent("Firebase_Auth", "Google已登录，Firebase未初始化，请检查配置", EventState.ERROR, "");
                if (authResult != null)
                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, "not_initialized");
            }
        } else {
            if (googleAuth != null) {
                TestManager.getInstance().addEvent("Firebase_Auth", "未登录Google，开始登录Google", EventState.NORMAL, "");
                googleAuth.logout(new Callback() {
                    @Override
                    public void onResult() {
                        googleAuth.login(new IAuthResponse() {
                            @Override
                            public void onLoginResult(String platform, boolean status, String channel, String reason) {
                                if (status) {
                                    if (firebaseAuth != null) {
                                        TestManager.getInstance().addEvent("Firebase_Auth", "Google登陆成功，开始绑定Google渠道", EventState.NORMAL, "");
                                        firebaseAuth.loginWithGoogle(authResult, true);
                                    } else {
                                        ILog.e(TAG, "login with google failed:firebase not_initialized");
                                        TestManager.getInstance().addEvent("Firebase_Auth", "Google登录成功，但Firebase未初始化，结束绑定，请检查配置", EventState.ERROR, "");
                                        if (authResult != null)
                                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, "fb_not_initialized");
                                    }
                                } else {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "Google登录失败，结束绑定", EventState.ERROR, "" + reason);
                                    ILog.e(TAG, "login with google failed:" + reason);
                                    if (authResult != null)
                                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, "google_login_failed");
                                }
                            }
                        });
                    }
                });
            } else {
                ILog.e(TAG, "login with google failed:not_initialized");
                TestManager.getInstance().addEvent("Firebase_Auth", "Google登录失败，服务未初始化，请检查配置", EventState.ERROR, "");
                if (authResult != null)
                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, "not_initialized");
            }
        }
    }

    @Override
    public void loginWithPlayGames(IAuthResponse authResult) {
        if (isPlayGamesLogged()) {
            TestManager.getInstance().addEvent("Firebase_Auth", "PlayGames已登陆，开始刷新auth code", EventState.NORMAL, "");
            playGamesAuth.refreshAuthCode(result -> {
                if (result) {
                    if (firebaseAuth != null) {
                        TestManager.getInstance().addEvent("Firebase_Auth", "auth code成功，开始PlayGames绑定", EventState.NORMAL, "");
                        firebaseAuth.loginWithPlayGames(authResult, true);
                    } else {
                        TestManager.getInstance().addEvent("Firebase_Auth", "auth code刷新成功，但Firebase未初始化，请检查配置", EventState.ERROR, "");
                        ILog.e(TAG, "login with play games failed:not_initialized");
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, "not_initialized");
                    }
                } else {
                    TestManager.getInstance().addEvent("Firebase_Auth", "auth code刷新失败，结束绑定", EventState.ERROR, "");
                    ILog.e(TAG, "login with play games failed:refresh_auth_code");
                    if (authResult != null)
                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, "not_initialized");
                }
            });
        } else {
            if (playGamesAuth != null) {
                playGamesAuth.login(new IAuthResponse() {
                    @Override
                    public void onLoginResult(String platform, boolean status, String channel, String reason) {
                        if (status) {
                            if (firebaseAuth != null) {
                                TestManager.getInstance().addEvent("Firebase_Auth", "PlayGames登陆成功，开始绑定PlayGames渠道", EventState.NORMAL, "");
                                firebaseAuth.loginWithPlayGames(authResult, true);
                            } else {
                                ILog.e(TAG, "login with play games failed: firebase not_initialized");
                                TestManager.getInstance().addEvent("Firebase_Auth", "PlayGames登录成功，但Firebase服务未初始化，请检查配置", EventState.ERROR, "");
                                if (authResult != null)
                                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, "not_initialized");
                            }
                        } else {
                            TestManager.getInstance().addEvent("Firebase_Auth", "PlayGames登录失败，结束绑定", EventState.ERROR, "" + reason);
                            ILog.e(TAG, "login with playgames failed:not_initialized");
                            if (authResult != null)
                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, "fb_not_initialized");
                        }
                    }
                });
            } else {
                ILog.e(TAG, "login with play games failed:not_initialized");
                TestManager.getInstance().addEvent("Firebase_Auth", "PlayGames登录失败，服务未初始化，请检查配置", EventState.ERROR, "");
                if (authResult != null)
                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, "not_initialized");
            }
        }
    }

    @Override
    public void loginWithFacebook(IAuthResponse authResult) {
        if (isFacebookLogged()) {
            if (firebaseAuth != null) {
                TestManager.getInstance().addEvent("Firebase_Auth", "Facebook已登陆，开始绑定Facebook渠道登录", EventState.NORMAL, "");
                firebaseAuth.loginWithFacebook(authResult, true);
            } else {
                TestManager.getInstance().addEvent("Firebase_Auth", "Facebook已登录，Firebase未初始化，请检查配置", EventState.ERROR, "");
                ILog.e(TAG, "login with facebook failed:not_initialized");
                if (authResult != null)
                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, "not_initialized");
            }
        } else {
            if (facebookAuth != null) {
                TestManager.getInstance().addEvent("Firebase_Auth", "开始登录facebook", EventState.NORMAL, "");
                facebookAuth.login(new IAuthResponse() {
                    @Override
                    public void onLoginResult(String platform, boolean status, String channel, String reason) {
                        if (status) {
                            if (firebaseAuth != null) {
                                TestManager.getInstance().addEvent("Firebase_Auth", "Facebook登陆成功，开始绑定Facebook渠道", EventState.NORMAL, "");
                                firebaseAuth.loginWithFacebook(authResult, true);
                            } else {
                                TestManager.getInstance().addEvent("Firebase_Auth", "Facebook登录成功，但Firebase未初始化，结束绑定，请检查配置", EventState.ERROR, "");
                                ILog.e(TAG, "login with facebook failed:not_initialized");
                                if (authResult != null)
                                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, "not_initialized");
                            }
                        } else {
                            TestManager.getInstance().addEvent("Firebase_Auth", "Facebook登录失败,结束绑定", EventState.ERROR, "" + reason);
                            ILog.e(TAG, "login with facebook failed:not_initialized");
                            if (authResult != null)
                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, "fb_not_initialized");
                        }
                    }
                });
            } else {
                TestManager.getInstance().addEvent("Firebase_Auth", "Facebook登录失败，服务未初始化，请检查配置", EventState.ERROR, "");
                ILog.e(TAG, "login with facebook failed:not_initialized");
                if (authResult != null)
                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, "not_initialized");
            }
        }
    }

    @Override
    public void loginWithEmailAndPassword(String email, String password, IAuthResponse authResult) {
        if (firebaseAuth != null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "开始Email渠道登录", EventState.NORMAL, "");
            firebaseAuth.loginWithEmailAndPassword(email, password, new IAuthResponse() {
                @Override
                public void onLoginResult(String platform, boolean status, String channel, String reason) {
                    if (authResult != null)
                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, status, channel, reason);
                }
            }, true);
        } else {
            ILog.e(TAG, "login with facebook failed:not_initialized");
            TestManager.getInstance().addEvent("Firebase_Auth", "Email登录失败，服务未初始化，请检查配置", EventState.ERROR, "");
            if (authResult != null)
                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, "not_initialized");
        }
    }

    // Firestore 操作
    @Override
    public void set(String collection, String documentId, String jsonData, IArchiveResult callback) {
        if (archiveImpl != null) archiveImpl.set(collection, documentId, jsonData, callback);
        else {
            ILog.w(TAG, "archive invalid to use");
            callback.onFailure(collection, documentId, "not_initialized");
        }
    }

    @Override
    public void read(String collection, String documentId, IArchiveResult callback) {
        if (archiveImpl != null) archiveImpl.read(collection, documentId, callback);
        else {
            ILog.w(TAG, "archive invalid to use");
            callback.onFailure(collection, documentId, "not_initialized");
        }
    }

    @Override
    public void merge(String collection, String documentId, String jsonData, IArchiveResult callback) {
        if (archiveImpl != null) archiveImpl.merge(collection, documentId, jsonData, callback);
        else {
            ILog.w(TAG, "archive invalid to use");
            callback.onFailure(collection, documentId, "not_initialized");
        }
    }

    @Override
    public void query(String collection, String documentId, IArchiveResult callback) {
        if (archiveImpl != null) archiveImpl.query(collection, documentId, callback);
        else {
            ILog.w(TAG, "archive invalid to use");
            callback.onFailure(collection, null, "not_initialized");
        }
    }

    @Override
    public void delete(String collection, String documentId, IArchiveResult callback) {
        if (archiveImpl != null) archiveImpl.delete(collection, documentId, callback);
        else {
            ILog.w(TAG, "archive invalid to use");
            callback.onFailure(collection, documentId, "not_initialized");
        }
    }

    @Override
    public void update(String collection, String documentId, String jsonData, IArchiveResult callback) {
        if (archiveImpl != null) archiveImpl.update(collection, documentId, jsonData, callback);
        else {
            ILog.w(TAG, "archive invalid to use");
            callback.onFailure(collection, documentId, "not_initialized");
        }
    }

    @Override
    public void snapshot(String collection, String documentId, IArchiveResult callback) {
        if (archiveImpl != null) archiveImpl.snapshot(collection, documentId, callback);
        else {
            ILog.w(TAG, "archive invalid to use");
            callback.onFailure(collection, documentId, "not_initialized");
        }
    }

}
