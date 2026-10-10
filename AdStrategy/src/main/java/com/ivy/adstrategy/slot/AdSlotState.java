package com.ivy.adstrategy.slot;

import com.ivy.adstrategy.AdType;

public class AdSlotState {
  private final String slotId;
  private final AdType adType;
  private final int readyCount;
  private final int loadingCount;
  private final int consecutiveFailCount;
  private final long oldestReadyAgeMs;
  private final long nextAllowedLoadTimeMs;
  private final SlotStatus status;

  public AdSlotState(String slotId, AdType adType, int readyCount, int loadingCount, int consecutiveFailCount, long oldestReadyAgeMs, long nextAllowedLoadTimeMs, SlotStatus status) {
    this.slotId = slotId;
    this.adType = adType;
    this.readyCount = readyCount;
    this.loadingCount = loadingCount;
    this.consecutiveFailCount = consecutiveFailCount;
    this.oldestReadyAgeMs = oldestReadyAgeMs;
    this.nextAllowedLoadTimeMs = nextAllowedLoadTimeMs;
    this.status = status;
  }

  public String getSlotId() {
    return slotId;
  }

  public AdType getAdType() {
    return adType;
  }

  public int getReadyCount() {
    return readyCount;
  }

  public int getLoadingCount() {
    return loadingCount;
  }

  public int getConsecutiveFailCount() {
    return consecutiveFailCount;
  }

  public long getOldestReadyAgeMs() {
    return oldestReadyAgeMs;
  }

  public long getNextAllowedLoadTimeMs() {
    return nextAllowedLoadTimeMs;
  }

  public SlotStatus getStatus() {
    return status;
  }
}
