package com.ivy.base.games.auth;

import com.ivy.base.Callback;
import com.ivy.base.util.rx.Action;

public interface IBaseAuth {

    void setup(String appId, org.json.JSONObject jsonObject, boolean debug, IAuthResult authResult);

    void autoLogin();

    void loadLoginStatus();

    void login(IAuthResponse authCallback); // Kotlin 默认参数去掉，Java 只能重载

    void logout();

    void logout(Callback callback);

    boolean isTokenValid();

    boolean getLoginStatus();

    String getUserInfo();

    String getUserId();

    void onActivityResult(int requestCode, int resultCode, android.content.Intent data);

}