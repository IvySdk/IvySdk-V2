package com.ivy.adstrategy.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.ivy.adstrategy.AdStrategyConfig;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class RuntimeStateMonitor {
  private static final String TAG = "hoho";
  private static final long BACKGROUND_CONFIRM_DELAY_MS = 1200L;
  private static final long SEVERE_STALL_THRESHOLD_MS = 2000L;
  private static final long METRICS_DECAY_INTERVAL_MS = 10000L;

  private final Context context;
  private final AdStrategyConfig config;
  private final long startElapsedMs;
  private final ComponentCallbacks2 componentCallbacks;
  private final Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;
  private final Handler mainHandler;
  private final Runnable backgroundConfirmRunnable;

  // Background watchdog
  private final HandlerThread watchdogThread;
  private final Handler watchdogHandler;
  private volatile boolean pingInFlight;
  private volatile long pingSentTimeMs;
  private volatile long currentMainLagMs;
  private volatile long recentMaxLagMs;
  private volatile int recentSevereStallCount;
  private volatile long lastSevereStallMs;
  private volatile long lastDecayTimeMs;

  private volatile boolean background;
  private volatile long lastMemoryPressureSignalMs;
  private volatile boolean criticalMemoryPressure;
  private volatile long availableMemoryMb = -1L;
  private volatile long thresholdMemoryMb = -1L;
  private volatile String criticalMemoryReason;
  private volatile int activeAdShowingCount;
  private volatile long adShowingStartedMs;
  private volatile long lastAdShowFinishedMs;
  private final Set<Activity> startedActivities =
      Collections.newSetFromMap(new IdentityHashMap<Activity, Boolean>());
  private volatile MemoryPressureListener memoryPressureListener;

  private final Runnable mainThreadPingReplyRunnable = new Runnable() {
    @Override
    public void run() {
      long replyNow = SystemClock.elapsedRealtime();
      long lag = replyNow - pingSentTimeMs;
      currentMainLagMs = lag;
      if (lag > recentMaxLagMs) {
        recentMaxLagMs = lag;
      }
      if (lag >= SEVERE_STALL_THRESHOLD_MS) {
        lastSevereStallMs = replyNow;
        recentSevereStallCount++;
      }
      pingInFlight = false;
    }
  };

  private final Runnable watchdogPingRunnable = new Runnable() {
    @Override
    public void run() {
      long now = SystemClock.elapsedRealtime();
      if (pingInFlight) {
        long lag = now - pingSentTimeMs;
        currentMainLagMs = lag;
        if (lag > recentMaxLagMs) {
          recentMaxLagMs = lag;
        }
        if (lag >= SEVERE_STALL_THRESHOLD_MS) {
          lastSevereStallMs = now;
          recentSevereStallCount++;
        }
      } else {
        pingInFlight = true;
        pingSentTimeMs = now;
        if (mainHandler != null) {
          mainHandler.post(mainThreadPingReplyRunnable);
        } else {
          pingInFlight = false;
        }
      }

      if (now - lastDecayTimeMs >= METRICS_DECAY_INTERVAL_MS) {
        lastDecayTimeMs = now;
        if (now - lastSevereStallMs >= METRICS_DECAY_INTERVAL_MS) {
          recentSevereStallCount = 0;
          recentMaxLagMs = currentMainLagMs;
        }
      }

      if (watchdogHandler != null) {
        watchdogHandler.postDelayed(this, config.getMainThreadBeatIntervalMs());
      }
    }
  };

  public RuntimeStateMonitor(Context context, AdStrategyConfig config) {
    this.context = context.getApplicationContext();
    this.config = config;
    this.startElapsedMs = SystemClock.elapsedRealtime();
    this.lastDecayTimeMs = this.startElapsedMs;
    this.mainHandler = Looper.getMainLooper() == null ? null : new Handler(Looper.getMainLooper());
    this.backgroundConfirmRunnable = new Runnable() {
      @Override
      public void run() {
        if (getStartedActivityCount() <= 0) {
          setBackground(true, "activity_stopped_confirmed");
        }
      }
    };
    this.componentCallbacks = createComponentCallbacks();
    this.context.registerComponentCallbacks(componentCallbacks);
    this.activityLifecycleCallbacks = createActivityLifecycleCallbacks();
    registerActivityLifecycleCallbacks();

    this.watchdogThread = new HandlerThread("MainThreadWatchdog");
    this.watchdogThread.start();
    this.watchdogHandler = new Handler(this.watchdogThread.getLooper());
    this.watchdogHandler.post(watchdogPingRunnable);
  }

  public Handler getWatchdogHandler() {
    return watchdogHandler;
  }

  public void setMemoryPressureListener(MemoryPressureListener memoryPressureListener) {
    this.memoryPressureListener = memoryPressureListener;
  }

  public void markMemoryPressure(String reason) {
    lastMemoryPressureSignalMs = SystemClock.elapsedRealtime();
    Log.d(TAG, "RuntimeStateMonitor memory pressure reason=" + reason);
  }

  public void onForeground() {
    setBackground(false, "manual_foreground");
  }

  public void bindHostActivity(Activity activity) {
    if (activity == null) {
      return;
    }
    synchronized (startedActivities) {
      startedActivities.add(activity);
    }
    cancelBackgroundConfirm();
    setBackground(false, "bind_host:" + activity.getClass().getSimpleName());
    Log.d(TAG, "RuntimeStateMonitor bind host="
        + activity.getClass().getName()
        + ", startedCount=" + getStartedActivityCount());
  }

  public void onBackground() {
    setBackground(true, "manual_background");
  }

  public void onAdShowStarted() {
    if (activeAdShowingCount <= 0) {
      adShowingStartedMs = SystemClock.elapsedRealtime();
    }
    activeAdShowingCount++;
  }

  public void onAdShowFinished() {
    if (activeAdShowingCount > 0) {
      activeAdShowingCount--;
    }
    if (activeAdShowingCount <= 0) {
      activeAdShowingCount = 0;
      adShowingStartedMs = 0L;
    }
    lastAdShowFinishedMs = SystemClock.elapsedRealtime();
  }

  public RuntimeState snapshot() {
    long now = SystemClock.elapsedRealtime();
    boolean adShowing = isAdShowing(now);
    boolean memoryPressure = isMemoryPressure(now);
    boolean mainThreadBusy = !adShowing && (currentMainLagMs > config.getMainThreadBusyThresholdMs());
    long timeSinceLastSevereStall = lastSevereStallMs > 0L ? (now - lastSevereStallMs) : Long.MAX_VALUE;

    return new RuntimeState(
        now - startElapsedMs <= config.getLaunchPeriodMs(),
        background,
        memoryPressure,
        criticalMemoryPressure,
        availableMemoryMb,
        thresholdMemoryMb,
        criticalMemoryReason,
        mainThreadBusy,
        adShowing,
        isWeakNetwork(),
        currentMainLagMs,
        recentMaxLagMs,
        recentSevereStallCount,
        timeSinceLastSevereStall
    );
  }

  public boolean isSafeForHeavyOperation() {
    if (background || criticalMemoryPressure) {
      return false;
    }
    long now = SystemClock.elapsedRealtime();
    if (isAdShowing(now)) {
      return false;
    }
    if (currentMainLagMs > config.getGateMaxMainLagMs()) {
      return false;
    }
    if (lastSevereStallMs > 0L && (now - lastSevereStallMs) < 3000L) {
      return false;
    }
    return true;
  }

  private boolean isAdShowing(long now) {
    if (activeAdShowingCount > 0) {
      if (config.getMaxAdShowingSuppressMs() > 0L
          && adShowingStartedMs > 0L
          && now - adShowingStartedMs > config.getMaxAdShowingSuppressMs()) {
        Log.w(TAG, "RuntimeStateMonitor clear stale ad showing state after timeout="
            + config.getMaxAdShowingSuppressMs());
        activeAdShowingCount = 0;
        adShowingStartedMs = 0L;
        lastAdShowFinishedMs = now;
      } else {
        return true;
      }
    }
    return lastAdShowFinishedMs > 0L
        && now - lastAdShowFinishedMs <= config.getShowFinishedRefillDelayMs();
  }

  private boolean isMemoryPressure(long now) {
    boolean recentPressureSignal = lastMemoryPressureSignalMs > 0L
        && now - lastMemoryPressureSignalMs <= config.getMemoryPressureSignalTtlMs();
    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    if (activityManager == null) {
      return criticalMemoryPressure || recentPressureSignal;
    }
    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
    activityManager.getMemoryInfo(memoryInfo);
    availableMemoryMb = memoryInfo.availMem / (1024L * 1024L);
    thresholdMemoryMb = memoryInfo.threshold / (1024L * 1024L);
    long criticalMemoryMb = config.getCriticalAvailableMemoryMb();
    if (!criticalMemoryPressure) {
      if (memoryInfo.lowMemory) {
        latchCriticalMemoryPressure("system_low_memory", now);
      } else if (criticalMemoryMb > 0L
          && memoryInfo.availMem <= criticalMemoryMb * 1024L * 1024L) {
        latchCriticalMemoryPressure("available_memory_below_limit", now);
      } else if (memoryInfo.threshold > 0L && memoryInfo.availMem <= memoryInfo.threshold) {
        latchCriticalMemoryPressure("available_memory_below_system_threshold", now);
      }
    }
    return criticalMemoryPressure || recentPressureSignal || memoryInfo.lowMemory;
  }

  private void latchCriticalMemoryPressure(String reason, long now) {
    if (criticalMemoryPressure) {
      return;
    }
    criticalMemoryPressure = true;
    criticalMemoryReason = reason;
    lastMemoryPressureSignalMs = now;
    Log.w(TAG, "RuntimeStateMonitor critical memory latched, reason=" + reason
        + ", availableMb=" + availableMemoryMb
        + ", thresholdMb=" + thresholdMemoryMb
        + ", limitMb=" + config.getCriticalAvailableMemoryMb());
  }

  private ComponentCallbacks2 createComponentCallbacks() {
    return new ComponentCallbacks2() {
      @Override
      public void onTrimMemory(int level) {
        if (isRunningMemoryPressure(level)) {
          notifyMemoryPressure("trim_memory_" + level);
        } else if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
          Log.d(TAG, "RuntimeStateMonitor trim ui hidden");
        }
      }

      @Override
      public void onLowMemory() {
        latchCriticalMemoryPressure("on_low_memory", SystemClock.elapsedRealtime());
        notifyMemoryPressure("low_memory");
      }

      @Override
      public void onConfigurationChanged(Configuration newConfig) {
      }
    };
  }

  private Application.ActivityLifecycleCallbacks createActivityLifecycleCallbacks() {
    return new Application.ActivityLifecycleCallbacks() {
      @Override
      public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
      }

      @Override
      public void onActivityStarted(Activity activity) {
        synchronized (startedActivities) {
          startedActivities.add(activity);
        }
        cancelBackgroundConfirm();
        setBackground(false, "activity_started:" + activity.getClass().getSimpleName());
        Log.d(TAG, "RuntimeStateMonitor activity started="
            + activity.getClass().getName()
            + ", startedCount=" + getStartedActivityCount());
      }

      @Override
      public void onActivityResumed(Activity activity) {
        cancelBackgroundConfirm();
        setBackground(false, "activity_resumed:" + activity.getClass().getSimpleName());
      }

      @Override
      public void onActivityPaused(Activity activity) {
      }

      @Override
      public void onActivityStopped(Activity activity) {
        boolean removed;
        synchronized (startedActivities) {
          removed = startedActivities.remove(activity);
        }
        Log.d(TAG, "RuntimeStateMonitor activity stopped="
            + activity.getClass().getName()
            + ", startedCount=" + getStartedActivityCount()
            + ", tracked=" + removed);
        if (getStartedActivityCount() <= 0) {
          scheduleBackgroundConfirm();
        }
      }

      @Override
      public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
      }

      @Override
      public void onActivityDestroyed(Activity activity) {
      }
    };
  }

  private void registerActivityLifecycleCallbacks() {
    if (context instanceof Application) {
      ((Application) context).registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
      return;
    }
    Log.w(TAG, "RuntimeStateMonitor context is not Application, background lifecycle callbacks unavailable");
  }

  private boolean isRunningMemoryPressure(int level) {
    return level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
        || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL;
  }

  private void notifyMemoryPressure(String reason) {
    markMemoryPressure(reason);
    MemoryPressureListener listener = memoryPressureListener;
    if (listener != null) {
      listener.onMemoryPressure(reason);
    }
  }

  private boolean isWeakNetwork() {
    try {
      ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      if (connectivityManager == null) {
        return false;
      }
      NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
      return networkInfo == null || !networkInfo.isConnected();
    } catch (Throwable ignored) {
      return false;
    }
  }

  private void scheduleBackgroundConfirm() {
    if (mainHandler == null) {
      setBackground(true, "activity_stopped_no_handler");
      return;
    }
    mainHandler.removeCallbacks(backgroundConfirmRunnable);
    mainHandler.postDelayed(backgroundConfirmRunnable, BACKGROUND_CONFIRM_DELAY_MS);
  }

  private void cancelBackgroundConfirm() {
    if (mainHandler != null) {
      mainHandler.removeCallbacks(backgroundConfirmRunnable);
    }
  }

  private void setBackground(boolean newValue, String reason) {
    if (background == newValue) {
      return;
    }
    background = newValue;
    Log.d(TAG, "RuntimeStateMonitor background=" + newValue + ", reason=" + reason
        + ", startedCount=" + getStartedActivityCount());
  }

  private int getStartedActivityCount() {
    synchronized (startedActivities) {
      return startedActivities.size();
    }
  }
}
