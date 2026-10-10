package com.ivy.adstrategy.runtime;

public class RuntimeState {
  private final boolean launchPeriod;
  private final boolean background;
  private final boolean memoryPressure;
  private final boolean criticalMemoryPressure;
  private final long availableMemoryMb;
  private final long thresholdMemoryMb;
  private final String criticalMemoryReason;
  private final boolean mainThreadBusy;
  private final boolean adShowing;
  private final boolean weakNetwork;
  private final long currentMainLagMs;
  private final long recentMaxLagMs;
  private final int recentSevereStallCount;
  private final long timeSinceLastSevereStallMs;

  public RuntimeState(boolean launchPeriod, boolean background, boolean memoryPressure,
      boolean criticalMemoryPressure, long availableMemoryMb, long thresholdMemoryMb,
      String criticalMemoryReason, boolean mainThreadBusy, boolean adShowing,
      boolean weakNetwork, long currentMainLagMs, long recentMaxLagMs,
      int recentSevereStallCount, long timeSinceLastSevereStallMs) {
    this.launchPeriod = launchPeriod;
    this.background = background;
    this.memoryPressure = memoryPressure;
    this.criticalMemoryPressure = criticalMemoryPressure;
    this.availableMemoryMb = availableMemoryMb;
    this.thresholdMemoryMb = thresholdMemoryMb;
    this.criticalMemoryReason = criticalMemoryReason;
    this.mainThreadBusy = mainThreadBusy;
    this.adShowing = adShowing;
    this.weakNetwork = weakNetwork;
    this.currentMainLagMs = currentMainLagMs;
    this.recentMaxLagMs = recentMaxLagMs;
    this.recentSevereStallCount = recentSevereStallCount;
    this.timeSinceLastSevereStallMs = timeSinceLastSevereStallMs;
  }

  public boolean isLaunchPeriod() {
    return launchPeriod;
  }

  public boolean isBackground() {
    return background;
  }

  public boolean isMemoryPressure() {
    return memoryPressure;
  }

  public boolean isCriticalMemoryPressure() {
    return criticalMemoryPressure;
  }

  public long getAvailableMemoryMb() {
    return availableMemoryMb;
  }

  public long getThresholdMemoryMb() {
    return thresholdMemoryMb;
  }

  public String getCriticalMemoryReason() {
    return criticalMemoryReason;
  }

  public boolean isMainThreadBusy() {
    return mainThreadBusy;
  }

  public boolean isAdShowing() {
    return adShowing;
  }

  public boolean isWeakNetwork() {
    return weakNetwork;
  }

  public long getCurrentMainLagMs() {
    return currentMainLagMs;
  }

  public long getRecentMaxLagMs() {
    return recentMaxLagMs;
  }

  public int getRecentSevereStallCount() {
    return recentSevereStallCount;
  }

  public long getTimeSinceLastSevereStallMs() {
    return timeSinceLastSevereStallMs;
  }
}
