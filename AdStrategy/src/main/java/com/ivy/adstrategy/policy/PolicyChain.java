package com.ivy.adstrategy.policy;

import java.util.ArrayList;
import java.util.List;

public class PolicyChain {
  private final List<AdPolicy> policies = new ArrayList<>();

  public PolicyChain add(AdPolicy policy) {
    if (policy != null) {
      policies.add(policy);
    }
    return this;
  }

  public PolicyDecision evaluate(PolicyContext context) {
    PolicyDecision best = PolicyDecision.none();
    for (AdPolicy policy : policies) {
      PolicyDecision decision = policy.evaluate(context);
      if (decision != null && decision.getType() != PolicyDecisionType.NONE && decision.getPriority() >= best.getPriority()) {
        best = decision;
      }
    }
    return best;
  }
}
