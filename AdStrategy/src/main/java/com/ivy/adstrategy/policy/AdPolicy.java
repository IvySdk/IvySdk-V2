package com.ivy.adstrategy.policy;

public interface AdPolicy {
  PolicyDecision evaluate(PolicyContext context);
}
