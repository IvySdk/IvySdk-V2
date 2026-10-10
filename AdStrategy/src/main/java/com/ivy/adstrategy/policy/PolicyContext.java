package com.ivy.adstrategy.policy;

import com.ivy.adstrategy.AdStrategy;
import com.ivy.adstrategy.runtime.RuntimeState;
import com.ivy.adstrategy.slot.AdSlotState;
import com.ivy.adstrategy.slot.SlotStateMachine;

public class PolicyContext {
  private final AdStrategy strategy;
  private final RuntimeState runtimeState;
  private final SlotStateMachine slotStateMachine;
  private final AdSlotState inventoryState;
  private final long nowMs;

  public PolicyContext(AdStrategy strategy, RuntimeState runtimeState, SlotStateMachine slotStateMachine, AdSlotState inventoryState, long nowMs) {
    this.strategy = strategy;
    this.runtimeState = runtimeState;
    this.slotStateMachine = slotStateMachine;
    this.inventoryState = inventoryState;
    this.nowMs = nowMs;
  }

  public AdStrategy getStrategy() {
    return strategy;
  }

  public RuntimeState getRuntimeState() {
    return runtimeState;
  }

  public SlotStateMachine getSlotStateMachine() {
    return slotStateMachine;
  }

  public AdSlotState getInventoryState() {
    return inventoryState;
  }

  public long getNowMs() {
    return nowMs;
  }
}
