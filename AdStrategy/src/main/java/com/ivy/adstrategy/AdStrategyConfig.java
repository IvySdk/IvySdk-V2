package com.ivy.adstrategy;

import java.util.EnumMap;
import java.util.Map;

public class AdStrategyConfig {
  private final Map<AdType, Integer> targetReadyCounts = new EnumMap<>(AdType.class);
  private final Map<AdType, Boolean> keepReadyOverrides = new EnumMap<>(AdType.class);
  private long launchPeriodMs = 30000L;
  private long mainThreadBusyThresholdMs = 700L;
  private long mainThreadBeatIntervalMs = 500L;
  private long lowMemoryMb = 2048L;
  private long highMemoryMb = 4096L;
  private int lowCpuCores = 4;
  private int highCpuCores = 8;
  private boolean blockHeavyAdsInLaunchPeriod = true;
  private long foregroundTickIntervalMs = 20000L;
  private long startupTickIntervalMs = 8000L;
  private long showFinishedRefillDelayMs = 5000L;
  private long maxAdShowingSuppressMs = 70000L;
  private long noFillRetryDelayMs = 60000L;
  private long networkRetryDelayMs = 30000L;
  private long timeoutRetryDelayMs = 30000L;
  private long otherRetryDelayMs = 20000L;
  private long retryCooldownMs = 300000L;
  private long memoryPressureSignalTtlMs = 30000L;
  private long criticalAvailableMemoryMb = 200L;
  private boolean previousExitGuardEnabled = true;
  private long previousExitMaxAgeMs = 7L * 24L * 60L * 60L * 1000L;
  private int maxRetryCount = 3;
  private long gateCheckIntervalMs = 500L;
  private long gateMaxWaitMs = 30000L;
  private long gateMaxMainLagMs = 500L;
  private long bannerSevereStallCooldownMs = 300000L;
  private int bannerSevereStallThresholdCount = 2;
  private int maxBannerSevereStallOccurrences = 2;

  public long getLaunchPeriodMs() {
    return launchPeriodMs;
  }

  public AdStrategyConfig setLaunchPeriodMs(long launchPeriodMs) {
    this.launchPeriodMs = Math.max(0L, launchPeriodMs);
    return this;
  }

  public long getMainThreadBusyThresholdMs() {
    return mainThreadBusyThresholdMs;
  }

  public AdStrategyConfig setMainThreadBusyThresholdMs(long mainThreadBusyThresholdMs) {
    this.mainThreadBusyThresholdMs = Math.max(100L, mainThreadBusyThresholdMs);
    return this;
  }

  public long getMainThreadBeatIntervalMs() {
    return mainThreadBeatIntervalMs;
  }

  public AdStrategyConfig setMainThreadBeatIntervalMs(long mainThreadBeatIntervalMs) {
    this.mainThreadBeatIntervalMs = Math.max(100L, mainThreadBeatIntervalMs);
    return this;
  }

  public long getLowMemoryMb() {
    return lowMemoryMb;
  }

  public AdStrategyConfig setLowMemoryMb(long lowMemoryMb) {
    this.lowMemoryMb = Math.max(0L, lowMemoryMb);
    return this;
  }

  public long getHighMemoryMb() {
    return highMemoryMb;
  }

  public AdStrategyConfig setHighMemoryMb(long highMemoryMb) {
    this.highMemoryMb = Math.max(0L, highMemoryMb);
    return this;
  }

  public int getLowCpuCores() {
    return lowCpuCores;
  }

  public AdStrategyConfig setLowCpuCores(int lowCpuCores) {
    this.lowCpuCores = Math.max(1, lowCpuCores);
    return this;
  }

  public int getHighCpuCores() {
    return highCpuCores;
  }

  public AdStrategyConfig setHighCpuCores(int highCpuCores) {
    this.highCpuCores = Math.max(1, highCpuCores);
    return this;
  }

  public boolean isBlockHeavyAdsInLaunchPeriod() {
    return blockHeavyAdsInLaunchPeriod;
  }

  public AdStrategyConfig setBlockHeavyAdsInLaunchPeriod(boolean blockHeavyAdsInLaunchPeriod) {
    this.blockHeavyAdsInLaunchPeriod = blockHeavyAdsInLaunchPeriod;
    return this;
  }

  public long getForegroundTickIntervalMs() {
    return foregroundTickIntervalMs;
  }

  public AdStrategyConfig setForegroundTickIntervalMs(long foregroundTickIntervalMs) {
    this.foregroundTickIntervalMs = Math.max(1000L, foregroundTickIntervalMs);
    return this;
  }

  public long getStartupTickIntervalMs() {
    return startupTickIntervalMs;
  }

  public AdStrategyConfig setStartupTickIntervalMs(long startupTickIntervalMs) {
    this.startupTickIntervalMs = Math.max(1000L, startupTickIntervalMs);
    return this;
  }

  public long getShowFinishedRefillDelayMs() {
    return showFinishedRefillDelayMs;
  }

  public AdStrategyConfig setShowFinishedRefillDelayMs(long showFinishedRefillDelayMs) {
    this.showFinishedRefillDelayMs = Math.max(0L, showFinishedRefillDelayMs);
    return this;
  }

  public long getMaxAdShowingSuppressMs() {
    return maxAdShowingSuppressMs;
  }

  public AdStrategyConfig setMaxAdShowingSuppressMs(long maxAdShowingSuppressMs) {
    this.maxAdShowingSuppressMs = Math.max(0L, maxAdShowingSuppressMs);
    return this;
  }

  public long getNoFillRetryDelayMs() {
    return noFillRetryDelayMs;
  }

