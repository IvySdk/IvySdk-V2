package com.ivy.track.property;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

public class NetworkStatusReceiver extends BroadcastReceiver {

    public interface ConnectivityListener {
        void onChanged();
    }

    private ConnectivityListener listener;

    public void setListener(ConnectivityListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            if (listener != null) {
                listener.onChanged();
            }
        }
    }
}