package com.ivy.appsflyer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;

import com.ivy.base.App;

import java.util.concurrent.atomic.AtomicReference;

public class NetworkMonitor {

    public static final String STATE_ONLINE = "online";
    public static final String STATE_OFFLINE = "offline";
    public static final String STATE_UNKNOWN = "unknown";

    private static final Object STATE_LOCK = new Object();
    private static String systemNetworkState = STATE_UNKNOWN;
    private static volatile String networkState = STATE_UNKNOWN;
    private static volatile long networkGeneration;
    private static Network currentNetwork;

    public interface OnNetworkAvailableListener {
        void onNetworkAvailable();
    }

    // 返回监听缓存的最新状态，避免每个事件都查询系统网络服务。
    public static String getNetworkState() {
        return networkState;
    }

    public static boolean isNetworkAvailable() {
        return STATE_ONLINE.equals(getNetworkState());
    }

    // unknown 时允许发送器做一次受控探测；明确离线时才阻止请求。
    public static boolean canAttemptConnection() {
        return !STATE_OFFLINE.equals(getNetworkState());
    }

    public static long getNetworkGeneration() {
        return networkGeneration;
    }

    // AppsFlyer 成功只能证明发起请求时的网络可达，不能覆盖之后的网络切换。
    public static void markAppsFlyerReachable(long generation) {
        synchronized (STATE_LOCK) {
            if (generation == networkGeneration && !STATE_OFFLINE.equals(systemNetworkState)) {
                networkState = STATE_ONLINE;
            }
        }
    }

    // 监听网络从“离线”恢复为可探测，或从“未验证”变为系统已验证。
    public static void registerNetworkListener(final OnNetworkAvailableListener listener) {
        Context context = App.getInstance().getApplicationContext();
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            updateSystemState(null, STATE_UNKNOWN);
            return;
        }

        refreshActiveNetwork(cm);
        final AtomicReference<String> listenerState = new AtomicReference<>(getNetworkState());
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                                              @NonNull NetworkCapabilities capabilities) {
                super.onCapabilitiesChanged(network, capabilities);
                refreshAndNotify(cm, listenerState, listener);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                refreshAndNotify(cm, listenerState, listener);
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(callback);
        } else {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            cm.registerNetworkCallback(request, callback);
        }
    }

    private static void refreshAndNotify(ConnectivityManager cm,
                                         AtomicReference<String> listenerState,
                                         OnNetworkAvailableListener listener) {
        refreshActiveNetwork(cm);
        String newState = getNetworkState();
        String oldState = listenerState.getAndSet(newState);
        boolean becameUsable = STATE_OFFLINE.equals(oldState) && !STATE_OFFLINE.equals(newState);
        boolean becameValidated = !STATE_ONLINE.equals(oldState) && STATE_ONLINE.equals(newState);
        if ((becameUsable || becameValidated) && listener != null) {
            listener.onNetworkAvailable();
        }
    }

    private static void refreshActiveNetwork(ConnectivityManager cm) {
        Network activeNetwork = cm.getActiveNetwork();
        if (activeNetwork == null) {
            updateSystemState(null, STATE_OFFLINE);
            return;
        }
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
        if (capabilities == null
                || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            updateSystemState(activeNetwork, STATE_OFFLINE);
        } else if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            updateSystemState(activeNetwork, STATE_ONLINE);
        } else {
            updateSystemState(activeNetwork, STATE_UNKNOWN);
        }
    }

    private static void updateSystemState(Network network, String newState) {
        synchronized (STATE_LOCK) {
            boolean networkChanged = currentNetwork == null
                    ? network != null : !currentNetwork.equals(network);
            if (networkChanged || !newState.equals(systemNetworkState)) {
                currentNetwork = network;
                systemNetworkState = newState;
                networkState = newState;
                networkGeneration++;
            }
        }
    }
}
