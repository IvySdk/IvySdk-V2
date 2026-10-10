package com.ivy.adstrategy.policy;

public class PolicyDecision {
  private final PolicyDecisionType type;
  private final String reason;
  private final int score;
  private final int priority;

  public PolicyDecision(PolicyDecisionType type, String reason, int score, int priority) {
    this.type = type;
    this.reason = reason;
    this.score = score;
    this.priority = priority;
  }

  public static PolicyDecision none() {
    return new PolicyDecision(PolicyDecisionType.NONE, "none", 0, 0);
  }

  public PolicyDecisionType getType() {
    return type;
  }

  public String getReason() {
    return reason;
  }

  public int getScore() {
    return score;
  }

  public int getPriority() {
    return priority;
  }
}
