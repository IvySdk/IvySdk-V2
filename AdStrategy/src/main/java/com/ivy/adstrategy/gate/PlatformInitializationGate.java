package com.ivy.adstrategy.gate;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.ivy.adstrategy.AdStrategy;
import com.ivy.adstrategy.AdStrategyConfig;
import com.ivy.adstrategy.metrics.StrategyEvent;
import com.ivy.adstrategy.runtime.RuntimeState;

import java.util.HashMap;
import java.util.Map;

public class PlatformInitializationGate {
  private static final String TAG = "hoho";

  public enum GateDecision {
    ALLOW,
    DEFER,
    DROP_THIS_SESSION
  }

  public interface Callback {
    void onAllow();
    void onDrop(String reason);
  }

  private final AdStrategy strategy;
  private final Handler mainHandler;
  private final Handler backgroundHandler;
  private final long startTimeMs;
  private volatile boolean resolved;

  public PlatformInitializationGate(AdStrategy strategy, Handler backgroundHandler) {
    this.strategy = strategy;
    this.mainHandler = new Handler(Looper.getMainLooper());
    this.backgroundHandler = backgroundHandler;
    this.startTimeMs = SystemClock.elapsedRealtime();
  }

  public void evaluateAndExecute(final Activity activity, final Callback callback) {
    if (resolved) {
      return;
    }
    if (backgroundHandler != null) {
      backgroundHandler.post(new Runnable() {
        @Override
        public void run() {
          checkGate(activity, callback);
        }
      });
    } else {
      checkGate(activity, callback);
    }
  }

  private void checkGate(final Activity activity, final Callback callback) {
    if (resolved) {
      return;
    }
    long elapsed = SystemClock.elapsedRealtime() - startTimeMs;
    AdStrategyConfig config = strategy.getConfig();
    RuntimeState state = strategy.getRuntimeState();

    if (state.isCriticalMemoryPressure()) {
      resolveDrop(callback, "critical_memory_pressure", state, elapsed);
      return;
    }

    if (elapsed > config.getGateMaxWaitMs()) {
      Log.w(TAG, "PlatformInitializationGate max wait timeout=" + elapsed + "ms, allow fail-safe");
      resolveAllow(callback, state, elapsed, "max_wait_timeout_fallback");
      return;
    }

    if (activity != null && activity.isFinishing()) {
      resolveDrop(callback, "activity_finishing", state, elapsed);
      return;
    }

    if (state.isBackground()) {
      defer(activity, callback, "app_background", state, elapsed);
      return;
    }

    if (state.isMainThreadBusy()) {
      defer(activity, callback, "main_thread_busy", state, elapsed);
      return;
    }

    if (state.getRecentSevereStallCount() > 0 && state.getTimeSinceLastSevereStallMs() < 3000L) {
      defer(activity, callback, "recent_severe_stall", state, elapsed);
      return;
    }

    // Passed all safe-window checks
    resolveAllow(callback, state, elapsed, "safe_window_confirmed");
  }

  private void defer(final Activity activity, final Callback callback,
                     String reason, RuntimeState state, long elapsed) {
    if (resolved) {
      return;
    }
    emitGateEvent("DEFER", reason, state, elapsed);
    Log.d(TAG, "PlatformInitializationGate DEFER reason=" + reason
        + ", elapsed=" + elapsed + "ms, mainLag=" + state.getCurrentMainLagMs() + "ms");

    long delay = strategy.getConfig().getGateCheckIntervalMs();
    if (backgroundHandler != null) {
      backgroundHandler.postDelayed(new Runnable() {
        @Override
        public void run() {
          checkGate(activity, callback);
        }
      }, delay);
    } else {
      mainHandler.postDelayed(new Runnable() {
        @Override
        public void run() {
          checkGate(activity, callback);
        }
      }, delay);
    }
  }

  private void resolveAllow(final Callback callback, RuntimeState state, long elapsed, String reason) {
    if (resolved) {
      return;
    }
    resolved = true;
    emitGateEvent("ALLOW", reason, state, elapsed);
    Log.i(TAG, "PlatformInitializationGate ALLOW reason=" + reason + ", elapsed=" + elapsed + "ms");
    mainHandler.post(new Runnable() {
      @Override
      public void run() {
        if (callback != null) {
          callback.onAllow();
        }
      }
    });
  }

  private void resolveDrop(final Callback callback, final String reason, RuntimeState state, long elapsed) {
    if (resolved) {
      return;
    }
    resolved = true;
    emitGateEvent("DROP", reason, state, elapsed);
    Log.w(TAG, "PlatformInitializationGate DROP reason=" + reason + ", elapsed=" + elapsed + "ms");
    mainHandler.post(new Runnable() {
      @Override
      public void run() {
        if (callback != null) {
          callback.onDrop(reason);
        }
      }
    });
  }

  private void emitGateEvent(String decision, String reason, RuntimeState state, long elapsed) {
    Map<String, Object> params = new HashMap<>();
    params.put("gate_decision", decision);
    params.put("gate_reason", reason);
    params.put("elapsed_ms", elapsed);
    params.put("main_lag_ms", state.getCurrentMainLagMs());
    params.put("recent_max_lag_ms", state.getRecentMaxLagMs());
    params.put("recent_stall_count", state.getRecentSevereStallCount());
    params.put("background", state.isBackground());
    params.put("memory_pressure", state.isMemoryPressure());
    params.put("critical_memory", state.isCriticalMemoryPressure());
    strategy.emitEvent(new StrategyEvent("sdk_ads_init_gate", null, params));
  }
}
