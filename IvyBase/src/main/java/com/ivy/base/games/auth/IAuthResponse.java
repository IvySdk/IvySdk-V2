package com.ivy.base.games.auth;

public interface IAuthResponse {
    void onLoginResult(String platform, boolean status, String channel, String reason);
}