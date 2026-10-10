package com.ivy.crosspromotion;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Maintains an independent smooth weighted round-robin sequence for each ad format. */
final class SmoothWeightedSelector {
    private final Map<CrossPromotionConfig.Format, Map<String, Long>> currentWeights =
            new HashMap<>();
    private final Map<CrossPromotionConfig.Format, String> lastSelectedIds = new HashMap<>();

    synchronized String select(
            CrossPromotionConfig.Format format,
            LinkedHashMap<String, Integer> candidates
    ) {
        if (candidates.isEmpty()) {
            return null;
        }

        Map<String, Long> previousWeights = currentWeights.get(format);
        Map<String, Long> nextWeights = new HashMap<>();
        String lastSelectedId = lastSelectedIds.get(format);
        String selectedId = null;
        long selectedWeight = Long.MIN_VALUE;
        long totalWeight = 0;

        for (Map.Entry<String, Integer> candidate : candidates.entrySet()) {
            String id = candidate.getKey();
            int weight = Math.max(1, candidate.getValue());
            Long previousWeight = previousWeights == null ? null : previousWeights.get(id);
            long currentWeight = (previousWeight == null ? 0L : previousWeight) + weight;
            nextWeights.put(id, currentWeight);
            totalWeight += weight;

            if (currentWeight > selectedWeight
                    || (currentWeight == selectedWeight
                    && selectedId != null
                    && selectedId.equals(lastSelectedId)
                    && !id.equals(lastSelectedId))) {
                selectedId = id;
                selectedWeight = currentWeight;
            }
        }

        nextWeights.put(selectedId, nextWeights.get(selectedId) - totalWeight);
        currentWeights.put(format, nextWeights);
        lastSelectedIds.put(format, selectedId);
        return selectedId;
    }

    synchronized void clear() {
        currentWeights.clear();
        lastSelectedIds.clear();
    }
}
