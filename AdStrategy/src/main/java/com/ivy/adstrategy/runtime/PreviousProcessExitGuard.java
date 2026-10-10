package com.ivy.adstrategy.runtime;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.system.OsConstants;
import android.util.Log;

import java.util.List;

public final class PreviousProcessExitGuard {
  private static final String TAG = "hoho";
  private static final String PREFS_NAME = "ivy_ad_strategy";
  private static final String KEY_CONSUMED_EXIT_TIMESTAMP = "consumed_abnormal_exit_timestamp";

  private PreviousProcessExitGuard() {
  }

  public static Decision checkAndConsume(Context context, long maxAgeMs) {
    if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      return Decision.noMatch();
    }
    try {
      ActivityManager activityManager =
          (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
      if (activityManager == null) {
        return Decision.noMatch();
      }
      List<ApplicationExitInfo> exitInfos = activityManager.getHistoricalProcessExitReasons(
          context.getPackageName(), 0, 10);
      ApplicationExitInfo latest = findLatestMainProcessExit(exitInfos, context.getPackageName());
      if (latest == null || !isRecent(latest.getTimestamp(), maxAgeMs)) {
        return Decision.noMatch();
      }
      SharedPreferences preferences =
          context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
      long consumedTimestamp = preferences.getLong(KEY_CONSUMED_EXIT_TIMESTAMP, 0L);
      if (latest.getTimestamp() <= consumedTimestamp || !isUserVisible(latest)) {
        return Decision.noMatch();
      }
      String reason = abnormalReason(latest);
      if (reason == null) {
        return Decision.noMatch();
      }
      reason = reason + "_" + latest.getImportance() + "_" + latest.getStatus();
      preferences.edit()
          .putLong(KEY_CONSUMED_EXIT_TIMESTAMP, latest.getTimestamp())
          .commit();
      Log.w(TAG, "PreviousProcessExitGuard matched abnormal exit, reason=" + reason
          + ", timestamp=" + latest.getTimestamp()
          + ", importance=" + latest.getImportance()
          + ", status=" + latest.getStatus());
      return Decision.matched(reason, latest.getTimestamp(), latest.getImportance());
    } catch (Throwable throwable) {
      Log.w(TAG, "PreviousProcessExitGuard check failed", throwable);
      return Decision.noMatch();
    }
  }

  private static ApplicationExitInfo findLatestMainProcessExit(
      List<ApplicationExitInfo> exitInfos, String packageName) {
    if (exitInfos == null) {
      return null;
    }
    for (ApplicationExitInfo exitInfo : exitInfos) {
      if (exitInfo != null && packageName.equals(exitInfo.getProcessName())) {
        return exitInfo;
      }
    }
    return null;
  }

  private static boolean isRecent(long timestamp, long maxAgeMs) {
    if (timestamp <= 0L) {
      return false;
    }
    if (maxAgeMs <= 0L) {
      return true;
    }
    long ageMs = System.currentTimeMillis() - timestamp;
    return ageMs >= 0L && ageMs <= maxAgeMs;
  }

  private static boolean isUserVisible(ApplicationExitInfo exitInfo) {
    return exitInfo.getImportance()
        <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
  }

  private static String abnormalReason(ApplicationExitInfo exitInfo) {
    switch (exitInfo.getReason()) {
      case ApplicationExitInfo.REASON_ANR:
        return "anr";
      case ApplicationExitInfo.REASON_LOW_MEMORY:
        return "low_memory";
      case ApplicationExitInfo.REASON_CRASH:
        return "java_crash";
      case ApplicationExitInfo.REASON_CRASH_NATIVE:
        return "native_crash";
      case ApplicationExitInfo.REASON_INITIALIZATION_FAILURE:
        return "initialization_failure";
      case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE:
        return "excessive_resource_usage";
      case ApplicationExitInfo.REASON_SIGNALED:
        if (!ActivityManager.isLowMemoryKillReportSupported()
            && exitInfo.getStatus() == OsConstants.SIGKILL) {
          return "possible_low_memory_sigkill";
        }
        return null;
      default:
        return null;
    }
  }

  public static final class Decision {
    private static final Decision NO_MATCH = new Decision(false, null, 0L, 0);
    private final boolean abnormalExit;
    private final String reason;
    private final long exitTimestamp;
    private final int importance;

    private Decision(boolean abnormalExit, String reason, long exitTimestamp, int importance) {
      this.abnormalExit = abnormalExit;
      this.reason = reason;
      this.exitTimestamp = exitTimestamp;
      this.importance = importance;
    }

    private static Decision noMatch() {
      return NO_MATCH;
    }

    private static Decision matched(String reason, long exitTimestamp, int importance) {
      return new Decision(true, reason, exitTimestamp, importance);
    }

    public boolean hasAbnormalExit() {
      return abnormalExit;
    }

    public String getReason() {
      return reason;
    }

    public long getExitTimestamp() {
      return exitTimestamp;
    }

    public int getImportance() {
      return importance;
    }
  }
}
