package com.ivy.adstrategy.scheduler;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.ivy.adstrategy.AdStrategy;
import com.ivy.adstrategy.event.AdEvent;
import com.ivy.adstrategy.event.AdEventType;
import com.ivy.adstrategy.policy.BackgroundPolicy;
import com.ivy.adstrategy.policy.ExperiencePolicy;
import com.ivy.adstrategy.policy.InventoryPolicy;
import com.ivy.adstrategy.policy.MemoryPolicy;
import com.ivy.adstrategy.policy.PolicyChain;
import com.ivy.adstrategy.policy.PolicyContext;
import com.ivy.adstrategy.policy.PolicyDecision;
import com.ivy.adstrategy.policy.PolicyDecisionType;
import com.ivy.adstrategy.policy.RetryPolicy;
import com.ivy.adstrategy.policy.SlotLifecyclePolicy;
import com.ivy.adstrategy.runtime.MemoryPressureListener;
import com.ivy.adstrategy.runtime.RuntimeState;
import com.ivy.adstrategy.slot.AdSlot;
import com.ivy.adstrategy.slot.AdSlotState;
import com.ivy.adstrategy.slot.SlotRegistry;
import com.ivy.adstrategy.slot.SlotStateMachine;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.EventState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdAutoScheduler implements MemoryPressureListener {
  private static final String TAG = "hoho";
  private static final String TEST_TAG = "AdScheduler";
  private final AdStrategy strategy;
  private final SlotRegistry slotRegistry;
  private final InventoryProvider inventoryProvider;
  private final AdActionExecutor actionExecutor;
  private final PolicyChain policyChain;
  private HandlerThread handlerThread;
  private Handler handler;
  private volatile boolean started;

  private final Runnable tickRunnable = new Runnable() {
    @Override
    public void run() {
      tick("timer");
      scheduleNextTick();
    }
  };

  public AdAutoScheduler(AdStrategy strategy, List<AdSlot> slots, InventoryProvider inventoryProvider, AdActionExecutor actionExecutor) {
    if (strategy == null) {
      throw new IllegalArgumentException("strategy must not be null");
    }
    if (inventoryProvider == null) {
      throw new IllegalArgumentException("inventoryProvider must not be null");
    }
    if (actionExecutor == null) {
      throw new IllegalArgumentException("actionExecutor must not be null");
    }
    this.strategy = strategy;
    this.inventoryProvider = inventoryProvider;
    this.actionExecutor = actionExecutor;
    this.slotRegistry = new SlotRegistry();
    this.slotRegistry.registerAll(slots);
    this.policyChain = new PolicyChain()
        .add(new SlotLifecyclePolicy())
        .add(new MemoryPolicy())
        .add(new BackgroundPolicy())
        .add(new ExperiencePolicy())
        .add(new RetryPolicy())
        .add(new InventoryPolicy());
  }

  public synchronized void start() {
    if (started) {
      return;
    }
    Log.d(TAG, "AdAutoScheduler start slots=" + slotRegistry.getAll().size());
    handlerThread = new HandlerThread("AdAutoScheduler");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
    started = true;
    strategy.setMemoryPressureListener(this);
    postEvent(new AdEvent(AdEventType.TICK));
    scheduleNextTick();
  }

  public synchronized void stop() {
    Log.d(TAG, "AdAutoScheduler stop");
    started = false;
    strategy.setMemoryPressureListener(null);
    if (handler != null) {
      handler.removeCallbacksAndMessages(null);
    }
    if (handlerThread != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        handlerThread.quitSafely();
      } else {
        handlerThread.quit();
      }
    }
    handler = null;
    handlerThread = null;
  }

  public void onEvent(final AdEvent event) {
    postEvent(event);
  }

  public void onForeground() {
    strategy.onForeground();
    postEvent(new AdEvent(AdEventType.APP_FOREGROUND));
  }

  public void onBackground() {
    strategy.onBackground();
    postEvent(new AdEvent(AdEventType.APP_BACKGROUND));
  }

  public void onMemoryPressure() {
    strategy.markMemoryPressure("manual_memory_pressure");
    postEvent(new AdEvent(AdEventType.MEMORY_PRESSURE));
  }

  @Override
  public void onMemoryPressure(String reason) {
    Log.d(TAG, "AdAutoScheduler memory pressure callback reason=" + reason);
    postEvent(new AdEvent(AdEventType.MEMORY_PRESSURE, null, reason));
  }

  public void releaseAll(final String reason) {
    if (!started || handler == null) {
      TestManager.getInstance().addEvent(TEST_TAG,
          "释放全部：已忽略（调度器未启动）",
          EventState.WARNING,
          "reason=" + reason);
      return;
    }
    handler.post(new Runnable() {
      @Override
      public void run() {
        releaseAllNow(reason == null ? "manual_release_all" : reason);
      }
    });
  }

  public void onLoadStarted(String slotId) {
    onLoadStarted(slotId, null);
  }

  public void onLoadStarted(String slotId, String adUnit) {
    postEvent(new AdEvent(AdEventType.LOAD_STARTED, slotId, adUnit, null));
  }

  public void onLoadSuccess(String slotId) {
    onLoadSuccess(slotId, null);
  }

  public void onLoadSuccess(String slotId, String adUnit) {
    postEvent(new AdEvent(AdEventType.LOAD_SUCCESS, slotId, adUnit, null));
  }

  public void onLoadFailed(String slotId, String reason) {
    onLoadFailed(slotId, null, reason);
  }

  public void onLoadFailed(String slotId, String adUnit, String reason) {
    postEvent(new AdEvent(AdEventType.LOAD_FAILED, slotId, adUnit, reason));
  }

  public void onShowStarted(String slotId) {
    strategy.onAdShowStarted();
    postEvent(new AdEvent(AdEventType.SHOW_STARTED, slotId));
  }

  public void onShowFinished(String slotId) {
    strategy.onAdShowFinished();
    postEvent(new AdEvent(AdEventType.SHOW_FINISHED, slotId));
  }

  public List<SlotStateMachine> getSlots() {
    return slotRegistry.getAll();
  }

  private void postEvent(final AdEvent event) {
    if (!started || handler == null) {
      return;
    }
    handler.post(new Runnable() {
      @Override
      public void run() {
        handleEvent(event);
      }
    });
  }

  private void handleEvent(AdEvent event) {
    Log.d(TAG, "AdAutoScheduler event=" + event.getType() + ", slot=" + event.getSlotId() + ", reason=" + event.getReason());
    applyEventToStateMachine(event);
    if (event.getType() == AdEventType.APP_BACKGROUND || event.getType() == AdEventType.MEMORY_PRESSURE) {
      tick(event.getType().name());
      return;
    }
    if (event.getType() == AdEventType.SHOW_FINISHED) {
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          tick("show_finished_refill");
        }
      }, strategy.getConfig().getShowFinishedRefillDelayMs());
      return;
    }
    tick(formatTrigger(event));
  }

  private void applyEventToStateMachine(AdEvent event) {
    if (event.getSlotId() == null) {
      return;
    }
    SlotStateMachine machine = slotRegistry.get(event.getSlotId());
    if (machine == null) {
      return;
    }
    switch (event.getType()) {
      case LOAD_STARTED:
        machine.onLoadStarted();
        break;
      case LOAD_SUCCESS:
        machine.onLoadSuccess();
        break;
      case LOAD_FAILED:
        machine.onLoadFailed(event.getReason(), retryDelay(event.getReason(), machine));
        break;
      case SHOW_STARTED:
        machine.onShowStarted();
        break;
      case SHOW_FINISHED:
        machine.onShowFinished(strategy.getConfig().getShowFinishedRefillDelayMs());
        break;
      case AD_RELEASED:
        machine.onReleased();
        break;
      case SLOT_DISABLED:
        machine.onDisabled();
        break;
      case SLOT_ENABLED:
        machine.onEnabled();
        break;
      default:
        break;
    }
  }

  private void tick(String trigger) {
    Log.d(TAG, "AdAutoScheduler tick trigger=" + trigger);
    Map<String, AdSlotState> inventory = inventoryMap();
    RuntimeState runtimeState = strategy.getRuntimeState();
    long now = SystemClock.elapsedRealtime();
    List<AdAction> actions = new ArrayList<>();
    Map<String, Integer> actionCounts = new LinkedHashMap<>();
    Map<String, Integer> reasonCounts = new LinkedHashMap<>();
    Map<String, Integer> adTypeCounts = new LinkedHashMap<>();
    for (SlotStateMachine machine : slotRegistry.getAll()) {
      AdSlotState state = inventory.get(machine.getSlot().getSlotId());
      machine.refreshFromInventory(state);
      PolicyContext context = new PolicyContext(strategy, runtimeState, machine, state, now);
      PolicyDecision decision = policyChain.evaluate(context);
      AdAction action = toAction(machine, decision);
      actions.add(action);
      increment(actionCounts, action.getType().name());
      increment(reasonCounts, action.getReason());
      increment(adTypeCounts, action.getAdType().name() + "_" + action.getType().name());
    }
    String decisionSummary = formatActionCounts(actionCounts);
    String decisionDetail = "device=" + strategy.getDeviceSnapshot().getLevel()
        + ", memoryPressure=" + runtimeState.isMemoryPressure()
        + ", criticalMemoryPressure=" + runtimeState.isCriticalMemoryPressure()
        + ", availableMemoryMb=" + runtimeState.getAvailableMemoryMb()
        + ", thresholdMemoryMb=" + runtimeState.getThresholdMemoryMb()
        + ", criticalMemoryReason=" + runtimeState.getCriticalMemoryReason()
        + ", mainThreadBusy=" + runtimeState.isMainThreadBusy()
        + ", adShowing=" + runtimeState.isAdShowing()
        + ", background=" + runtimeState.isBackground()
        + ", slots=" + actions.size()
        + ", actions=" + actionCounts
        + ", reasons=" + reasonCounts
        + ", byAdType=" + adTypeCounts;
    EventState decisionState = trigger.startsWith(AdEventType.LOAD_FAILED.name())
        || actionCounts.containsKey(AdActionType.RELEASE.name())
        || actionCounts.containsKey(AdActionType.DISABLE.name())
        ? EventState.WARNING : EventState.NORMAL;
    TestManager.getInstance().addEvent(TEST_TAG,
        trigger + "：" + decisionSummary,
        decisionState,
        decisionDetail);
    for (AdAction action : actions) {
      if (shouldLogDetail(action)) {
        EventState actionState = action.getType() == AdActionType.RELEASE
            || action.getType() == AdActionType.DISABLE
            ? EventState.WARNING : EventState.NORMAL;
        TestManager.getInstance().addEvent(TEST_TAG,
            action.getType() + " " + action.getAdType() + "：" + action.getSlotId(),
            actionState,
            "reason=" + action.getReason() + ", score=" + action.getScore());
      }
      if (shouldExecute(action)) {
        actionExecutor.execute(action);
        SlotStateMachine machine = slotRegistry.get(action.getSlotId());
        if (machine != null) {
          updateMachineAfterAction(machine, action);
        }
      }
      if (shouldEmitAction(action)) {
        strategy.emitSchedulerAction(action, trigger);
      }
    }
  }

  private void releaseAllNow(String reason) {
    Log.d(TAG, "AdAutoScheduler releaseAll reason=" + reason);
    List<SlotStateMachine> slots = slotRegistry.getAll();
    Map<String, Integer> adTypeCounts = new LinkedHashMap<>();
    for (SlotStateMachine machine : slots) {
      increment(adTypeCounts, machine.getSlot().getAdType().name());
    }
    TestManager.getInstance().addEvent(TEST_TAG,
        "释放全部：" + slots.size() + " 个",
        EventState.WARNING,
        "reason=" + reason + ", byAdType=" + adTypeCounts);
    for (SlotStateMachine machine : slots) {
      AdAction action = AdAction.release(
          machine.getSlot().getSlotId(),
          machine.getSlot().getAdType(),
          reason,
          -100
      );
      actionExecutor.execute(action);
      updateMachineAfterAction(machine, action);
      if (shouldEmitAction(action)) {
        strategy.emitSchedulerAction(action, reason);
      }
    }
  }

  private Map<String, AdSlotState> inventoryMap() {
    Map<String, AdSlotState> map = new HashMap<>();
    List<AdSlotState> states = inventoryProvider.getSlotStates();
    if (states == null) {
      return map;
    }
    for (AdSlotState state : states) {
      map.put(state.getSlotId(), state);
    }
    return map;
  }

  private AdAction toAction(SlotStateMachine machine, PolicyDecision decision) {
    if (decision == null) {
      return AdAction.keep(machine.getSlot().getSlotId(), machine.getSlot().getAdType(), "no_policy", 0);
    }
    if (decision.getType() == PolicyDecisionType.LOAD) {
      return AdAction.load(machine.getSlot().getSlotId(), machine.getSlot().getAdType(), decision.getReason(), decision.getScore());
    }
    if (decision.getType() == PolicyDecisionType.RELEASE) {
      return AdAction.release(machine.getSlot().getSlotId(), machine.getSlot().getAdType(), decision.getReason(), decision.getScore());
    }
    if (decision.getType() == PolicyDecisionType.PAUSE) {
      return AdAction.keep(machine.getSlot().getSlotId(), machine.getSlot().getAdType(), decision.getReason(), decision.getScore());
    }
    if (decision.getType() == PolicyDecisionType.DISABLE) {
      return AdAction.disable(machine.getSlot().getSlotId(), machine.getSlot().getAdType(), decision.getReason(), decision.getScore());
    }
    return AdAction.keep(machine.getSlot().getSlotId(), machine.getSlot().getAdType(), decision.getReason(), decision.getScore());
  }

  private boolean shouldExecute(AdAction action) {
    if (action.getType() != AdActionType.KEEP) {
      return true;
    }
    return action.getAdType() == com.ivy.adstrategy.AdType.BANNER
        && "not_preloadable".equals(action.getReason());
  }

  private boolean shouldEmitAction(AdAction action) {
    return action.getType() == AdActionType.LOAD
        || action.getType() == AdActionType.RELEASE
        || action.getType() == AdActionType.DISABLE;
  }

  private boolean shouldLogDetail(AdAction action) {
    return action.getType() != AdActionType.KEEP;
  }

  private void increment(Map<String, Integer> counts, String key) {
    String safeKey = key == null ? "unknown" : key;
    Integer count = counts.get(safeKey);
    counts.put(safeKey, count == null ? 1 : count + 1);
  }

  private String formatActionCounts(Map<String, Integer> actionCounts) {
    StringBuilder summary = new StringBuilder();
    appendActionCount(summary, actionCounts, AdActionType.LOAD, "加载");
    appendActionCount(summary, actionCounts, AdActionType.RELEASE, "释放");
    appendActionCount(summary, actionCounts, AdActionType.DISABLE, "禁用");
    appendActionCount(summary, actionCounts, AdActionType.KEEP, "保持");
    return summary.length() == 0 ? "无广告位" : summary.toString();
  }

  private String formatTrigger(AdEvent event) {
    String trigger = event.getType().name();
    if ((event.getType() == AdEventType.LOAD_STARTED
        || event.getType() == AdEventType.LOAD_SUCCESS
        || event.getType() == AdEventType.LOAD_FAILED)
        && event.getAdUnit() != null && event.getAdUnit().length() > 0) {
      return trigger + " [" + event.getAdUnit() + "]";
    }
    return trigger;
  }

  private void appendActionCount(StringBuilder summary, Map<String, Integer> actionCounts,
                                 AdActionType actionType, String label) {
    Integer count = actionCounts.get(actionType.name());
    if (count == null || count == 0) {
      return;
    }
    if (summary.length() > 0) {
      summary.append("，");
    }
    summary.append(label).append(' ').append(count);
  }

  private void updateMachineAfterAction(SlotStateMachine machine, AdAction action) {
    if (action.getType() == AdActionType.LOAD) {
      machine.onLoadStarted();
    } else if (action.getType() == AdActionType.RELEASE) {
      machine.onReleased();
    } else if (action.getType() == AdActionType.DISABLE) {
      machine.onDisabled();
    }
  }

  private void scheduleNextTick() {
    if (!started || handler == null) {
      return;
    }
    long delay = strategy.getRuntimeState().isLaunchPeriod()
        ? strategy.getConfig().getStartupTickIntervalMs()
        : strategy.getConfig().getForegroundTickIntervalMs();
    handler.removeCallbacks(tickRunnable);
    handler.postDelayed(tickRunnable, delay);
  }

  private long retryDelay(String reason, SlotStateMachine machine) {
    if (machine.getConsecutiveFailCount() + 1 >= strategy.getConfig().getMaxRetryCount()) {
      return strategy.getConfig().getRetryCooldownMs();
    }
    if (reason == null) {
      return strategy.getConfig().getOtherRetryDelayMs();
    }
    String lower = reason.toLowerCase();
    if (lower.contains("no_fill") || lower.contains("nofill")) {
      return strategy.getConfig().getNoFillRetryDelayMs();
    }
    if (lower.contains("network")) {
      return strategy.getConfig().getNetworkRetryDelayMs();
    }
    if (lower.contains("timeout")) {
      return strategy.getConfig().getTimeoutRetryDelayMs();
    }
    return strategy.getConfig().getOtherRetryDelayMs();
  }
}
