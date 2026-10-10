package com.ivy.track.property;

import android.os.Build;

public final class AndroidVersionUtils {

    private AndroidVersionUtils() {}

    public static String getAndroidMajorVersion() {
        int sdk = Build.VERSION.SDK_INT;
        if (sdk >= 36) return "15+";
        if (sdk >= 35) return "15";
        if (sdk >= 34) return "14";
        if (sdk >= 33) return "13";
        if (sdk >= 31) return "12";
        if (sdk >= 30) return "11";
        if (sdk >= 29) return "10";
        if (sdk >= 28) return "9";
        if (sdk >= 26) return "8";
        if (sdk >= 24) return "7";
        if (sdk >= 23) return "6";
        if (sdk >= 21) return "5";
        return "<5";
    }

    public static String getAndroidVersionName() {
        int sdk = Build.VERSION.SDK_INT;

        switch (sdk) {
            case 35: return "Android 15";
            case 34: return "Android 14";
            case 33: return "Android 13";
            case 32: return "Android 12L";
            case 31: return "Android 12";
            case 30: return "Android 11";
            case 29: return "Android 10";
            case 28: return "Android 9";
            case 27: return "Android 8.1";
            case 26: return "Android 8.0";
            case 25: return "Android 7.1";
            case 24: return "Android 7.0";
            case 23: return "Android 6.0";
            case 22: return "Android 5.1";
            case 21: return "Android 5.0";
            default:
                if (sdk > 35) return "Android " + sdk + " (Future)";
                return "Android <5";
        }
    }

}
