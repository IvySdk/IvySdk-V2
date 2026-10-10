package com.ivy.base.track;

import android.content.Context;

public interface ITrack extends IAppsflyerInvite, IEvent {

    void setup(Context context, String appId, String config, boolean debug, IConversationCallback conversationCallback, Runnable readyListener);

    void setUserProperty(String key, Object value);
}