  public AdStrategyConfig setNoFillRetryDelayMs(long noFillRetryDelayMs) {
    this.noFillRetryDelayMs = Math.max(0L, noFillRetryDelayMs);
    return this;
  }

  public long getNetworkRetryDelayMs() {
    return networkRetryDelayMs;
  }

  public AdStrategyConfig setNetworkRetryDelayMs(long networkRetryDelayMs) {
    this.networkRetryDelayMs = Math.max(0L, networkRetryDelayMs);
    return this;
  }

  public long getTimeoutRetryDelayMs() {
    return timeoutRetryDelayMs;
  }

  public AdStrategyConfig setTimeoutRetryDelayMs(long timeoutRetryDelayMs) {
    this.timeoutRetryDelayMs = Math.max(0L, timeoutRetryDelayMs);
    return this;
  }

  public long getOtherRetryDelayMs() {
    return otherRetryDelayMs;
  }

  public AdStrategyConfig setOtherRetryDelayMs(long otherRetryDelayMs) {
    this.otherRetryDelayMs = Math.max(0L, otherRetryDelayMs);
    return this;
  }

  public long getRetryCooldownMs() {
    return retryCooldownMs;
  }

  public AdStrategyConfig setRetryCooldownMs(long retryCooldownMs) {
    this.retryCooldownMs = Math.max(0L, retryCooldownMs);
    return this;
  }

  public long getMemoryPressureSignalTtlMs() {
    return memoryPressureSignalTtlMs;
  }

  public AdStrategyConfig setMemoryPressureSignalTtlMs(long memoryPressureSignalTtlMs) {
    this.memoryPressureSignalTtlMs = Math.max(0L, memoryPressureSignalTtlMs);
    return this;
  }

  public long getCriticalAvailableMemoryMb() {
    return criticalAvailableMemoryMb;
  }

  public AdStrategyConfig setCriticalAvailableMemoryMb(long criticalAvailableMemoryMb) {
    this.criticalAvailableMemoryMb = Math.max(0L, criticalAvailableMemoryMb);
    return this;
  }

  public boolean isPreviousExitGuardEnabled() {
    return previousExitGuardEnabled;
  }

  public AdStrategyConfig setPreviousExitGuardEnabled(boolean previousExitGuardEnabled) {
    this.previousExitGuardEnabled = previousExitGuardEnabled;
    return this;
  }

  public long getPreviousExitMaxAgeMs() {
    return previousExitMaxAgeMs;
  }

  public AdStrategyConfig setPreviousExitMaxAgeMs(long previousExitMaxAgeMs) {
    this.previousExitMaxAgeMs = Math.max(0L, previousExitMaxAgeMs);
    return this;
  }

  public int getMaxRetryCount() {
    return maxRetryCount;
  }

  public AdStrategyConfig setMaxRetryCount(int maxRetryCount) {
    this.maxRetryCount = Math.max(1, maxRetryCount);
    return this;
  }

  public long getGateCheckIntervalMs() {
    return gateCheckIntervalMs;
  }

  public AdStrategyConfig setGateCheckIntervalMs(long gateCheckIntervalMs) {
    this.gateCheckIntervalMs = Math.max(100L, gateCheckIntervalMs);
    return this;
  }

  public long getGateMaxWaitMs() {
    return gateMaxWaitMs;
  }

  public AdStrategyConfig setGateMaxWaitMs(long gateMaxWaitMs) {
    this.gateMaxWaitMs = Math.max(0L, gateMaxWaitMs);
    return this;
  }

  public long getGateMaxMainLagMs() {
    return gateMaxMainLagMs;
  }

  public AdStrategyConfig setGateMaxMainLagMs(long gateMaxMainLagMs) {
    this.gateMaxMainLagMs = Math.max(100L, gateMaxMainLagMs);
    return this;
  }

  public long getBannerSevereStallCooldownMs() {
    return bannerSevereStallCooldownMs;
  }

  public AdStrategyConfig setBannerSevereStallCooldownMs(long bannerSevereStallCooldownMs) {
    this.bannerSevereStallCooldownMs = Math.max(0L, bannerSevereStallCooldownMs);
    return this;
  }

  public int getBannerSevereStallThresholdCount() {
    return bannerSevereStallThresholdCount;
  }

  public AdStrategyConfig setBannerSevereStallThresholdCount(int bannerSevereStallThresholdCount) {
    this.bannerSevereStallThresholdCount = Math.max(0, bannerSevereStallThresholdCount);
    return this;
  }

  public int getMaxBannerSevereStallOccurrences() {
    return maxBannerSevereStallOccurrences;
  }

  public AdStrategyConfig setMaxBannerSevereStallOccurrences(int maxBannerSevereStallOccurrences) {
    this.maxBannerSevereStallOccurrences = Math.max(1, maxBannerSevereStallOccurrences);
    return this;
  }

  public AdStrategyConfig setTargetReadyCount(AdType adType, int targetReadyCount) {
    if (adType != null) {
      targetReadyCounts.put(adType, Math.max(0, targetReadyCount));
    }
    return this;
  }

  public Integer getTargetReadyCountOverride(AdType adType) {
    return targetReadyCounts.get(adType);
  }

  public AdStrategyConfig setKeepReady(AdType adType, boolean keepReady) {
    if (adType != null) {
      keepReadyOverrides.put(adType, keepReady);
      targetReadyCounts.put(adType, keepReady ? 1 : 0);
    }
    return this;
  }

  public Boolean getKeepReadyOverride(AdType adType) {
    return keepReadyOverrides.get(adType);
  }
}
