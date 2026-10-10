package com.ivy.base.games.auth;

import com.ivy.base.games.IGet;

import org.json.JSONObject;
import java.util.function.Supplier;

public interface IFirebaseAuth {

    /**
     * @param jsonObject 配置对象
     * @param debug 是否调试
     * @param getPlayGamesAuthCode 返回PlayGames服务端授权码
     * @param getFacebookAccessToken 返回Facebook AccessToken，可为null
     * @param getGoogleIdToken 返回Google IdToken
     */
    void setup(JSONObject jsonObject, boolean debug,
               IGet getPlayGamesAuthCode,
               IGet getFacebookAccessToken,
               IGet getGoogleIdToken);

    // 注释掉的signIn方法可以根据需求自己实现
    // void signIn(String channel, String email, String emailPassword);

    void logout();

    void reloadLastLoginStatus(IFirebaseAuthReload authReload);

    /**
     * @param channel 可传null
     */
    String getUserInfo(String channel);

    String getUserId();

    boolean isAnonymous();

    boolean isChannelLinked(String channel);

    boolean canChannelUnlink(String channel);

    void unlinkChannel(String channel, IFirebaseUnlink callback);

    void loginAnonymous(IAuthResponse authResult);

    void loginWithGoogle(IAuthResponse authResult, boolean reauth);

    void loginWithPlayGames(IAuthResponse authResult, boolean reauth);

    void loginWithFacebook(IAuthResponse authResult, boolean reauth);

    void loginWithEmailAndPassword(String email, String password, IAuthResponse authResult, boolean reauth);
}