package com.ivy.adstrategy.device;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import com.ivy.adstrategy.AdStrategyConfig;

public class DeviceClassifier {
  private final Context context;
  private final AdStrategyConfig config;

  public DeviceClassifier(Context context, AdStrategyConfig config) {
    this.context = context.getApplicationContext();
    this.config = config;
  }

  public DeviceSnapshot snapshot() {
    long totalMemoryMb = readTotalMemoryMb();
    int cpuCores = Math.max(1, Runtime.getRuntime().availableProcessors());
    boolean lowRamDevice = readLowRamDevice();
    DeviceLevel level = classify(totalMemoryMb, lowRamDevice);
    return new DeviceSnapshot(level, totalMemoryMb, cpuCores, lowRamDevice);
  }

  private DeviceLevel classify(long totalMemoryMb, boolean lowRamDevice) {
    if (lowRamDevice || (totalMemoryMb > 0 && totalMemoryMb <= config.getLowMemoryMb())) {
      return DeviceLevel.LOW;
    }
    if (totalMemoryMb >= config.getHighMemoryMb()) {
      return DeviceLevel.HIGH;
    }
    return DeviceLevel.MID;
  }

  private boolean readLowRamDevice() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      return false;
    }
    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    return activityManager != null && activityManager.isLowRamDevice();
  }

  private long readTotalMemoryMb() {
    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    if (activityManager == null) {
      return 0L;
    }
    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
    activityManager.getMemoryInfo(memoryInfo);
    return memoryInfo.totalMem / 1024L / 1024L;
  }
}
