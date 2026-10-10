package com.ivy.adstrategy.metrics;

import com.ivy.adstrategy.AdType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StrategyEvent {
  public static final String DEVICE_LEVEL = "ad_strategy_device_level";
  public static final String SCHEDULER_ACTION = "ad_strategy_scheduler_action";

  private final String name;
  private final AdType adType;
  private final Map<String, Object> params;

  public StrategyEvent(String name, AdType adType, Map<String, Object> params) {
    this.name = name;
    this.adType = adType;
    this.params = Collections.unmodifiableMap(new HashMap<>(params));
  }

  public String getName() {
    return name;
  }

  public AdType getAdType() {
    return adType;
  }

  public Map<String, Object> getParams() {
    return params;
  }
}
