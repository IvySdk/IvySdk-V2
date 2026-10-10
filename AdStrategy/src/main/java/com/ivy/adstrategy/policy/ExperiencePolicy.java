package com.ivy.adstrategy.policy;

import com.ivy.adstrategy.AdType;

public class ExperiencePolicy implements AdPolicy {
  private long bannerCooldownUntilMs = 0L;
  private int bannerSevereStallOccurrenceCount = 0;
  private boolean bannerCircuitBroken = false;

  @Override
  public PolicyDecision evaluate(PolicyContext context) {
    AdType adType = context.getSlotStateMachine().getSlot().getAdType();
    if (adType == AdType.BANNER) {
      if (bannerCircuitBroken) {
        return new PolicyDecision(PolicyDecisionType.DISABLE, "banner_circuit_breaker_disabled", -100, 110);
      }
      if (context.getNowMs() < bannerCooldownUntilMs) {
        return new PolicyDecision(PolicyDecisionType.KEEP, "banner_stall_cooldown", -50, 95);
      }
      int stallThreshold = context.getStrategy().getConfig().getBannerSevereStallThresholdCount();
      if (stallThreshold > 0 && context.getRuntimeState().getRecentSevereStallCount() >= stallThreshold) {
        bannerSevereStallOccurrenceCount++;
        int maxOccurrences = context.getStrategy().getConfig().getMaxBannerSevereStallOccurrences();
        if (maxOccurrences > 0 && bannerSevereStallOccurrenceCount >= maxOccurrences) {
          bannerCircuitBroken = true;
          return new PolicyDecision(PolicyDecisionType.DISABLE, "banner_circuit_breaker_disabled", -100, 110);
        }
        bannerCooldownUntilMs = context.getNowMs() + context.getStrategy().getConfig().getBannerSevereStallCooldownMs();
        return new PolicyDecision(PolicyDecisionType.RELEASE, "banner_sustained_severe_stall", -100, 95);
      }
    }
    if (context.getRuntimeState().isMainThreadBusy()) {
      return new PolicyDecision(PolicyDecisionType.PAUSE, "main_thread_busy", -50, 75);
    }
    if (context.getStrategy().getConfig().isBlockHeavyAdsInLaunchPeriod()
        && context.getRuntimeState().isLaunchPeriod()
        && (adType == AdType.INTERSTITIAL || adType == AdType.REWARDED_INTERSTITIAL || adType == AdType.REWARDED)) {
      return new PolicyDecision(PolicyDecisionType.PAUSE, "launch_period_heavy_ad", -35, 60);
    }
    return PolicyDecision.none();
  }
}
