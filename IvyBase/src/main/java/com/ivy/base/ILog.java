package com.ivy.base;

import android.util.Log;

public class ILog {

    public static final String ITag = "ITAG";

    /**
     * level 对应系统log级别
     * Log.VERBOSE     2
     * Log.DEBUG       3
     * Log.INFO        4
     * Log.WARN        5
     * Log.ERROR       6
     * Log.ASSERT      7(no use)
     * Log.NONE        8
     */
    public static volatile boolean isDebug = false;

    public static void v(String tag, String... msg) {
        if (isDebug) {
            Log.v(formatTag(tag), formatMessage(msg));
        }
    }

    public static void d(String tag, String... msg) {
        if (isDebug) {
            Log.d(formatTag(tag), formatMessage(msg));
        }
    }

    public static void i(String tag, String... msg) {
        if (isDebug) {
            Log.i(formatTag(tag), formatMessage(msg));
        }
    }

    public static void w(String tag, String... msg) {
        if (isDebug) {
            Log.w(formatTag(tag), formatMessage(msg));
        }
    }

    public static void e(String tag, String... msg) {
        if (isDebug) {
            Log.e(formatTag(tag), formatMessage(msg));
        }
    }

    private static String formatTag(String tag) {
        return ITag + "_" + (tag != null ? tag : "");
    }

    private static String formatMessage(String... msg) {
        if (msg == null || msg.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < msg.length; i++) {
            sb.append(msg[i]);
            if (i < msg.length - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }
}
