package com.ivy.core;

import com.android.client.INotificationEvent;
import com.android.client.IRemoteConfigSyncListener;
import com.ivy.base.ad.IAdListener;
import com.ivy.base.billing.IPurchaseResult;
import com.ivy.base.games.IPlayGameEvents;
import com.ivy.base.games.auth.IAuthResult;
import com.ivy.base.helper.IHelperCallback;

public class Builder {

    public IAdListener adListener;
    public IPurchaseResult purchaseResult;
    public IHelperCallback helperListener;
    public IAuthResult authResult;
    public INotificationEvent notificationEvent;
    public IRemoteConfigSyncListener remoteConfigSyncListener;

    public IPlayGameEvents playGameEvents;
    private Builder() {
    }

    public static class Build {
        private final Builder builder = new Builder();

        public Build setAdListener(IAdListener adListener) {
            builder.adListener = adListener;
            return this;
        }

        public Build setPurchaseListener(IPurchaseResult purchaseResult) {
            builder.purchaseResult = purchaseResult;
            return this;
        }

        public Build setHelperListener(IHelperCallback callback) {
            builder.helperListener = callback;
            return this;
        }

        public Build setAuthListener(IAuthResult callback) {
            builder.authResult = callback;
            return this;
        }

        public Build setNotificationEventListener(INotificationEvent callback) {
            builder.notificationEvent = callback;
            return this;
        }

        public Build setRemoteConfigSyncListener(IRemoteConfigSyncListener callback) {
            builder.remoteConfigSyncListener = callback;
            return this;
        }

        public Build setPlayGameEventsListener(IPlayGameEvents callback) {
            builder.playGameEvents = callback;
            return this;
        }

        public Builder build() {
            return builder;
        }
    }
}
