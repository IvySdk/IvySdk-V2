package com.ivy.adstrategy.slot;

import android.os.SystemClock;

public class SlotStateMachine {
  private final AdSlot slot;
  private SlotStatus status = SlotStatus.IDLE;
  private int consecutiveFailCount;
  private long nextAllowedLoadTimeMs;
  private long lastReadyTimeMs;
  private String lastFailReason;

  public SlotStateMachine(AdSlot slot) {
    this.slot = slot;
  }

  public AdSlot getSlot() {
    return slot;
  }

  public SlotStatus getStatus() {
    return status;
  }

  public int getConsecutiveFailCount() {
    return consecutiveFailCount;
  }

  public long getNextAllowedLoadTimeMs() {
    return nextAllowedLoadTimeMs;
  }

  public String getLastFailReason() {
    return lastFailReason;
  }

  public void onLoadStarted() {
    if (status == SlotStatus.DISABLED) {
      return;
    }
    status = SlotStatus.LOADING;
  }

  public void onLoadSuccess() {
    clearRetryState();
    lastReadyTimeMs = SystemClock.elapsedRealtime();
    status = SlotStatus.READY;
  }

  public void onLoadFailed(String reason, long retryDelayMs) {
    consecutiveFailCount++;
    lastFailReason = reason;
    nextAllowedLoadTimeMs = SystemClock.elapsedRealtime() + Math.max(0L, retryDelayMs);
    status = SlotStatus.COOLDOWN;
  }

  public void onShowStarted() {
    if (status == SlotStatus.READY) {
      status = SlotStatus.SHOWING;
    }
  }

  public void onShowFinished(long cooldownMs) {
    nextAllowedLoadTimeMs = SystemClock.elapsedRealtime() + Math.max(0L, cooldownMs);
    status = SlotStatus.COOLDOWN;
  }

  public void onReleased() {
    if (status == SlotStatus.COOLDOWN && SystemClock.elapsedRealtime() >= nextAllowedLoadTimeMs) {
      clearRetryState();
    }
    status = SlotStatus.RELEASED;
  }

  public void onDisabled() {
    status = SlotStatus.DISABLED;
  }

  public void onEnabled() {
    if (status == SlotStatus.DISABLED) {
      status = SlotStatus.IDLE;
    }
  }

  public void refreshFromInventory(AdSlotState state) {
    if (status == SlotStatus.DISABLED || state == null) {
      return;
    }
    if (state.getReadyCount() > 0) {
      status = SlotStatus.READY;
      if (lastReadyTimeMs == 0L) {
        lastReadyTimeMs = SystemClock.elapsedRealtime() - Math.max(0L, state.getOldestReadyAgeMs());
      }
    } else if (state.getLoadingCount() > 0) {
      status = SlotStatus.LOADING;
    } else if (status == SlotStatus.RELEASED) {
      status = SlotStatus.IDLE;
    } else if (status == SlotStatus.COOLDOWN && SystemClock.elapsedRealtime() >= nextAllowedLoadTimeMs) {
      clearRetryState();
      status = SlotStatus.IDLE;
    }
  }

  public long getReadyAgeMs() {
    if (lastReadyTimeMs == 0L) {
      return 0L;
    }
    return Math.max(0L, SystemClock.elapsedRealtime() - lastReadyTimeMs);
  }

  private void clearRetryState() {
    consecutiveFailCount = 0;
    nextAllowedLoadTimeMs = 0L;
    lastFailReason = null;
  }
}
