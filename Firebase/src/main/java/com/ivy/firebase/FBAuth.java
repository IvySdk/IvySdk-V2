package com.ivy.firebase;

import android.text.TextUtils;

import androidx.core.util.Consumer;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PlayGamesAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.games.IGet;
import com.ivy.base.games.auth.AuthPlatforms;
import com.ivy.base.games.auth.FirebaseLinkChannel;
import com.ivy.base.games.auth.IAuthResponse;
import com.ivy.base.games.auth.IFirebaseAuth;
import com.ivy.base.games.auth.IFirebaseAuthReload;
import com.ivy.base.games.auth.IFirebaseUnlink;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class FBAuth implements IFirebaseAuth, IUnProguard {
    public static final String TAG = "Firebase";

    private boolean debug = false;
    private IGet getPlayGamesAuthCode;
    private IGet getFacebookAccessToken;
    private IGet getGoogleIdToken;

    @Override
    public void setup(JSONObject jsonObject, boolean debug, IGet getPlayGamesAuthCode, IGet getFacebookAccessToken, IGet getGoogleIdToken) {
        this.debug = debug;
        this.getPlayGamesAuthCode = getPlayGamesAuthCode;
        this.getFacebookAccessToken = getFacebookAccessToken;
        this.getGoogleIdToken = getGoogleIdToken;
    }

    @Override
    public void logout() {
        FirebaseAuth.getInstance().signOut();
    }

    @Override
    public void reloadLastLoginStatus(final IFirebaseAuthReload authReload) {
        ILog.i(TAG, "reload last sign status");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            ILog.i(TAG, "current sign user is null, no sign history");
            authReload.onReload(false, "current user invalid");
        } else {
            ILog.i(TAG, "current user valid! start reload");
            currentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        ILog.i(TAG, "reload task succeed! but user invalid");
                        authReload.onReload(false, "invalid user");
                    } else {
                        ILog.i(TAG, "reload task succeed!");
                        authReload.onReload(true, "");
                    }
                } else {
                    ILog.i(TAG, "reload failed:" + task.getException().getMessage());
                    authReload.onReload(false, task.getException() != null ? task.getException().getMessage() : "unknown");
                }
            });
        }
    }

    @Override
    public String getUserInfo(String channel) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return "{}";

        String providerId = channel2ProviderId(channel);
        if (providerId != null) {
            for (UserInfo provider : user.getProviderData()) {
                if (provider != null && provider.getProviderId().equals(providerId)) {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("id", provider.getUid());
                        json.put("name", provider.getDisplayName() != null ? provider.getDisplayName() : "");
                        json.put("photo", provider.getPhotoUrl() != null ? provider.getPhotoUrl().toString() : "");
                        json.put("email", provider.getEmail() != null ? provider.getEmail() : "");
                        return json.toString();
                    } catch (Exception e) {
                        ILog.e(TAG, "get current user info err:" + e.getMessage());
                        return "{}";
                    }
                }
            }
            return "{}";
        } else {
            try {
                JSONObject json = new JSONObject();
                json.put("id", user.getUid());
                json.put("name", user.getDisplayName() != null ? user.getDisplayName() : "");
                json.put("photo", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                json.put("email", user.getEmail() != null ? user.getEmail() : "");
                return json.toString();
            } catch (Exception e) {
                ILog.e(TAG, "get current user info err:" + e.getMessage());
                return "{}";
            }
        }
    }

    @Override
    public String getUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "";
    }

    @Override
    public boolean isAnonymous() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.isAnonymous() : true;
    }

    @Override
    public boolean isChannelLinked(String channel) {
        if (channel == null || channel.isEmpty()) return false;
        String providerId = channel2ProviderId(channel);
        if (providerId == null) return false;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return false;

        for (UserInfo info : currentUser.getProviderData()) {
            if (info != null && providerId.equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canChannelUnlink(String channel) {
        if (channel == null || channel.isEmpty()) return false;
        String providerId = channel2ProviderId(channel);
        if (PlayGamesAuthProvider.PROVIDER_ID.equals(providerId)) return false;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return false;

        int countLinks = 0;
        boolean contains = false;
        for (UserInfo info : currentUser.getProviderData()) {
            if (info != null) {
                if (providerId.equals(info.getProviderId())) contains = true;
                if (GoogleAuthProvider.PROVIDER_ID.equals(info.getProviderId()) || PlayGamesAuthProvider.PROVIDER_ID.equals(info.getProviderId()) || FacebookAuthProvider.PROVIDER_ID.equals(info.getProviderId()) || EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                    countLinks++;
                }
            }
        }
        return contains && countLinks >= 2;
    }

    @Override
    public void unlinkChannel(final String channel, final IFirebaseUnlink callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "解绑" + channel + "失败，当前未登录", EventState.ERROR, "");
            if (callback != null) callback.onUnlinked(channel, false, "had not sign in");
            return;
        }
        final String providerId = channel2ProviderId(channel);
        if (providerId == null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "解绑" + channel + "失败，无效的渠道Id", EventState.ERROR, "");
            if (callback != null) callback.onUnlinked(channel, false, "unable to unlink");
            return;
        }

        currentUser.unlink(providerId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ILog.i(TAG, "unlink " + channel + " success");
                TestManager.getInstance().addEvent("Firebase_Auth", "解绑" + channel + "成功", EventState.NORMAL, "");
                if (callback != null) {
                    callback.onUnlinked(channel, true, "");
                }
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "unknown";
                TestManager.getInstance().addEvent("Firebase_Auth", "解绑" + channel + "失败", EventState.ERROR, msg);
                ILog.i(TAG, "unlink " + channel + " failed:" + msg);
                if (callback != null) callback.onUnlinked(channel, false, msg);
            }
        });
    }

    @Override
    public void loginAnonymous(final IAuthResponse authResult) {
        ILog.i(TAG, "start sign Anonymous");
        TestManager.getInstance().addEvent("Firebase_Auth", "调用匿名登录", EventState.NORMAL, "");
        FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    ILog.i(TAG, "sign Anonymous succeed! but user invalid");
                    TestManager.getInstance().addEvent("Firebase_Auth", "匿名登录成功，但用户信息创建失败", EventState.ERROR, "");
                    if (authResult != null)
                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.ANONYMOUS, "invalid user");
                } else {
                    ILog.i(TAG, "sign Anonymous succeed");
                    TestManager.getInstance().addEvent("Firebase_Auth", "匿名登录成功", EventState.NORMAL, "用户id:" + user.getUid());
                    if (authResult != null)
                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.ANONYMOUS, "");
                }
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "unknown";
                ILog.i(TAG, "sign Anonymous failed:" + msg);
                TestManager.getInstance().addEvent("Firebase_Auth", "匿名登录失败", EventState.ERROR, msg);
                if (authResult != null)
                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.ANONYMOUS, msg);
            }
        });
    }

    @Override
    public void loginWithGoogle(final IAuthResponse authResult, final boolean reauth) {
        final String idToken = getGoogleIdToken != null ? getGoogleIdToken.get() : null;
        if (idToken == null || idToken.isEmpty()) {
            TestManager.getInstance().addEvent("Firebase_Auth", "绑定Google失败，google token无效", EventState.ERROR, "");
            if (authResult != null)
                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, "invalid play games serverAuthCode");
            return;
        }

        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            ILog.i(TAG, "current user valid! check had linked with google yet");
            TestManager.getInstance().addEvent("Firebase_Auth", "已存在登录用户，开始检测是否已绑定", EventState.NORMAL, "用户id:" + currentUser.getUid());
            for (UserInfo info : currentUser.getProviderData()) {
                if (info != null && GoogleAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                    ILog.i(TAG, "had linked with google !!! just re-auth");
                    TestManager.getInstance().addEvent("Firebase_Auth", "已登录用户已绑定Google，开始重新激活Google", EventState.NORMAL, "用户id:" + currentUser.getUid());
                    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
                    currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user == null) {
                                ILog.i(TAG, "re-auth with google succeed! but user invalid");
                                TestManager.getInstance().addEvent("Firebase_Auth", "重新激活Google成功,但获取用户信息失败", EventState.ERROR, "用户id:" + currentUser.getUid());
                                if (authResult != null)
                                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, "invalid user");
                            } else {
                                ILog.i(TAG, "re-auth with google succeed!");
                                TestManager.getInstance().addEvent("Firebase_Auth", "重新激活Google成功", EventState.NORMAL, "用户id:" + currentUser.getUid());
                                if (authResult != null)
                                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.GOOGLE, "");
                            }
                        } else {
                            String msg = task.getException() != null ? task.getException().getMessage() : "unknown";
                            ILog.i(TAG, "re-auth with google failed:" + msg);
                            TestManager.getInstance().addEvent("Firebase_Auth", "重新激活Google失败", EventState.ERROR, msg);
                            if (authResult != null)
                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, msg);
                        }
                    });
                    return;
                }
            }
            TestManager.getInstance().addEvent("Firebase_Auth", "已登录用户未绑定Google， 开始绑定", EventState.NORMAL, "用户id:" + currentUser.getUid());
            ILog.i(TAG, "start link to google");
            AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
            currentUser.linkWithCredential(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        ILog.i(TAG, "sign succeed with google! but user invalid");
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定Google成功，但获取用户信息失败", EventState.ERROR, "");
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, "invalid user");
                    } else {
                        ILog.i(TAG, "success link to google");
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定Google成功", EventState.NORMAL, "用户id:" + currentUser.getUid());
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.GOOGLE, "");
                    }
                } else {
                    TestManager.getInstance().addEvent("Firebase_Auth", "绑定Google失败，开始拆解原因", EventState.ERROR, "");
                    Exception ex = task.getException();
                    if (ex instanceof FirebaseAuthRecentLoginRequiredException) {
                        ILog.i(TAG, "require recent login re-auth");
                        if (reauth) {
                            TestManager.getInstance().addEvent("Firebase_Auth", "绑定失效，重新激活", EventState.NORMAL, "");
                            reauthentication(status -> {
                                ILog.i(TAG, "require recent login re-auth result:" + status);
                                if (status) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活成功，开始重新绑定", EventState.NORMAL, "");
                                    loginWithGoogle(authResult, false);
                                } else {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活失败", EventState.ERROR, ex.getMessage());
                                    if (authResult != null)
                                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, ex.getMessage());
                                }
                            });
                        } else {
                            ILog.i(TAG, "unable to reauthentication");
                            TestManager.getInstance().addEvent("Firebase_Auth", "已重新激活，仍出现绑定失效", EventState.ERROR, "");
                            if (authResult != null)
                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, ex.getMessage());
                        }
                    } else if (ex instanceof FirebaseAuthUserCollisionException) {
                        ILog.i(TAG, "FirebaseAuthUserCollisionException");
                        TestManager.getInstance().addEvent("Firebase_Auth", "用户碰撞，退出当前用户，重新绑定", EventState.ERROR, "");
                        FirebaseAuth.getInstance().signOut();
                        ILog.i(TAG, "recover previous account, sign out current account");
                        loginWithGoogle(authResult, reauth);
                    } else {
                        String msg = ex != null ? ex.getMessage() : "unknown";
                        ILog.i(TAG, "failed link to google; " + msg);
                        TestManager.getInstance().addEvent("Firebase_Auth", "其它原因，绑定失败", EventState.ERROR, msg);
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, msg);
                    }
                }
            });
        } else {
            ILog.i(TAG, "current user invalid! start sign with google");
            TestManager.getInstance().addEvent("Firebase_Auth", "不存在已登陆用户，开始绑定Google", EventState.NORMAL, "");
            FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        ILog.i(TAG, "sign succeed with google! but user invalid");
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定Google成功,获取用户信息失败", EventState.ERROR, "");
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, "invalid user");
                    } else {
                        ILog.i(TAG, "sign success with google");
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定Google成功", EventState.NORMAL, "用户id:" + user.getUid());
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.GOOGLE, "");
                    }
                } else {
                    TestManager.getInstance().addEvent("Firebase_Auth", "绑定Google失败，开始分析原因", EventState.ERROR, "");
                    Exception ex = task.getException();
                    if (ex instanceof FirebaseAuthRecentLoginRequiredException) {
                        ILog.i(TAG, "require recent login re-auth");
                        if (reauth) {
                            TestManager.getInstance().addEvent("Firebase_Auth", "绑定失效，重新激活", EventState.NORMAL, "");
                            reauthentication(status -> {
                                if (status) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活成功，开始重新绑定", EventState.NORMAL, "");
                                    loginWithGoogle(authResult, false);
                                } else {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活失败", EventState.ERROR, ex.getMessage());
                                    if (authResult != null)
                                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, ex.getMessage());
                                }
                            });
                        } else {
                            TestManager.getInstance().addEvent("Firebase_Auth", "已重新激活，仍出现绑定失效", EventState.ERROR, "");
                            if (authResult != null)
                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, ex.getMessage());
                        }
                    } else {
                        String msg = ex != null ? ex.getMessage() : "unknown";
                        TestManager.getInstance().addEvent("Firebase_Auth", "其它原因，绑定Google失败", EventState.ERROR, msg);
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.GOOGLE, msg);
                    }
                }
            });
        }
    }


    @Override
    public void loginWithPlayGames(final IAuthResponse authResult, final boolean reauth) {
        String serverAuthCode = getPlayGamesAuthCode != null ? getPlayGamesAuthCode.get() : null;
        if (serverAuthCode == null || serverAuthCode.isEmpty()) {
            TestManager.getInstance().addEvent("Firebase_Auth", "绑定PlayGames失败，auth token无效", EventState.ERROR, "");
            if (authResult != null)
                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, "invalid play games serverAuthCode");
            return;
        }

        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "已存在登录用户，开始检测是否已绑定", EventState.NORMAL, "用户id:" + currentUser.getUid());
            ILog.i(TAG, "current user valid! check had linked with play-games yet");
            for (UserInfo info : currentUser.getProviderData()) {
                if (info != null && PlayGamesAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                    ILog.i(TAG, "had linked with play-games !!! just re-auth");
                    TestManager.getInstance().addEvent("Firebase_Auth", "已登录用户已绑定PlayGames，开始重新激活PlayGames", EventState.NORMAL, "用户id:" + currentUser.getUid());
                    AuthCredential credential = PlayGamesAuthProvider.getCredential(serverAuthCode);
                    currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user == null) {
                                TestManager.getInstance().addEvent("Firebase_Auth", "重新激活PlayGames成功,但获取用户信息失败", EventState.ERROR, "用户id:" + currentUser.getUid());
                                ILog.i(TAG, "re-auth with play-games succeed! but user invalid");
                                if (authResult != null)
                                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, "invalid user");
                            } else {
                                TestManager.getInstance().addEvent("Firebase_Auth", "重新激活PlayGames成功", EventState.NORMAL, "用户id:" + currentUser.getUid());
                                ILog.i(TAG, "re-auth with play-games succeed!");
                                if (authResult != null)
                                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.PLAY_GAMES, "");
                            }
                        } else {
                            String msg = task.getException() != null ? task.getException().getMessage() : "unknown";
                            TestManager.getInstance().addEvent("Firebase_Auth", "重新激活PlayGames失败", EventState.ERROR, msg);
                            ILog.i(TAG, "re-auth with play-games failed;" + msg);
                            if (authResult != null)
                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, msg);
                        }
                    });
                    return;
                }
            }

            ILog.i(TAG, "start link to play-games");
            TestManager.getInstance().addEvent("Firebase_Auth", "已登录用户未绑定PlayGames， 开始绑定", EventState.NORMAL, "用户id:" + currentUser.getUid());
            AuthCredential credential = PlayGamesAuthProvider.getCredential(serverAuthCode);
            currentUser.linkWithCredential(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        ILog.i(TAG, "sign succeed with play-games! but user invalid");
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定PlayGames成功，但获取用户信息失败", EventState.ERROR, "");
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, "invalid user");
                    } else {
                        ILog.i(TAG, "success link to play-games");
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定PlayGames成功", EventState.NORMAL, "用户id:" + currentUser.getUid());
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.PLAY_GAMES, "");
                    }
                } else {
                    TestManager.getInstance().addEvent("Firebase_Auth", "绑定PlayGames失败，开始拆解原因", EventState.ERROR, "");
                    Exception ex = task.getException();
                    if (ex instanceof FirebaseAuthRecentLoginRequiredException) {
                        ILog.i(TAG, "require recent login re-auth");
                        if (reauth) {
                            TestManager.getInstance().addEvent("Firebase_Auth", "登录失效，重新激活", EventState.NORMAL, "");
                            reauthentication(status -> {
                                ILog.i(TAG, "require recent login re-auth result:" + status);
                                if (status) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活成功，开始重新绑定", EventState.NORMAL, "");
                                    loginWithPlayGames(authResult, false);
                                } else {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活失败", EventState.ERROR, ex.getMessage());
                                    if (authResult != null)
                                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, ex.getMessage());
                                }
                            });
                        } else {
                            TestManager.getInstance().addEvent("Firebase_Auth", "已重新激活，仍出现绑定失效", EventState.ERROR, "");
                            if (authResult != null)
                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, ex.getMessage());
                        }
                    } else if (ex instanceof FirebaseAuthUserCollisionException) {
                        ILog.i(TAG, "FirebaseAuthUserCollisionException");
                        TestManager.getInstance().addEvent("Firebase_Auth", "用户碰撞，退出当前用户，重新绑定", EventState.ERROR, "");
                        FirebaseAuth.getInstance().signOut();
                        ILog.i(TAG, "recover previous account, sign out current account");
                        loginWithPlayGames(authResult, reauth);
                    } else {
                        String msg = ex != null ? ex.getMessage() : "unknown";
                        TestManager.getInstance().addEvent("Firebase_Auth", "其它原因，绑定失败", EventState.ERROR, msg);
                        ILog.i(TAG, "failed link to play-games; " + msg);
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, msg);
                    }
                }
            });
        } else {
            ILog.i(TAG, "current user invalid! start sign with play-games");
            TestManager.getInstance().addEvent("Firebase_Auth", "不存在已登陆用户，开始绑定PlayGames", EventState.NORMAL, "");
            FirebaseAuth.getInstance().signInWithCredential(PlayGamesAuthProvider.getCredential(serverAuthCode)).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        ILog.i(TAG, "sign succeed with play-games! but user invalid");
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定PlayGames成功,获取用户信息失败", EventState.ERROR, "");
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, "invalid user");
                    } else {
                        ILog.i(TAG, "sign success with play-games");
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定PlayGames成功", EventState.NORMAL, "用户id:" + user.getUid());
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.PLAY_GAMES, "");
                    }
                } else {
                    TestManager.getInstance().addEvent("Firebase_Auth", "绑定PlayGames失败，开始分析原因", EventState.ERROR, "");
                    Exception ex = task.getException();
                    if (ex instanceof FirebaseAuthRecentLoginRequiredException) {
                        if (reauth) {
                            TestManager.getInstance().addEvent("Firebase_Auth", "绑定失效，重新激活", EventState.NORMAL, "");
                            reauthentication(status -> {
                                if (status) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活成功，开始重新绑定", EventState.NORMAL, "");
                                    loginWithPlayGames(authResult, false);
                                } else {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活失败", EventState.ERROR, ex.getMessage());
                                    if (authResult != null)
                                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, ex.getMessage());
                                }
                            });
                        } else {
                            TestManager.getInstance().addEvent("Firebase_Auth", "已重新激活，仍出现绑定失效", EventState.ERROR, "");
                            if (authResult != null)
                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, ex.getMessage());
                        }
                    } else {
                        String msg = ex != null ? ex.getMessage() : "unknown";
                        TestManager.getInstance().addEvent("Firebase_Auth", "其它原因，绑定PlayGames失败", EventState.ERROR, msg);
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.PLAY_GAMES, msg);
                    }
                }
            });
        }
    }

    @Override
    public void loginWithFacebook(final IAuthResponse authResult, final boolean reauth) {
        String accessToken = getFacebookAccessToken != null ? getFacebookAccessToken.get() : null;
        if (accessToken == null || accessToken.isEmpty()) {
            TestManager.getInstance().addEvent("Firebase_Auth", "绑定Facebook失败，access token无效", EventState.ERROR, "");
            if (authResult != null)
                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, FirebaseLinkChannel.FACEBOOK, "invalid facebook access token");
            return;
        }

        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "已存在登录用户，开始检测是否已绑定", EventState.NORMAL, "用户id:" + currentUser.getUid());
            ILog.i(TAG, "current user valid! check had linked with facebook yet");
            for (UserInfo info : currentUser.getProviderData()) {
                if (info != null && FacebookAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                    ILog.i(TAG, "had linked with facebook!!! just re-auth");
                    TestManager.getInstance().addEvent("Firebase_Auth", "已登录用户已绑定Facebook，开始重新激活Facebook", EventState.NORMAL, "用户id:" + currentUser.getUid());
                    AuthCredential credential = FacebookAuthProvider.getCredential(accessToken);
                    currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user == null) {
                                TestManager.getInstance().addEvent("Firebase_Auth", "重新激活Facebook成功,但获取用户信息失败", EventState.ERROR, "用户id:" + currentUser.getUid());
                                if (authResult != null)
                                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, "invalid user");
                            } else {
                                TestManager.getInstance().addEvent("Firebase_Auth", "重新激活Facebook成功", EventState.NORMAL, "用户id:" + currentUser.getUid());
                                if (authResult != null)
                                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.FACEBOOK, "");
                            }
                        } else {
                            String msg = task.getException() != null ? task.getException().getMessage() : "unknown";
                            TestManager.getInstance().addEvent("Firebase_Auth", "重新激活Facebook失败", EventState.ERROR, msg);
                            if (authResult != null)
                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, msg);
                        }
                    });
                    return;
                }
            }

            ILog.i(TAG, "start link to facebook");
            TestManager.getInstance().addEvent("Firebase_Auth", "已登录用户未绑定Facebook，开始绑定", EventState.NORMAL, "用户id:" + currentUser.getUid());
            AuthCredential credential = FacebookAuthProvider.getCredential(accessToken);
            currentUser.linkWithCredential(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定Facebook成功，但获取用户信息失败", EventState.ERROR, "");
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, "invalid user");
                    } else {
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定Facebook成功", EventState.NORMAL, "用户id:" + currentUser.getUid());
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.FACEBOOK, "");
                    }
                } else {
                    TestManager.getInstance().addEvent("Firebase_Auth", "绑定Facebook失败，开始拆解原因", EventState.ERROR, "");
                    Exception ex = task.getException();
                    if (ex instanceof FirebaseAuthRecentLoginRequiredException) {
                        if (reauth) {
                            TestManager.getInstance().addEvent("Firebase_Auth", "绑定失效，重新激活", EventState.NORMAL, "");
                            reauthentication(status -> {
                                if (status) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活成功，开始重新绑定", EventState.NORMAL, "");
                                    loginWithFacebook(authResult, false);
                                } else {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活失败", EventState.ERROR, ex.getMessage());
                                    if (authResult != null)
                                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, ex.getMessage());
                                }
                            });
                        } else {
                            TestManager.getInstance().addEvent("Firebase_Auth", "已重新激活，仍出现绑定失效", EventState.ERROR, "");
                            if (authResult != null)
                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, ex.getMessage());
                        }
                    } else if (ex instanceof FirebaseAuthUserCollisionException) {
                        TestManager.getInstance().addEvent("Firebase_Auth", "用户碰撞，退出当前用户，重新绑定", EventState.ERROR, "");
                        FirebaseAuth.getInstance().signOut();
                        loginWithFacebook(authResult, reauth);
                    } else {
                        String msg = ex != null ? ex.getMessage() : "unknown";
                        TestManager.getInstance().addEvent("Firebase_Auth", "其它原因，绑定失败", EventState.ERROR, msg);
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, msg);
                    }
                }
            });
        } else {
            ILog.i(TAG, "current user invalid! start sign with facebook");
            TestManager.getInstance().addEvent("Firebase_Auth", "不存在已登陆用户，开始绑定Facebook", EventState.NORMAL, "");
            FirebaseAuth.getInstance().signInWithCredential(FacebookAuthProvider.getCredential(accessToken)).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定Facebook成功,获取用户信息失败", EventState.ERROR, "");
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, "invalid user");
                    } else {
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定Facebook成功", EventState.NORMAL, "用户id:" + user.getUid());
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.FACEBOOK, "");
                    }
                } else {
                    TestManager.getInstance().addEvent("Firebase_Auth", "绑定Facebook失败，开始分析原因", EventState.ERROR, "");
                    Exception ex = task.getException();
                    if (ex instanceof FirebaseAuthRecentLoginRequiredException) {
                        if (reauth) {
                            TestManager.getInstance().addEvent("Firebase_Auth", "绑定失效，重新激活", EventState.NORMAL, "");
                            reauthentication(status -> {
                                if (status) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活成功，开始重新绑定", EventState.NORMAL, "");
                                    loginWithFacebook(authResult, false);
                                } else {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活失败", EventState.ERROR, ex.getMessage());
                                    if (authResult != null)
                                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, ex.getMessage());
                                }
                            });
                        } else {
                            TestManager.getInstance().addEvent("Firebase_Auth", "已重新激活，仍出现绑定失效", EventState.ERROR, "");
                            if (authResult != null)
                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, ex.getMessage());
                        }
                    } else {
                        String msg = ex != null ? ex.getMessage() : "unknown";
                        TestManager.getInstance().addEvent("Firebase_Auth", "其它原因，绑定Facebook失败", EventState.ERROR, msg);
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.FACEBOOK, msg);
                    }
                }
            });
        }
    }

    @Override
    public void loginWithEmailAndPassword(final String email, final String password, final IAuthResponse authResult, final boolean reauth) {
        ILog.i(TAG, "start login with email:" + email + "; password:" + password);

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            ILog.i("sign with email failed: invalid email or password");
            TestManager.getInstance().addEvent("Firebase_Auth", "绑定Facebook失败，access token无效", EventState.ERROR, "");
            if (authResult != null)
                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, "invalid email or password");
            return;
        }

        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            TestManager.getInstance().addEvent("Firebase_Auth", "已存在登录用户，开始检测是否已绑定", EventState.NORMAL, "用户id:" + currentUser.getUid());
            ILog.i(TAG, "current user valid! check had linked with email yet");
            boolean handled = false;
            for (UserInfo info : currentUser.getProviderData()) {
                if (info != null && EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                    ILog.i(TAG, "had linked with email!!! just re-auth");
                    TestManager.getInstance().addEvent("Firebase_Auth", "已登录用户已绑定Email，检查是否同一个email", EventState.NORMAL, "用户id:" + currentUser.getUid());
                    String savedEmail = info.getEmail();
                    ILog.i(TAG, "user info email:" + savedEmail);
                    if (TextUtils.isEmpty(savedEmail)) {
                        savedEmail = LocalStorage.Instance.decodeString("__saved_email", null);
                        ILog.i(TAG, "saved email:" + savedEmail);
                    }
//                    String savedEmail = LocalStorage.Instance.decodeString("__saved_email", null);
                    boolean isSameEmail = email.equals(savedEmail);

                    if (isSameEmail) {
                        TestManager.getInstance().addEvent("Firebase_Auth", "同一个email,开始重新激活", EventState.NORMAL, "用户id:" + currentUser.getUid());
                        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
                        currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                if (user == null) {
                                    ILog.i(TAG, "re-auth with email succeed! but user invalid");
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活成功，但是获取用户信息失败", EventState.ERROR, "");
                                    if (authResult != null)
                                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, "invalid user");
                                } else {
                                    ILog.i(TAG, "re-auth with email succeed!");
                                    TestManager.getInstance().addEvent("Firebase_Auth", "重新激活成功", EventState.NORMAL, "用户id:" + currentUser.getUid());
                                    if (authResult != null)
                                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.EMAIL, "");
                                }
                            } else {
                                String msg = task.getException() != null ? task.getException().getMessage() : "unknown";
                                ILog.i(TAG, "re-auth with email failed;" + msg);
                                TestManager.getInstance().addEvent("Firebase_Auth", "重新激活失败", EventState.ERROR, msg);
                                if (authResult != null)
                                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, msg);
                            }
                        });
                        handled = true;
                        break;
                    } else {
                        ILog.i(TAG, "linked with anthor email yet!!! quit current user,login enter email");
//                        FirebaseAuth.getInstance().signOut();
//                        loginWithEmailAndPassword(email, password, authResult, reauth);
                    }
                }
            }

            if (!handled) {
                ILog.i(TAG, "already signed user; just link email");
                TestManager.getInstance().addEvent("Firebase_Auth", "开始绑定Email", EventState.NORMAL, "");
                AuthCredential credential = EmailAuthProvider.getCredential(email, password);
                currentUser.linkWithCredential(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ILog.i(TAG, "link to email success");
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定Email成功", EventState.NORMAL, "");
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.EMAIL, "");
                        LocalStorage.Instance.encodeString("__saved_email", email);
                        LocalStorage.Instance.encodeString("__saved_password", password);
                    } else {
                        TestManager.getInstance().addEvent("Firebase_Auth", "绑定Email失败，开始拆解原因", EventState.NORMAL, "");
                        Exception ex = task.getException();
                        if (ex instanceof FirebaseAuthRecentLoginRequiredException) {
                            ILog.i(TAG, "require recent login re-auth");
                            if (reauth) {
                                TestManager.getInstance().addEvent("Firebase_Auth", "绑定失效，重新激活", EventState.NORMAL, "");
                                reauthentication(result -> {
                                    ILog.i(TAG, "require recent login re-auth result:" + result);
                                    if (result) {
                                        TestManager.getInstance().addEvent("Firebase_Auth", "重新激活成功", EventState.NORMAL, "");
                                        loginWithEmailAndPassword(email, password, authResult, false);
                                    } else {
                                        TestManager.getInstance().addEvent("Firebase_Auth", "重新激活失败", EventState.ERROR, "");
                                        if (authResult != null)
                                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, ex.getMessage());
                                    }
                                });
                            } else {
                                ILog.i(TAG, "unable to reauthentication");
                                TestManager.getInstance().addEvent("Firebase_Auth", "重新激活失败", EventState.ERROR, "");
                                if (authResult != null)
                                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, ex.getMessage());
                            }
                        } else if (ex instanceof FirebaseAuthUserCollisionException) {
                            ILog.i(TAG, "FirebaseAuthUserCollisionException");
                            TestManager.getInstance().addEvent("Firebase_Auth", "用户碰撞，退出当前用户，重新登录", EventState.NORMAL, "");
                            FirebaseAuth.getInstance().signOut();
                            ILog.i(TAG, "recover previous account, sign out current account");
                            loginWithEmailAndPassword(email, password, authResult, reauth);
                        } else if (ex instanceof FirebaseAuthInvalidUserException) {
                            ILog.i(TAG, "invalid user! start create");
                            TestManager.getInstance().addEvent("Firebase_Auth", "无效用户，开始创建用户", EventState.NORMAL, "");
                            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener(createTask -> {
                                if (createTask.isSuccessful()) {
                                    ILog.i(TAG, "create user success, start sign");
                                    TestManager.getInstance().addEvent("Firebase_Auth", "创建用户成功，开始登录", EventState.NORMAL, "");
                                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener(signTask -> {
                                        if (signTask.isSuccessful()) {
                                            ILog.i(TAG, "sign success");
                                            TestManager.getInstance().addEvent("Firebase_Auth", "登录成功", EventState.NORMAL, "");
                                            if (authResult != null)
                                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.EMAIL, "");
                                            LocalStorage.Instance.encodeString("__saved_email", email);
                                            LocalStorage.Instance.encodeString("__saved_password", password);
                                        } else {
                                            String msg = signTask.getException() != null ? signTask.getException().getMessage() : "unknown";
                                            ILog.i(TAG, "sign failed!" + msg);
                                            TestManager.getInstance().addEvent("Firebase_Auth", "登录失败", EventState.ERROR, msg);
                                            if (authResult != null)
                                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, msg);
                                        }
                                    });
                                } else {
                                    String msg = createTask.getException() != null ? createTask.getException().getMessage() : "unknown";
                                    TestManager.getInstance().addEvent("Firebase_Auth", "创建用户失败", EventState.ERROR, msg);
                                    ILog.i(TAG, "create user failed: " + msg);
                                    if (authResult != null)
                                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, msg);
                                }
                            });
                        } else {
                            String msg = ex != null ? ex.getMessage() : "unknown";
                            ILog.i(TAG, "sign with email failed:" + msg);
                            TestManager.getInstance().addEvent("Firebase_Auth", "其它原因，绑定失败", EventState.ERROR, msg);
                            if (authResult != null)
                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, msg);
                        }
                    }
                });
            }
        } else {
            ILog.i(TAG, "no user signed! sign to email");
            TestManager.getInstance().addEvent("Firebase_Auth", "开始登录", EventState.NORMAL, "");
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener(signTask -> {
                if (signTask.isSuccessful()) {
                    ILog.i(TAG, "sign with email success");
                    TestManager.getInstance().addEvent("Firebase_Auth", "登录成功", EventState.NORMAL, "");
                    if (authResult != null)
                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.EMAIL, "");
                    LocalStorage.Instance.encodeString("__saved_email", email);
                    LocalStorage.Instance.encodeString("__saved_password", password);
                } else {
                    TestManager.getInstance().addEvent("Firebase_Auth", "登录失败, 开始拆解原因", EventState.ERROR, "");
                    Exception ex = signTask.getException();
                    if (ex instanceof FirebaseAuthRecentLoginRequiredException) {
                        if (reauth) {
                            TestManager.getInstance().addEvent("Firebase_Auth", "绑定失效，重新激活", EventState.ERROR, "");
                            ILog.i(TAG, "sign with email require recent login re-auth");
                            reauthentication(result -> {
                                if (result) {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "激活成功，重新绑定", EventState.NORMAL, "");
                                    loginWithEmailAndPassword(email, password, authResult, false);
                                } else {
                                    TestManager.getInstance().addEvent("Firebase_Auth", "激活失败，绑定失败", EventState.ERROR, "");
                                    if (authResult != null)
                                        authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, ex.getMessage());
                                }
                            });
                        } else {
                            ILog.i(TAG, "sign with email unable to reauthentication");
                            TestManager.getInstance().addEvent("Firebase_Auth", "激活后，绑定依然无效", EventState.ERROR, "");
                            if (authResult != null)
                                authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, ex.getMessage());
                        }
                    } else if (ex instanceof FirebaseAuthInvalidUserException) {
                        ILog.i(TAG, "sign with email invalid user! start create");
                        TestManager.getInstance().addEvent("Firebase_Auth", "无效用户，开始创建用户", EventState.NORMAL, "");
                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener(createTask -> {
                            if (createTask.isSuccessful()) {
                                ILog.i(TAG, "create user success, start sign");
                                TestManager.getInstance().addEvent("Firebase_Auth", "创建用户成功，开始登录", EventState.NORMAL, "");
                                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener(innerSignTask -> {
                                    if (innerSignTask.isSuccessful()) {
                                        ILog.i(TAG, "sign success");
                                        TestManager.getInstance().addEvent("Firebase_Auth", "登录成功", EventState.NORMAL, "");
                                        if (authResult != null)
                                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, true, FirebaseLinkChannel.EMAIL, "");
                                        LocalStorage.Instance.encodeString("__saved_email", email);
                                        LocalStorage.Instance.encodeString("__saved_password", password);
                                    } else {
                                        String msg = innerSignTask.getException() != null ? innerSignTask.getException().getMessage() : "unknown";
                                        ILog.i(TAG, "sign failed!" + msg);
                                        TestManager.getInstance().addEvent("Firebase_Auth", "登录失败", EventState.ERROR, msg);
                                        if (authResult != null)
                                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, msg);
                                    }
                                });
                            } else {
                                String msg = createTask.getException() != null ? createTask.getException().getMessage() : "unknown";
                                ILog.i(TAG, "create user failed: " + msg);
                                TestManager.getInstance().addEvent("Firebase_Auth", "创建用户失败", EventState.ERROR, msg);
                                if (authResult != null)
                                    authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, msg);
                            }
                        });
                    } else {
                        String msg = ex != null ? ex.getMessage() : "unknown";
                        ILog.i(TAG, "sign with email failed:" + msg);
                        TestManager.getInstance().addEvent("Firebase_Auth", "其它原因，绑定失败", EventState.ERROR, msg);
                        if (authResult != null)
                            authResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FIREBASE, false, FirebaseLinkChannel.EMAIL, msg);
                    }
                }
            });
        }
    }

    private void reauthentication(final Consumer<Boolean> onResult) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            ILog.i(TAG, "reauthentication failed; no signed user");
            onResult.accept(false);
            return;
        }

        Set<String> providerIds = new HashSet<>();
        for (UserInfo info : currentUser.getProviderData()) {
            if (info != null) providerIds.add(info.getProviderId());
        }

        if (providerIds.contains(GoogleAuthProvider.PROVIDER_ID)) {
            ILog.i(TAG, "start reauthentication with google");
            String idToken = getGoogleIdToken != null ? getGoogleIdToken.get() : null;
            if (idToken != null) {
                currentUser.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).addOnCompleteListener(task -> onResult.accept(task.isSuccessful()));
            } else {
                ILog.i(TAG, "reauthentication failed; unable get google idToken");
                onResult.accept(false);
            }
        } else if (providerIds.contains(PlayGamesAuthProvider.PROVIDER_ID)) {
            ILog.i(TAG, "start reauthentication with play games");
            String authCode = getPlayGamesAuthCode != null ? getPlayGamesAuthCode.get() : null;
            if (authCode != null) {
                currentUser.reauthenticate(PlayGamesAuthProvider.getCredential(authCode)).addOnCompleteListener(task -> onResult.accept(task.isSuccessful()));
            } else {
                ILog.i(TAG, "reauthentication failed; unable get play games server-auth-code");
                onResult.accept(false);
            }
        } else if (providerIds.contains(FacebookAuthProvider.PROVIDER_ID)) {
            ILog.i(TAG, "start reauthentication with facebook");
            String accessToken = getFacebookAccessToken != null ? getFacebookAccessToken.get() : null;
            if (accessToken != null) {
                currentUser.reauthenticate(FacebookAuthProvider.getCredential(accessToken)).addOnCompleteListener(task -> onResult.accept(task.isSuccessful()));
            } else {
                ILog.i(TAG, "reauthentication failed; unable get facebook accessToken");
                onResult.accept(false);
            }
        } else if (providerIds.contains(EmailAuthProvider.PROVIDER_ID)) {
            ILog.i(TAG, "start reauthentication with email");
            String savedEmail = LocalStorage.Instance.decodeString("__saved_email", null);
            String savedPassword = LocalStorage.Instance.decodeString("__saved_password", null);
            if (savedEmail == null || savedEmail.isEmpty() || savedPassword == null || savedPassword.isEmpty()) {
                ILog.i(TAG, "reauthentication failed; invalid email or password");
                onResult.accept(false);
            } else {
                currentUser.reauthenticate(EmailAuthProvider.getCredential(savedEmail, savedPassword)).addOnCompleteListener(task -> onResult.accept(task.isSuccessful()));
            }
        } else {
            ILog.i(TAG, "reauthentication failed; not valid provider id");
            onResult.accept(false);
        }
    }

    private String channel2ProviderId(String channel) {
        switch (channel) {
            case FirebaseLinkChannel.GOOGLE:
                return GoogleAuthProvider.PROVIDER_ID;
            case FirebaseLinkChannel.PLAY_GAMES:
                return PlayGamesAuthProvider.PROVIDER_ID;
            case FirebaseLinkChannel.FACEBOOK:
                return FacebookAuthProvider.PROVIDER_ID;
            case FirebaseLinkChannel.EMAIL:
                return EmailAuthProvider.PROVIDER_ID;
            case FirebaseLinkChannel.ANONYMOUS:
                return FirebaseLinkChannel.ANONYMOUS;
            default:
                return null;
        }
    }


}
