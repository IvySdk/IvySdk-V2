package com.ivy.base.games.auth;

public interface IAuthResult extends IAuthResponse {
    void onLogout(String platform);

    void onPlatformsInitialized();
}