package com.ivy.adstrategy.policy;

public class RetryPolicy implements AdPolicy {
  @Override
  public PolicyDecision evaluate(PolicyContext context) {
    if (context.getSlotStateMachine().getConsecutiveFailCount() >= context.getStrategy().getConfig().getMaxRetryCount()) {
      return new PolicyDecision(PolicyDecisionType.PAUSE, "retry_cooldown", -40, 65);
    }
    if (context.getNowMs() < context.getSlotStateMachine().getNextAllowedLoadTimeMs()) {
      return new PolicyDecision(PolicyDecisionType.KEEP, "waiting_retry_backoff", -10, 50);
    }
    return PolicyDecision.none();
  }
}
