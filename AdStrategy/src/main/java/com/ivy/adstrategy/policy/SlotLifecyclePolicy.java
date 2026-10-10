package com.ivy.adstrategy.policy;

import com.ivy.adstrategy.slot.SlotStatus;

public class SlotLifecyclePolicy implements AdPolicy {
  @Override
  public PolicyDecision evaluate(PolicyContext context) {
    SlotStatus status = context.getSlotStateMachine().getStatus();
    if (status == SlotStatus.DISABLED) {
      return new PolicyDecision(PolicyDecisionType.KEEP, "slot_disabled", 0, 120);
    }
    if (status == SlotStatus.LOADING) {
      return new PolicyDecision(PolicyDecisionType.KEEP, "slot_loading", 0, 85);
    }
    if (status == SlotStatus.SHOWING) {
      return new PolicyDecision(PolicyDecisionType.KEEP, "slot_showing", 0, 85);
    }
    return PolicyDecision.none();
  }
}
