package com.ivy.adstrategy.slot;

import com.ivy.adstrategy.AdType;

public class AdSlot {
  private final String slotId;
  private final AdType adType;
  private final int priority;
  private final int revenueScore;
  private final int resourceCost;
  private final boolean preloadable;

  public AdSlot(String slotId, AdType adType, int priority, int revenueScore, int resourceCost, boolean preloadable) {
    if (slotId == null || slotId.length() == 0) {
      throw new IllegalArgumentException("slotId must not be empty");
    }
    if (adType == null) {
      throw new IllegalArgumentException("adType must not be null");
    }
    this.slotId = slotId;
    this.adType = adType;
    this.priority = priority;
    this.revenueScore = revenueScore;
    this.resourceCost = resourceCost;
    this.preloadable = preloadable;
  }

  public String getSlotId() {
    return slotId;
  }

  public AdType getAdType() {
    return adType;
  }

  public int getPriority() {
    return priority;
  }

  public int getRevenueScore() {
    return revenueScore;
  }

  public int getResourceCost() {
    return resourceCost;
  }

  public boolean isPreloadable() {
    return preloadable;
  }
}
