package com.ivy.adstrategy.scheduler;

import com.ivy.adstrategy.AdType;

public class AdAction {
  private final AdActionType type;
  private final String slotId;
  private final AdType adType;
  private final String reason;
  private final int score;

  public AdAction(AdActionType type, String slotId, AdType adType, String reason, int score) {
    this.type = type;
    this.slotId = slotId;
    this.adType = adType;
    this.reason = reason;
    this.score = score;
  }

  public static AdAction load(String slotId, AdType adType, String reason, int score) {
    return new AdAction(AdActionType.LOAD, slotId, adType, reason, score);
  }

  public static AdAction release(String slotId, AdType adType, String reason, int score) {
    return new AdAction(AdActionType.RELEASE, slotId, adType, reason, score);
  }

  public static AdAction disable(String slotId, AdType adType, String reason, int score) {
    return new AdAction(AdActionType.DISABLE, slotId, adType, reason, score);
  }

  public static AdAction keep(String slotId, AdType adType, String reason, int score) {
    return new AdAction(AdActionType.KEEP, slotId, adType, reason, score);
  }

  public AdActionType getType() {
    return type;
  }

  public String getSlotId() {
    return slotId;
  }

  public AdType getAdType() {
    return adType;
  }

  public String getReason() {
    return reason;
  }

  public int getScore() {
    return score;
  }
}
