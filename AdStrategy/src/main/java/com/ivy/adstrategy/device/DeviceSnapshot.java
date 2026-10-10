package com.ivy.adstrategy.device;

public class DeviceSnapshot {
  private final DeviceLevel level;
  private final long totalMemoryMb;
  private final int cpuCores;
  private final boolean lowRamDevice;

  public DeviceSnapshot(DeviceLevel level, long totalMemoryMb, int cpuCores, boolean lowRamDevice) {
    this.level = level;
    this.totalMemoryMb = totalMemoryMb;
    this.cpuCores = cpuCores;
    this.lowRamDevice = lowRamDevice;
  }

  public DeviceLevel getLevel() {
    return level;
  }

  public long getTotalMemoryMb() {
    return totalMemoryMb;
  }

  public int getCpuCores() {
    return cpuCores;
  }

  public boolean isLowRamDevice() {
    return lowRamDevice;
  }
}
