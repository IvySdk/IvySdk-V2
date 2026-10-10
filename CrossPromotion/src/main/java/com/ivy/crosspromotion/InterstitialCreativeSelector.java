package com.ivy.crosspromotion;

import java.util.IdentityHashMap;
import java.util.Map;

/** Alternates ready image and video creatives independently for each ad orientation. */
final class InterstitialCreativeSelector {
    private final Map<CrossPromotionConfig.Ad, boolean[]> lastVideoSelections =
            new IdentityHashMap<>();

    synchronized boolean selectVideo(
            CrossPromotionConfig.Ad ad,
            boolean landscape,
            boolean videoReady,
            boolean imageReady
    ) {
        if (!videoReady) {
            return false;
        }
        if (!imageReady) {
            return true;
        }

        boolean[] lastSelections = lastVideoSelections.get(ad);
        if (lastSelections == null) {
            lastSelections = new boolean[2];
            lastVideoSelections.put(ad, lastSelections);
        }
        int orientationIndex = landscape ? 1 : 0;
        boolean selectVideo = !lastSelections[orientationIndex];
        lastSelections[orientationIndex] = selectVideo;
        return selectVideo;
    }

    synchronized void clear() {
        lastVideoSelections.clear();
    }
}
