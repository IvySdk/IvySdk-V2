package com.ivy.base.games.auth;

public final class AuthPlatforms {

    // google 账号登录
    public static final String LOGIN_PLATFORM_GOOGLE = "google";

    // google play games 游戏服务
    public static final String LOGIN_PLATFORM_PLAY_GAMES = "play_games";

    // facebook 登录
    public static final String LOGIN_PLATFORM_FACEBOOK = "facebook";

    // firestore 登录
    public static final String LOGIN_PLATFORM_FIREBASE = "firebase";

    private AuthPlatforms() {
        // 私有构造，防止实例化
    }
}
