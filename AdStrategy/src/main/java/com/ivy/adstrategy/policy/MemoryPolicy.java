package com.ivy.adstrategy.policy;

import com.ivy.adstrategy.AdType;

public class MemoryPolicy implements AdPolicy {
  public static final String CRITICAL_MEMORY_REASON = "available_memory_below_limit";

  @Override
  public PolicyDecision evaluate(PolicyContext context) {
    if (!context.getRuntimeState().isMemoryPressure()) {
      return PolicyDecision.none();
    }
    if (context.getRuntimeState().isCriticalMemoryPressure()) {
      return new PolicyDecision(PolicyDecisionType.DISABLE, CRITICAL_MEMORY_REASON, -100, 110);
    }
    AdType adType = context.getSlotStateMachine().getSlot().getAdType();
    if (adType == AdType.REWARDED) {
      return new PolicyDecision(PolicyDecisionType.PAUSE, "memory_pressure_keep_rewarded", -40, 80);
    }
    return new PolicyDecision(PolicyDecisionType.RELEASE, "memory_pressure_release", -80, 100);
  }
}
