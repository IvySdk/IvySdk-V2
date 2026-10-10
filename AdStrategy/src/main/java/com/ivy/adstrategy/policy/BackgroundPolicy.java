package com.ivy.adstrategy.policy;

import com.ivy.adstrategy.AdType;

public class BackgroundPolicy implements AdPolicy {
  @Override
  public PolicyDecision evaluate(PolicyContext context) {
    if (!context.getRuntimeState().isBackground()) {
      return PolicyDecision.none();
    }
    AdType adType = context.getSlotStateMachine().getSlot().getAdType();
    if (adType == AdType.BANNER || adType == AdType.NATIVE) {
      return new PolicyDecision(PolicyDecisionType.RELEASE, "background_release_low_priority", -60, 90);
    }
    return new PolicyDecision(PolicyDecisionType.PAUSE, "background_pause", -30, 70);
  }
}
