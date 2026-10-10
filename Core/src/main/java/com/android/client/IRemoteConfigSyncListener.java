package com.android.client;

public interface IRemoteConfigSyncListener {

    void onRemoteConfigSynced();

    void onAbTestConfigsSynced();

}
