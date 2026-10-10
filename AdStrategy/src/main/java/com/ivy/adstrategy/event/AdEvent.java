package com.ivy.adstrategy.event;

import android.os.SystemClock;

public class AdEvent {
  private final AdEventType type;
  private final String slotId;
  private final String adUnit;
  private final String reason;
  private final long timestampMs;

  public AdEvent(AdEventType type) {
    this(type, null, null);
  }

  public AdEvent(AdEventType type, String slotId) {
    this(type, slotId, null);
  }

  public AdEvent(AdEventType type, String slotId, String reason) {
    this(type, slotId, null, reason);
  }

  public AdEvent(AdEventType type, String slotId, String adUnit, String reason) {
    this.type = type;
    this.slotId = slotId;
    this.adUnit = adUnit;
    this.reason = reason;
    this.timestampMs = SystemClock.elapsedRealtime();
  }

  public AdEventType getType() {
    return type;
  }

  public String getSlotId() {
    return slotId;
  }

  public String getAdUnit() {
    return adUnit;
  }

  public String getReason() {
    return reason;
  }

  public long getTimestampMs() {
    return timestampMs;
  }
}
