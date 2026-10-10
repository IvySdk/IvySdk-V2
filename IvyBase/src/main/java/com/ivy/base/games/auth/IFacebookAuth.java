package com.ivy.base.games.auth;

public interface IFacebookAuth extends IBaseAuth {

    String getFriends();

    String requireAccessToken();
}