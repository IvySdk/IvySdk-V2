package com.ivy.base.track;

import android.content.Context;

public abstract class AbsTrack implements ITrack {

    protected boolean enableStatus = true;
    protected boolean enableAdPing = true;
    protected boolean enablePurchasePing = true;
    protected Context context;

    protected boolean debug = false;

    @Override
    public void setup(Context context, String appId, String config, boolean debug, IConversationCallback conversationCallback, Runnable readyListener) {
        this.context = context;
        this.debug = debug;
    }

    @Override
    public void appsflyerInviteUser(String channel, String campaign, String inviterId, String inviterAppId) {
        // 空实现
    }

    @Override
    public String getAppsflyerInviterId() {
        return null;
    }

    @Override
    public String getAppsflyerId() {
        return null;
    }
}