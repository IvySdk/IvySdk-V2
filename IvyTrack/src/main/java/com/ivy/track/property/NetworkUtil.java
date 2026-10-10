package com.ivy.track.property;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;

public class NetworkUtil {

    private boolean isNetWorkChanged = true;
    private String currentNetworkType = "NULL";

    public void startListenNetworkState(Context mContext) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(Network network) {
                            currentNetworkType = getNetworkType(mContext);
                            isNetWorkChanged = true;
                            super.onAvailable(network);
                        }

                        @Override
                        public void onLost(Network network) {
                            currentNetworkType = "NULL";
                            super.onLost(network);
                        }
                    });
                }
            } else {
                NetworkStatusReceiver.ConnectivityListener callback = new NetworkStatusReceiver.ConnectivityListener() {
                    @Override
                    public void onChanged() {
                        currentNetworkType = getNetworkType(mContext);
                        isNetWorkChanged = true;
                    }
                };
                NetworkStatusReceiver receiver = new NetworkStatusReceiver();
                receiver.setListener(callback);
                mContext.registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            }
        } catch (Exception ignored) {
        }
    }

    public String getNetworkType(Context context) {
        if ((isNetWorkChanged && "NULL".equals(currentNetworkType))) {
            currentNetworkType = getCurrentNetworkType(context);
            if (!"NULL".equals(currentNetworkType)) {
                isNetWorkChanged = true;
            }
        }
        if (currentNetworkType.trim().isEmpty()) {
            return "offline";
        }
        return currentNetworkType;
    }

    public String getCurrentNetworkType(Context context) {
        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = null;
            if (manager != null) {
                networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                    return "WIFI";
                }
            }
            return mobileNetworkType(
                    context,
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE),
                    manager
            );
        } catch (Exception e) {
            return "NULL";
        }
    }

    @SuppressLint("MissingPermission")
    private String mobileNetworkType(Context context, TelephonyManager telephonyManager, ConnectivityManager connectivityManager) {
        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (telephonyManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && checkHasPermission(
                        context,
                        android.Manifest.permission.READ_PHONE_STATE
                )) {
                    networkType = telephonyManager.getDataNetworkType();
                } else {
                    networkType = telephonyManager.getNetworkType();
                }
            } catch (Exception ignored) {
            }
        }

        if (networkType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null) {
                    networkType = networkInfo.getSubtype();
                }
            }
        }

        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            case TelephonyManager.NETWORK_TYPE_GSM:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_IWLAN:
            case 19: // NETWORK_TYPE_LTE_CA
                return "4G";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
            default:
                return "NULL";
        }
    }

    private boolean checkHasPermission(Context context, String permission) {
        try {
            Class<?> contextCompat = null;
            try {
                contextCompat = Class.forName("android.support.v4.content.ContextCompat");
            } catch (Exception e) {
                // ignored
            }
            if (contextCompat == null) {
                try {
                    contextCompat = Class.forName("androidx.core.content.ContextCompat");
                } catch (Exception e) {
                    // ignored
                }
            }

            if (contextCompat == null) {
                return true;
            }

            Method checkSelfPermissionMethod = contextCompat.getMethod(
                    "checkSelfPermission",
                    Context.class, String.class
            );
            int result = (int) checkSelfPermissionMethod.invoke(null, context, permission);
            return result == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isAirplaneModeOn(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.System.getInt(
                    context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON,
                    0
            ) != 0;
        } else {
            return Settings.Global.getInt(
                    context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON,
                    0
            ) != 0;
        }
    }
}