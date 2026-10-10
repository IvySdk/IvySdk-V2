package com.ivy.adstrategy.policy;

import com.ivy.adstrategy.slot.AdSlot;
import com.ivy.adstrategy.slot.AdSlotState;
import com.ivy.adstrategy.slot.SlotStatus;

public class InventoryPolicy implements AdPolicy {
  @Override
  public PolicyDecision evaluate(PolicyContext context) {
    AdSlot slot = context.getSlotStateMachine().getSlot();
    AdSlotState state = context.getInventoryState();
    if (!slot.isPreloadable()) {
      return new PolicyDecision(PolicyDecisionType.KEEP, "not_preloadable", 0, 10);
    }
    int ready = state == null ? 0 : state.getReadyCount();
    int loading = state == null ? 0 : state.getLoadingCount();
    int target = context.getStrategy().targetReadyCount(slot.getAdType());
    if (target <= 0) {
      boolean hasInventory = state != null && (state.getReadyCount() > 0 || state.getLoadingCount() > 0);
      boolean active = context.getSlotStateMachine().getStatus() == SlotStatus.READY
          || context.getSlotStateMachine().getStatus() == SlotStatus.LOADING
          || context.getSlotStateMachine().getStatus() == SlotStatus.SHOWING;
      if (!hasInventory && !active) {
        return new PolicyDecision(PolicyDecisionType.KEEP, "target_zero_idle", 0, 40);
      }
      return new PolicyDecision(PolicyDecisionType.RELEASE, "target_zero", 0, 40);
    }
    if (ready + loading < target) {
      return new PolicyDecision(PolicyDecisionType.LOAD, "inventory_below_target", 100, 30);
    }
    if (ready > target) {
      return new PolicyDecision(PolicyDecisionType.RELEASE, "inventory_above_target", 0, 35);
    }
    return new PolicyDecision(PolicyDecisionType.KEEP, "inventory_enough", 0, 10);
  }
}
