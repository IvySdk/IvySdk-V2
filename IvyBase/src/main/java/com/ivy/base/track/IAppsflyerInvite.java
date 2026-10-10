package com.ivy.base.track;

public interface IAppsflyerInvite {
    void appsflyerInviteUser(String channel, String campaign, String inviterId, String inviterAppId);

    String getAppsflyerInviterId();

    String getAppsflyerId();
}