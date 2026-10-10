package com.ivy.facebook;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;
import com.facebook.Profile;
import com.facebook.internal.Utility;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.ivy.base.ActivityUtil;
import com.ivy.base.Callback;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.games.auth.AuthPlatforms;
import com.ivy.base.games.auth.IAuthResponse;
import com.ivy.base.games.auth.IAuthResult;
import com.ivy.base.games.auth.IFacebookAuth;
import com.ivy.base.http.HttpUtil;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FacebookAuth implements IFacebookAuth, IUnProguard {

    public static final String TAG = "Facebook";

    public static final String PERMISSION_PUBLIC_PROFILE = "public_profile";
    public static final String PERMISSION_EMAIL = "email";
    public static final String PERMISSION_USER_FRIENDS = "user_friends";

    private IAuthResult globalAuthResult;
    private final List<String> permissions = new ArrayList<>();
    private CallbackManager callbackManager;

    private String friends = "[]";
    private IAuthResponse authCallback;

    private String appId = "";
    private String verifyUrl;

    private boolean autoSignIn = false;

    @Override
    public void setup(String appId, JSONObject jsonObject, boolean debug, IAuthResult authResult) {
        this.appId = appId;
        this.globalAuthResult = authResult;
        callbackManager = CallbackManager.Factory.create();
        verifyUrl = jsonObject.optString("verify-url");
        this.autoSignIn = jsonObject.optBoolean("auto_login", false);
        try {
            JSONArray permissionArr = jsonObject.getJSONArray("permissions");
            for (int index = 0; index < permissionArr.length(); index++) {
                permissions.add(permissionArr.optString(index));
            }
            ILog.i(TAG, "setup permissions:" + TextUtils.join(",", permissions));
        } catch (Exception e) {
            ILog.e(TAG, "format configured permission err:" + e.getMessage() + "; setup default permissions");
            permissions.add(PERMISSION_EMAIL);
            permissions.add(PERMISSION_PUBLIC_PROFILE);
        }
        if (autoSignIn) {
            autoLogin();
        } else {
            refreshAccessToken();
        }
    }

    @Override
    public String getFriends() {
        return friends;
    }

    @Override
    public String requireAccessToken() {
        AccessToken token = AccessToken.getCurrentAccessToken();
        return token != null ? token.getToken() : null;
    }

    @Override
    public void autoLogin() {
        if (!FacebookSdk.isInitialized()) {
            if (globalAuthResult != null) {
                globalAuthResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "facebook sdk not initialized");
            }
            return;
        }
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null && !accessToken.isExpired()) {
            if (TextUtils.isEmpty(verifyUrl)) {
                ILog.i(TAG, "auto login load login state: false");
                if (globalAuthResult != null) {
                    globalAuthResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, null);
                }
            } else {
                new Thread(() -> {
                    boolean verifyResult = verifyLogin(verifyUrl, accessToken.getToken());
                    ILog.i(TAG, "auto login verify result:" + verifyResult);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (globalAuthResult != null) {
                            if (verifyResult) {
                                globalAuthResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, true, null, null);
                            } else {
                                globalAuthResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "verify failed");
                            }
                        }
                    });
                }).start();
            }
        } else {
            ILog.i(TAG, "auto login failed");
            if (globalAuthResult != null) {
                globalAuthResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, null);
            }
        }
    }

    @Override
    public void loadLoginStatus() {
        // 未实现
    }

    @Override
    public void login(IAuthResponse callback) {
        TestManager.getInstance().addEvent("Facebook_Auth", "开始登录", EventState.NORMAL, "");
        IAuthResponse cb = (callback != null) ? callback : globalAuthResult;
        if (!FacebookSdk.isInitialized()) {
            if (cb != null)
                cb.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "facebook sdk not initialized");
            TestManager.getInstance().addEvent("Facebook_Auth", "登录失败", EventState.WARNING, "facebook sdk 没有初始化");
            return;
        }
        boolean loginStatus = getLoginStatus();
        if (loginStatus) {
            if (TextUtils.isEmpty(verifyUrl)) {
                if (cb != null)
                    cb.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, true, null, null);
                TestManager.getInstance().addEvent("Facebook_Auth", "登录成功", EventState.NORMAL, "");
            } else {
                AccessToken token = AccessToken.getCurrentAccessToken();
                if (token != null) {
                    new Thread(() -> {
                        boolean verifyResult = verifyLogin(verifyUrl, token.getToken());
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (cb != null) {
                                if (verifyResult) {
                                    cb.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, true, null, null);
                                    TestManager.getInstance().addEvent("Facebook_Auth", "登录成功", EventState.NORMAL, "");
                                } else {
                                    cb.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "verify failed");
                                    TestManager.getInstance().addEvent("Facebook_Auth", "登录失败", EventState.WARNING, "");
                                }
                            }
                        });
                    }).start();
                } else {
                    TestManager.getInstance().addEvent("Facebook_Auth", "登录失败", EventState.WARNING, "无效的AccessToken");
                    ILog.e(TAG, "response success, but invalid access token returned");
                    if (cb != null)
                        cb.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "invalid access token");
                }
            }
        } else {
            Activity activity = ActivityUtil.getInstance().getActivity();
            if (activity != null) {
                this.authCallback = callback;
                LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onCancel() {
                        ILog.i(TAG, "sign in failed:user canceled");
                        TestManager.getInstance().addEvent("Facebook_Auth", "登录失败", EventState.WARNING, "用户取消登录");
                        if (cb != null)
                            cb.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "user canceled");
                    }

                    @Override
                    public void onError(FacebookException error) {
                        ILog.i(TAG, "sign in failed:" + error.getMessage());
                        if (cb != null)
                            cb.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, error.getMessage());
                        TestManager.getInstance().addEvent("Facebook_Auth", "登录失败", EventState.ERROR, error.getMessage());
                    }

                    @Override
                    public void onSuccess(LoginResult result) {
                        ILog.i(TAG, "sign in success:");
                        Set<String> grantedPermissions = result.getRecentlyGrantedPermissions();
                        ILog.i(TAG, "granted permissions:" + TextUtils.join(",", grantedPermissions));

                        if (TextUtils.isEmpty(verifyUrl)) {
                            onLoginSuccess(grantedPermissions, cb);
                            TestManager.getInstance().addEvent("Facebook_Auth", "登录成功", EventState.NORMAL, "");
                        } else {
                            AccessToken token = AccessToken.getCurrentAccessToken();
                            if (token != null) {
                                new Thread(() -> {
                                    boolean verifyResult = verifyLogin(verifyUrl, token.getToken());
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        if (verifyResult) {
                                            onLoginSuccess(grantedPermissions, cb);
                                            TestManager.getInstance().addEvent("Facebook_Auth", "登录成功", EventState.NORMAL, "登录&校验完成，登录成功");
                                        } else {
                                            if (cb != null)
                                                cb.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "verify failed");
                                            TestManager.getInstance().addEvent("Facebook_Auth", "登录失败", EventState.ERROR, "登录回调成功， 中台校验失败，请检查校验配置");
                                        }
                                    });
                                }).start();
                            } else {
                                ILog.e(TAG, "response success, but invalid access token returned");
                                if (cb != null)
                                    cb.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "invalid access token");
                                TestManager.getInstance().addEvent("Facebook_Auth", "登录失败", EventState.ERROR, "登录回调成功，但是返回无效AccessToken,建议重试");
                            }
                        }
                    }
                });
                LoginManager.getInstance().logIn(activity, permissions);
            } else {
                ILog.i(TAG, "sign in failed: invalid activity");
                if (cb != null)
                    cb.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "invalid activity");
                TestManager.getInstance().addEvent("Facebook_Auth", "登录失败", EventState.ERROR, "SDK初始化故障");
            }
        }
    }

    @Override
    public void logout() {
        LoginManager.getInstance().logOut();
        if (globalAuthResult != null) {
            globalAuthResult.onLogout(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK);
        }
    }

    @Override
    public void logout(Callback callback) {

    }

    @Override
    public boolean isTokenValid() {
        return false;
    }

    @Override
    public boolean getLoginStatus() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null && !accessToken.isExpired();
    }

    @Override
    public String getUserInfo() {
        String profileJson = checkProfile();
        if (profileJson != null) {
            try {
                JSONObject json = new JSONObject();
                Profile profile = Profile.getCurrentProfile();
                if (profile != null) {
                    json.put("id", profile.getId() != null ? profile.getId() : "");
                    String name = profile.getName();
                    json.put("name", name != null ? name : "");
                    json.put("photo", profile.getProfilePictureUri(124, 124).toString());
                    AccessToken accessToken = AccessToken.getCurrentAccessToken();
                    String token = accessToken != null ? accessToken.getToken() : "";
                    json.put("token", token);
                }
                return json.toString();
            } catch (Exception ignored) {
            }
        }
        return "{}";
    }

    @Override
    public String getUserId() {
        Profile profile = Profile.getCurrentProfile();
        return profile != null ? profile.getId() : "";
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        ILog.i(TAG, "received:onActivityResult::requestCode:" + requestCode + ";resultCode:" + resultCode);
        if (callbackManager != null) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onLoginSuccess(Set<String> grantedPermissions, IAuthResponse callback) {
        requireUserInfo(callback);
        if (grantedPermissions.contains(PERMISSION_USER_FRIENDS)) {
            ILog.i(TAG, "friends permission allowed; start load friends list");
            requireFriends();
        }
    }

    private void refreshAccessToken() {
        if (FacebookSdk.isInitialized()) {
            AccessToken.refreshCurrentAccessTokenAsync(new AccessToken.AccessTokenRefreshCallback() {
                @Override
                public void OnTokenRefreshFailed(FacebookException exception) {
                    ILog.i(TAG, "refresh access token failed:" + (exception != null ? exception.getMessage() : ""));
                }

                @Override
                public void OnTokenRefreshed(AccessToken accessToken) {
                    ILog.i(TAG, "refresh access token succeed:" + accessToken);
                }
            });
        }
    }

    private String checkProfile() {
        try {
            Profile profile = Profile.getCurrentProfile();
            if (profile != null && profile.getId() != null && !profile.getId().isEmpty()) {
                return profile.toJSONObject().toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void requireUserInfo(IAuthResponse callback) {
        TestManager.getInstance().addEvent("Facebook_Auth", "开始拉取用户信息", EventState.NORMAL, "");
        String profileStr = checkProfile();
        if (profileStr != null) {
            ILog.i(TAG, "got user info from profile:" + profileStr);
            if (callback != null)
                callback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, true, null, null);
            TestManager.getInstance().addEvent("Facebook_Auth", "用户信息拉取成功", EventState.NORMAL, "");
        } else {
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            if (accessToken != null) {
                Utility.getGraphMeRequestWithCacheAsync(accessToken.getToken(), new Utility.GraphMeRequestWithCacheCallback() {
                    @Override
                    public void onFailure(FacebookException error) {
                        ILog.i(TAG, "request user info failed:" + (error != null ? error.getMessage() : ""));
                        if (callback != null) {
                            callback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "load user failed:" + (error != null ? error.getMessage() : ""));
                        }
                        TestManager.getInstance().addEvent("Facebook_Auth", "用户信息拉取失败", EventState.WARNING, (error != null ? error.getMessage() : ""));
                    }

                    @Override
                    public void onSuccess(JSONObject userInfo) {
                        if (userInfo != null) {
                            String id = userInfo.optString("id");
                            if (id == null || id.isEmpty()) {
                                ILog.i(TAG, "user id is null! invalid state");
                                if (callback != null)
                                    callback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "invalid user id");
                                TestManager.getInstance().addEvent("Facebook_Auth", "用户信息拉取失败", EventState.WARNING, "用户id无效");
                            } else {
                                ILog.i(TAG, "loaded user info:" + userInfo.toString());
                                String link = userInfo.optString("link_uri");
                                Uri linkUri = !TextUtils.isEmpty(link) ? Uri.parse(link) : null;
                                String picture = userInfo.optString("picture_uri");
                                Uri pictureUri = !TextUtils.isEmpty(picture) ? Uri.parse(picture) : null;
                                Profile profile = new Profile(id, userInfo.optString("first_name"), userInfo.optString("middle_name"), userInfo.optString("last_name"), userInfo.optString("name"), linkUri, pictureUri);
                                Profile.setCurrentProfile(profile);
                                if (callback != null)
                                    callback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, true, null, null);

                                TestManager.getInstance().addEvent("Facebook_Auth", "用户信息成功", EventState.NORMAL, userInfo.toString());
                            }
                        } else {
                            ILog.e(TAG, "load user success, but invalid response returned");
                            TestManager.getInstance().addEvent("Facebook_Auth", "用户信息拉取失败", EventState.WARNING, "回调成功，但返回无效信息");
                            if (callback != null)
                                callback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "invalid user info response");
                        }
                    }
                });
            } else {
                ILog.w(TAG, "load user info failed; access token invalid");
                TestManager.getInstance().addEvent("Facebook_Auth", "用户信息拉取失败", EventState.WARNING, "登录AccessToken无效，拉取用户信息失败");
                if (callback != null)
                    callback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_FACEBOOK, false, null, "invalid access token");
            }
        }
    }

    private void requireFriends() {
        Bundle bundle = new Bundle();
        bundle.putString("fields", "picture,name,id");
        new GraphRequest(AccessToken.getCurrentAccessToken(), "me/friends", bundle, HttpMethod.GET, response -> {
            try {
                JSONArray data = response.getJSONObject().optJSONArray("data");
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        try {
                            JSONObject item = data.optJSONObject(i);
                            if (item != null) {
                                JSONObject pic = item.optJSONObject("picture");
                                if (pic != null) {
                                    JSONObject d = pic.optJSONObject("data");
                                    if (d != null) {
                                        item.put("picture", d.optString("url", ""));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            ILog.e(TAG, "parse friend list item err:" + e.getMessage());
                        }
                    }
                    friends = data.toString();
                } else {
                    ILog.i(TAG, "request friends list failed! invalid response");
                    friends = "[]";
                }
            } catch (Exception e) {
                ILog.i(TAG, "request friends list failed! invalid response");
                friends = "[]";
            }
        }).executeAsync();
    }

    private boolean verifyLogin(String url, String token) {
        try {
            JSONObject json = new JSONObject();
            json.put("is_encrypt", false);
            JSONObject data = new JSONObject();
            data.put("app_id", appId);
            data.put("token", token);
            json.put("data", data);
            OkHttpClient okHttpClient = HttpUtil.getInstance().getOkHttpClient();
            MediaType mediaType = MediaType.get("application/json; charset=utf-8");
            RequestBody postBody = RequestBody.create(json.toString(), mediaType);
            Request request = new Request.Builder().url(url).post(postBody).build();
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                String str = response.body().string();
                String userId = new JSONObject(str).getString("data");
                if (userId != null && !userId.isEmpty()) {
                    return true;
                } else {
                    TestManager.getInstance().addEvent("Facebook_Auth", "中台校验失败", EventState.WARNING, "返回非法用户ID");
                }
            }
        } catch (Exception e) {
            ILog.e(TAG, "verify login failed:" + e.getMessage());
        }
        return false;
    }

}
