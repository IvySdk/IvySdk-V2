package com.ivy.adstrategy.scheduler;

import com.ivy.adstrategy.slot.AdSlotState;

import java.util.List;

public interface InventoryProvider {
  List<AdSlotState> getSlotStates();
}
