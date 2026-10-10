package com.ivy.adstrategy.slot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SlotRegistry {
  private final Map<String, SlotStateMachine> slots = new LinkedHashMap<>();

  public synchronized void register(AdSlot slot) {
    slots.put(slot.getSlotId(), new SlotStateMachine(slot));
  }

  public synchronized void registerAll(List<AdSlot> adSlots) {
    if (adSlots == null) {
      return;
    }
    for (AdSlot slot : adSlots) {
      register(slot);
    }
  }

  public synchronized SlotStateMachine get(String slotId) {
    return slots.get(slotId);
  }

  public synchronized List<SlotStateMachine> getAll() {
    return Collections.unmodifiableList(new ArrayList<>(slots.values()));
  }

  public synchronized boolean isEmpty() {
    return slots.isEmpty();
  }
}
