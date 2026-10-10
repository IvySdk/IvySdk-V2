package com.ivy.adstrategy;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

import com.ivy.adstrategy.device.DeviceClassifier;
import com.ivy.adstrategy.device.DeviceLevel;
import com.ivy.adstrategy.device.DeviceSnapshot;
import com.ivy.adstrategy.gate.PlatformInitializationGate;
import com.ivy.adstrategy.metrics.StrategyEvent;
import com.ivy.adstrategy.metrics.StrategyEventListener;
import com.ivy.adstrategy.runtime.MemoryPressureListener;
import com.ivy.adstrategy.runtime.RuntimeState;
import com.ivy.adstrategy.runtime.RuntimeStateMonitor;
import com.ivy.adstrategy.scheduler.AdAction;
import com.ivy.adstrategy.scheduler.AdActionExecutor;
import com.ivy.adstrategy.scheduler.AdAutoScheduler;
import com.ivy.adstrategy.scheduler.InventoryProvider;
import com.ivy.adstrategy.slot.AdSlot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdStrategy {
  private static volatile AdStrategy instance;

  private final AdStrategyConfig config;
  private final DeviceSnapshot deviceSnapshot;
  private final RuntimeStateMonitor runtimeStateMonitor;
  private StrategyEventListener eventListener;

  private AdStrategy(Context context, AdStrategyConfig config, StrategyEventListener eventListener) {
    this.config = config == null ? new AdStrategyConfig() : config;
    this.eventListener = eventListener;
    this.deviceSnapshot = new DeviceClassifier(context, this.config).snapshot();
    this.runtimeStateMonitor = new RuntimeStateMonitor(context, this.config);
    emitDeviceLevel();
  }

  public static synchronized AdStrategy init(Context context) {
    return init(context, new AdStrategyConfig(), null);
  }

  public static synchronized AdStrategy init(Context context, AdStrategyConfig config) {
    return init(context, config, null);
  }

  public static synchronized AdStrategy init(Context context, AdStrategyConfig config, StrategyEventListener eventListener) {
    if (context == null) {
      throw new IllegalArgumentException("context must not be null");
    }
    instance = new AdStrategy(context.getApplicationContext(), config, eventListener);
    return instance;
  }

  public static AdStrategy get() {
    if (instance == null) {
      throw new IllegalStateException("AdStrategy is not initialized. Call AdStrategy.init(context) first.");
    }
    return instance;
  }

  public static boolean isInitialized() {
    return instance != null;
  }

  public void setEventListener(StrategyEventListener eventListener) {
    this.eventListener = eventListener;
  }

  public DeviceSnapshot getDeviceSnapshot() {
    return deviceSnapshot;
  }

  public AdStrategyConfig getConfig() {
    return config;
  }

  public RuntimeState getRuntimeState() {
    return runtimeStateMonitor.snapshot();
  }

  public void onForeground() {
    runtimeStateMonitor.onForeground();
  }

  public void bindHostActivity(Activity activity) {
    runtimeStateMonitor.bindHostActivity(activity);
  }

  public void onBackground() {
    runtimeStateMonitor.onBackground();
  }

  public void onAdShowStarted() {
    runtimeStateMonitor.onAdShowStarted();
  }

  public void onAdShowFinished() {
    runtimeStateMonitor.onAdShowFinished();
  }

  public void setMemoryPressureListener(MemoryPressureListener listener) {
    runtimeStateMonitor.setMemoryPressureListener(listener);
  }

  public void markMemoryPressure(String reason) {
    runtimeStateMonitor.markMemoryPressure(reason);
  }

  public PlatformInitializationGate createInitializationGate(Handler backgroundHandler) {
    return new PlatformInitializationGate(this, backgroundHandler != null ? backgroundHandler : runtimeStateMonitor.getWatchdogHandler());
  }

  public AdAutoScheduler createScheduler(List<AdSlot> slots, InventoryProvider inventoryProvider, AdActionExecutor actionExecutor) {
    return new AdAutoScheduler(this, slots, inventoryProvider, actionExecutor);
  }

  public int targetReadyCount(AdType adType) {
    Boolean keepReady = config.getKeepReadyOverride(adType);
    if (keepReady != null) {
      return keepReady ? 1 : 0;
    }
    Integer override = config.getTargetReadyCountOverride(adType);
    if (override != null) {
      return override;
    }
    if (adType == AdType.BANNER || adType == AdType.NATIVE) {
      return 0;
    }
    return 1;
  }

  private void emitDeviceLevel() {
    Map<String, Object> params = new HashMap<>();
    params.put("device_level", deviceSnapshot.getLevel().name());
    params.put("total_memory_mb", deviceSnapshot.getTotalMemoryMb());
    params.put("cpu_cores", deviceSnapshot.getCpuCores());
    params.put("low_ram", deviceSnapshot.isLowRamDevice());
    emit(new StrategyEvent(StrategyEvent.DEVICE_LEVEL, null, params));
  }

  public void emitSchedulerAction(AdAction action, String trigger) {
    RuntimeState state = getRuntimeState();
    Map<String, Object> params = runtimeParams(state);
    params.put("action", action.getType().name());
    params.put("slot_id", action.getSlotId());
    params.put("reason", action.getReason());
    params.put("score", action.getScore());
    params.put("trigger", trigger);
    params.put("device_level", deviceSnapshot.getLevel().name());
    emit(new StrategyEvent(StrategyEvent.SCHEDULER_ACTION, action.getAdType(), params));
  }

  public void emitEvent(StrategyEvent event) {
    emit(event);
  }

  private Map<String, Object> runtimeParams(RuntimeState state) {
    Map<String, Object> params = new HashMap<>();
    params.put("launch_period", state.isLaunchPeriod());
    params.put("background", state.isBackground());
    params.put("memory_pressure", state.isMemoryPressure());
    params.put("critical_memory_pressure", state.isCriticalMemoryPressure());
    params.put("available_memory_mb", state.getAvailableMemoryMb());
    params.put("threshold_memory_mb", state.getThresholdMemoryMb());
    if (state.getCriticalMemoryReason() != null) {
      params.put("critical_memory_reason", state.getCriticalMemoryReason());
    }
    params.put("main_thread_busy", state.isMainThreadBusy());
    params.put("current_main_lag_ms", state.getCurrentMainLagMs());
    params.put("recent_max_lag_ms", state.getRecentMaxLagMs());
    params.put("recent_stall_count", state.getRecentSevereStallCount());
    params.put("ad_showing", state.isAdShowing());
    params.put("weak_network", state.isWeakNetwork());
    params.put("device_level", deviceSnapshot.getLevel().name());
    return params;
  }

  private void emit(StrategyEvent event) {
    StrategyEventListener listener = this.eventListener;
    if (listener != null) {
      listener.onStrategyEvent(event);
    }
  }
}
