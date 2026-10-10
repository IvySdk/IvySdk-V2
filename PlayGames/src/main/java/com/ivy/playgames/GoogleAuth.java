package com.ivy.playgames;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.Callback;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.games.auth.AuthPlatforms;
import com.ivy.base.games.auth.IAuthResponse;
import com.ivy.base.games.auth.IAuthResult;
import com.ivy.base.games.auth.IGoogleAuth;
import com.ivy.base.http.HttpUtil;
import com.ivy.base.local.LocalStorage;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;
import com.ivy.base.util.rx.Func;
import com.ivy.base.util.rx.Observer;
import com.ivy.base.util.rx.RxAndroid;
import com.ivy.base.util.rx.Schedulers;
import com.ivy.base.util.rx.SimpleRxJust;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GoogleAuth implements IGoogleAuth, IUnProguard {
    private static final String TAG = "G+";

    private String appId = "";
    private String webClientId = null;
    private IAuthResult globalAuthResult = null;
    private boolean isAuthenticated = false;
    private String userInfo = "{}";
    // private String serverAuthCode = "";
    private String verifyUrl = null;
    private String idToken = "";
    private boolean autoSignIn = false;

    private final CredentialManager credentialManager = CredentialManager.create(App.getInstance());

    @Override
    public void setup(String appId, JSONObject jsonObject, boolean debug, IAuthResult authResult) {
        this.appId = appId;
        this.globalAuthResult = authResult;
        this.webClientId = jsonObject.optString("web_client_id");
        this.autoSignIn = jsonObject.optBoolean("auto_login", false);
        if (webClientId == null || webClientId.isEmpty()) {
            ILog.e(TAG, "web_client_id can not be empty!!!");
        }
        this.verifyUrl = jsonObject.optString("verify-url");
        if (autoSignIn) {
            autoLogin();
        }
        checkUserInfo();
    }

    private void saveUserInfo(String info) {
        LocalStorage.Instance.encodeString("__google_user__", info);
    }

    private void checkUserInfo() {
        try {
            userInfo = LocalStorage.Instance.decodeString("__google_user__", "");
            JSONObject json = new JSONObject(userInfo);
            idToken = json.getString("token");
            String id = json.getString("id");
            if (TextUtils.isEmpty(id) || TextUtils.isEmpty(idToken)) {
                throw new IllegalArgumentException("user id or token invalid");
            }
            isAuthenticated = true;
        } catch (Throwable t) {
            isAuthenticated = false;
            userInfo = "{}";
            idToken = "";
        }
    }

    @Override
    public boolean isTokenValid() {
        try {
            String[] segments = idToken.split("\\.");
            byte[] payloadAsByteArray = Base64.decode(segments[1], Base64.URL_SAFE);
            String payloadInJson = new String(payloadAsByteArray, StandardCharsets.UTF_8);
            JSONObject jsonPayload = new JSONObject(payloadInJson);
            long exp = jsonPayload.getLong("exp");
            return exp * 1000 - System.currentTimeMillis() > 0;
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public void autoLogin() {
        doAutoSignIn();
    }

    @Override
    public void loadLoginStatus() {
        // no-op
    }

    @Override
    public void login(IAuthResponse authCallback) {
        IAuthResponse callback = (authCallback != null) ? authCallback : globalAuthResult;
        if (getLoginStatus()) {
            logout(new Callback() {
                @Override
                public void onResult() {
                    doSignIn(true, callback);
                }
            });
        } else {
            doSignIn(true, callback);
        }
    }

    @Override
    public void logout(Callback callback) {
        try {
            ClearCredentialStateRequest request = new ClearCredentialStateRequest();
            credentialManager.clearCredentialStateAsync(request, null, Executors.newCachedThreadPool(), new CredentialManagerCallback<Void, ClearCredentialException>() {
                @Override
                public void onResult(Void unused) {
                    isAuthenticated = false;
                    userInfo = "{}";
                    idToken = "";
                    saveUserInfo("");
                    if (callback != null) {
                        callback.onResult();
                    }
                }

                @Override
                public void onError(@NonNull ClearCredentialException e) {
                    if (callback != null) {
                        callback.onResult();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.onResult();
            }
        }
    }

    @Override
    public void logout() {
        try {
            ClearCredentialStateRequest request = new ClearCredentialStateRequest();
            credentialManager.clearCredentialStateAsync(request, null, Executors.newCachedThreadPool(), new CredentialManagerCallback<Void, ClearCredentialException>() {
                @Override
                public void onResult(Void unused) {
                    ILog.d(TAG, "logout google success");
                    isAuthenticated = false;
                    userInfo = "{}";
                    idToken = "";
                    saveUserInfo("");
                    if (globalAuthResult != null) {
                        globalAuthResult.onLogout(AuthPlatforms.LOGIN_PLATFORM_GOOGLE);
                    }
                }

                @Override
                public void onError(@NonNull ClearCredentialException e) {
                    ILog.e(TAG, "logout google failed:" + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean getLoginStatus() {
        return isAuthenticated;
    }

    @Override
    public String getUserInfo() {
        return userInfo;
    }

    @Override
    public String getUserId() {
        try {
            return new JSONObject(userInfo).optString("id");
        } catch (Exception e) {
            return "";
        }
    }

    public String getIdToken() {
        return idToken;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // no-op
    }

    private GetGoogleIdOption buildGoogleSignOption(boolean filterByAuthorizedAccounts) {
        GetGoogleIdOption.Builder optionBuilder = new GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(filterByAuthorizedAccounts).setAutoSelectEnabled(true);
        if (webClientId != null) {
            optionBuilder.setServerClientId(webClientId);
        } else {
            ILog.w(TAG, "invalid web_client_id");
        }
        return optionBuilder.build();
    }

    private void loadUserInfo(GoogleIdTokenCredential credential) {
        try {
            if (idToken == null || idToken.isEmpty()) {
                ILog.e(TAG, "load user info error:idToken can not be null");
                return;
            }
            String id = credential.getId();
            try {
                String[] segments = idToken.split("\\.");
                byte[] payloadAsByteArray = Base64.decode(segments[1], Base64.URL_SAFE);
                String payloadInJson = new String(payloadAsByteArray, StandardCharsets.UTF_8);
                JSONObject jsonPayload = new JSONObject(payloadInJson);
                id = jsonPayload.getString("sub");
            } catch (Exception e1) {
                ILog.e(TAG, "parse user id from idToken failed:" + e1.getMessage());
                e1.printStackTrace();
            }
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("name", credential.getDisplayName() != null ? credential.getDisplayName() : "");
            json.put("photo", credential.getProfilePictureUri() != null ? credential.getProfilePictureUri().toString() : "");
            json.put("token", idToken);
            userInfo = json.toString();
        } catch (Exception e) {
            ILog.e(TAG, "parse user info failed:" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void doAutoSignIn() {
        try {
            Activity activity = ActivityUtil.getInstance().getActivity();
            if (activity == null) {
                ILog.e(TAG, "auto login err:invalid activity");
                if (globalAuthResult != null) {
                    globalAuthResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, "invalid activity");
                }
                return;
            }
            GetGoogleIdOption option = buildGoogleSignOption(true);
            GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(option).build();
            credentialManager.getCredentialAsync(activity, request, null, Executors.newCachedThreadPool(), new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                @Override
                public void onResult(GetCredentialResponse result) {
                    try {
                        handleSignResult(result, (signResult, reSign, credential) -> {
                            if (signResult && credential != null) {
                                ILog.e(TAG, "auto login success");
                                idToken = credential.getIdToken();
                                loadUserInfo(credential);
                                if (verifyUrl == null || verifyUrl.isEmpty()) {
                                    isAuthenticated = true;
                                    saveUserInfo(userInfo);
                                    if (globalAuthResult != null) {
                                        globalAuthResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, true, null, null);
                                    }
                                } else {
                                    new SimpleRxJust<>(idToken).map(new Func<String, Pair<Boolean, String>>() {

                                        @Override
                                        public Pair<Boolean, String> call(String value) throws Exception {
                                            return verifyLogin(verifyUrl, value);
                                        }
                                    }).subscribeOn(Schedulers.io()).observeOn(RxAndroid.main()).subscribe(new Observer<Pair<Boolean, String>>() {
                                        @Override
                                        public void onSuccess(Pair<Boolean, String> verifyResult) {
                                            isAuthenticated = verifyResult.first;
                                            String id = verifyResult.second;
                                            if (id != null) {
                                                try {
                                                    new JSONObject(userInfo).put("id", id);
                                                } catch (Exception e) {
                                                    ILog.e(TAG, "reset user id from server error:" + e.getMessage());
                                                }
                                            }
                                            saveUserInfo(userInfo);
                                            if (globalAuthResult != null) {
                                                globalAuthResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, isAuthenticated, null, null);
                                            }
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            isAuthenticated = false;
                                            if (globalAuthResult != null) {
                                                globalAuthResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, null);
                                            }
                                        }
                                    });
                                }
                            } else {
                                ILog.e(TAG, "auto login failed");
                                isAuthenticated = false;
                                if (globalAuthResult != null) {
                                    globalAuthResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, null);
                                }
                            }
                        });
                    } catch (Exception e) {
                        ILog.e(TAG, "auto sign failed with handleSignResult:" + e.getMessage());
                        if (globalAuthResult != null) {
                            globalAuthResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, "invalid activity");
                        }
                    }
                }

                @Override
                public void onError(@NonNull GetCredentialException e) {
                    ILog.e(TAG, "auto sign failed with GetCredentialException:" + e.getMessage());
                    if (globalAuthResult != null) {
                        globalAuthResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, "invalid activity");
                    }
                }
            });

        } catch (Exception e) {
            ILog.e(TAG, "auto login failed:" + e.getMessage());
            isAuthenticated = false;
            if (globalAuthResult != null) {
                globalAuthResult.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, e.getMessage());
            }
        }
    }

    private void doSignIn(boolean filterByAuthorizedAccounts, IAuthResponse authCallback) {
        Activity activity = ActivityUtil.getInstance().getActivity();
        if (activity == null) {
            ILog.e(TAG, "auto login err:invalid activity");
            TestManager.getInstance().addEvent("Google_Auth", "开始登录Google", EventState.ERROR, "Activity 对象为null，检查初始化逻辑");
            if (authCallback != null) {
                authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, "invalid activity");
            }
            return;
        }
        try {
            TestManager.getInstance().addEvent("Google_Auth", "开始获取登录证书", EventState.NORMAL, "");
            GetGoogleIdOption option = buildGoogleSignOption(filterByAuthorizedAccounts);
            GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(option).build();
            credentialManager.getCredentialAsync(activity, request, null, Executors.newCachedThreadPool(), new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                @Override
                public void onResult(GetCredentialResponse result) {
                    try {
                        TestManager.getInstance().addEvent("Google_Auth", "开始校验登录证书", EventState.NORMAL, "");
                        handleSignResult(result, (signResult, reSign, credential) -> {
                            if (signResult) {
                                if (credential == null) {
                                    ILog.e(TAG, "sign in success, but invalid credential returned");
                                    if (authCallback != null) {
                                        authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, null);
                                    }
                                } else {
                                    ILog.e(TAG, "sign in success");
                                    idToken = credential.getIdToken();
                                    loadUserInfo(credential);
                                    if (verifyUrl == null || verifyUrl.isEmpty()) {
                                        TestManager.getInstance().addEvent("Google_Auth", "登录成功,未经中台校验", EventState.WARNING, "检查是否配置校验地址");
                                        isAuthenticated = true;
                                        saveUserInfo(userInfo);
                                        if (authCallback != null) {
                                            authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, true, null, null);
                                        }
                                    } else {
                                        TestManager.getInstance().addEvent("Google_Auth", "开始中台校验登录结果", EventState.NORMAL, "");
                                        new SimpleRxJust<>(idToken).map(new Func<String, Pair<Boolean, String>>() {
                                            @Override
                                            public Pair<Boolean, String> call(String value) throws Exception {
                                                return verifyLogin(verifyUrl, idToken);
                                            }
                                        }).subscribeOn(Schedulers.io()).observeOn(RxAndroid.main()).subscribe(new Observer<Pair<Boolean, String>>() {
                                            @Override
                                            public void onSuccess(Pair<Boolean, String> verifyResult) {
                                                isAuthenticated = verifyResult.first;
                                                String id = verifyResult.second;
                                                if (id != null) {
                                                    try {
                                                        userInfo = new JSONObject(userInfo).put("id", id).toString();
                                                    } catch (Exception e) {
                                                        ILog.e(TAG, "reset user id from server error:" + e.getMessage());
                                                    }
                                                }
                                                saveUserInfo(userInfo);
                                                TestManager.getInstance().addEvent("Google_Auth", "中台校验成功, 登录成功", EventState.NORMAL, userInfo);
                                                if (authCallback != null) {
                                                    authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, isAuthenticated, null, null);
                                                }
                                            }

                                            @Override
                                            public void onError(Throwable e) {
                                                TestManager.getInstance().addEvent("Google_Auth", "中台校验失败, 登录失败", EventState.ERROR, "");
                                                isAuthenticated = false;
                                                if (authCallback != null) {
                                                    authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, null);
                                                }
                                            }
                                        });
                                    }
                                }
                            } else {
                                if (reSign) {
                                    ILog.i(TAG, "no login history,start account sign in");
                                    TestManager.getInstance().addEvent("Google_Auth", "证书失效,尝试直接登录", EventState.NORMAL, "");
                                    doSignIn(false, authCallback);
                                } else {
                                    TestManager.getInstance().addEvent("Google_Auth", "Google登录失败", EventState.ERROR, "证书校验失败");
                                    isAuthenticated = false;
                                    if (authCallback != null) {
                                        authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, null);
                                    }
                                }
                            }
                        });
                    } catch (NoCredentialException e1) {
                        ILog.i(TAG, "no credential found,start account sign in:" + e1.getMessage());
                        if (filterByAuthorizedAccounts) {
                            TestManager.getInstance().addEvent("Google_Auth", "不存在Google登录证书,尝试直接登录", EventState.NORMAL, (e1 != null ? e1.getMessage() : ""));
                            doSignIn(false, authCallback);
                        } else {
                            ILog.i(TAG, "sign in exception:" + e1.getMessage());
                            TestManager.getInstance().addEvent("Google_Auth", "Google登录失败", EventState.ERROR, (e1 != null ? e1.getMessage() : ""));
                            isAuthenticated = false;
                            if (authCallback != null) {
                                authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, null);
                            }
                        }
                    } catch (Exception e) {
                        ILog.i(TAG, "sign in exception:" + e.getMessage());
                        TestManager.getInstance().addEvent("Google_Auth", "Google登录失败", EventState.ERROR, (e != null ? e.getMessage() : ""));
                        isAuthenticated = false;
                        if (authCallback != null) {
                            authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, null);
                        }
                    }
                }

                @Override
                public void onError(@NonNull GetCredentialException e) {
                    ILog.i(TAG, "sign in exception with GetCredentialException:" + e.getMessage());
                    if (filterByAuthorizedAccounts) {
                        TestManager.getInstance().addEvent("Google_Auth", "不存在Google登录证书,尝试直接登录", EventState.NORMAL, (e != null ? e.getMessage() : ""));
                        doSignIn(false, authCallback);
                    } else {
                        ILog.i(TAG, "sign in exception:" + e.getMessage());
                        TestManager.getInstance().addEvent("Google_Auth", "获取Google登录证书失败", EventState.ERROR, (e != null ? e.getMessage() : ""));
                        isAuthenticated = false;
                        if (authCallback != null) {
                            authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, null);
                        }
                    }

                }
            });
        } catch (Exception e) {
            ILog.i(TAG, "sign in exception:" + e.getMessage());
            TestManager.getInstance().addEvent("Google_Auth", "Google登录失败", EventState.NORMAL, (e != null ? e.getMessage() : ""));
            isAuthenticated = false;
            if (authCallback != null) {
                authCallback.onLoginResult(AuthPlatforms.LOGIN_PLATFORM_GOOGLE, false, null, null);
            }
        }
    }

    private void handleSignResult(GetCredentialResponse result, SignResultCallback callback) throws Exception {
        Credential credential = result.getCredential();
        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
                try {
                    GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(customCredential.getData());
                    callback.onResult(true, false, googleIdTokenCredential);
                } catch (Exception e) {
                    if (e.getMessage().contains("GoogleIdTokenParsing")) {
                        ILog.w(TAG, "Received an invalid google id token response:" + e.getMessage());
                        callback.onResult(false, true, null);
                        return;
                    }
                    ILog.w(TAG, "parsing google id response err:" + e.getMessage());
                    callback.onResult(false, false, null);
                }
            } else {
                ILog.w(TAG, "unknown custom credential type");
                callback.onResult(false, false, null);
            }
        } else {
            ILog.w(TAG, "unknown credential type");
            callback.onResult(false, false, null);
        }
    }

    private Pair<Boolean, String> verifyLogin(String url, String token) {
        try {
            JSONObject json = new JSONObject();
            json.put("is_encrypt", false);
            JSONObject data = new JSONObject();
            data.put("app_id", appId);
            data.put("token", token);
            json.put("data", data);
            OkHttpClient okHttpClient = HttpUtil.getInstance().getOkHttpClient();
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody postBody = RequestBody.create(mediaType, json.toString());
            Request request = new Request.Builder().url(url).post(postBody).build();
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                String str = response.body().string();
                String userId = new JSONObject(str).getString("data");
                if (userId != null && !userId.isEmpty()) {
                    return new Pair<>(true, userId);
                } else {
                    throw new Exception("user id empty");
                }
            } else {
                throw new Exception("response error");
            }
        } catch (Exception e) {
            ILog.e(TAG, "verify login failed:" + e.getMessage());
        }
        return new Pair<>(false, null);
    }

    private interface SignResultCallback {
        void onResult(boolean signResult, boolean reSign, GoogleIdTokenCredential credential);
    }

}
