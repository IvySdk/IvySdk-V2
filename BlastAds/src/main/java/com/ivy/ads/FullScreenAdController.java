package com.ivy.ads;

import com.ivy.base.ad.AdType;

import java.util.ArrayList;
import java.util.List;

/** One fullscreen opportunity at a time, including its entire fallback chain. */
final class FullScreenAdController {
    interface Host {
        long now();
        void post(Runnable task);
        List<BaseAd> getAds(AdType type);
        void onStarted(BaseAd ad);
        void onFinished(BaseAd ad);
        void onDisplayed(BaseAd ad);
        void onClosed(BaseAd ad, boolean rewarded);
        void onClicked(BaseAd ad);
        void onFailed(BaseAd ad, int code, String message, boolean hasFallback);
    }

    private final Host host;
    private final List<BaseAd> attempted = new ArrayList<>();
    private Attempt active;
    private long startedAt;
    private boolean stopped;

    FullScreenAdController(Host host) {
        this.host = host;
    }

    boolean isBusy() {
        return active != null;
    }

    boolean show(AdType type, Runnable beforeShow) {
        if (stopped || isBusy()) return false;
        BaseAd ad = select(host.getAds(type), null);
        if (ad == null) return false;
        attempted.clear();
        startedAt = host.now();
        active = new Attempt(ad);
        beforeShow.run();
        start(active);
        return true;
    }

    void stop() {
        stopped = true;
        active = null;
        attempted.clear();
    }

    // Queries have no auction notifications or other side effects.
    static BaseAd select(List<BaseAd> ads, List<BaseAd> excluded) {
        BaseAd best = null;
        for (BaseAd ad : ads) {
            if (ad.destroyed || !ad.isReady() || (excluded != null && excluded.contains(ad))) continue;
            if (best == null || (best.isSecondary() && !ad.isSecondary())
                    || (best.isSecondary() == ad.isSecondary() && Double.compare(ad.getPrice(), best.getPrice()) > 0)) {
                best = ad;
            }
        }
        return best;
    }

    static void notifySelection(List<BaseAd> ads, BaseAd selected) {
        for (BaseAd ad : ads) {
            if (ad != selected && !ad.destroyed && ad.isReady()) {
                ad.onLoss(selected.getNetworkName(), (float) selected.getPrice());
            }
        }
    }

    private void start(Attempt attempt) {
        if (!attempt.isCurrent()) return;
        attempted.add(attempt.ad);
        host.onStarted(attempt.ad);
        if (!attempt.ad.isReady()) {
            attempt.onAdShowFailed(attempt.ad, -1, "Ad not ready");
            return;
        }
        notifySelection(host.getAds(attempt.ad.getAdType()), attempt.ad);
        try {
            attempt.ad.show(attempt);
        } catch (RuntimeException error) {
            attempt.onAdShowFailed(attempt.ad, -1,
                    "SDK show exception: " + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private final class Attempt implements BaseAd.AdShowCallback {
        private final BaseAd ad;
        private boolean displayed;

        Attempt(BaseAd ad) { this.ad = ad; }
        boolean isCurrent() { return !stopped && active == this; }

        @Override public void onAdDisplayed(BaseAd ignored) {
            host.post(() -> {
                if (!isCurrent() || displayed) return;
                displayed = true;
                host.onDisplayed(ad);
            });
        }

        @Override public void onAdClosed(BaseAd ignored, boolean rewarded) {
            host.post(() -> {
                if (!isCurrent()) return;
                active = null;
                attempted.clear();
                ad.isShowing = false;
                host.onFinished(ad);
                host.onClosed(ad, rewarded);
            });
        }

        @Override public void onAdClicked(BaseAd ignored) {
            host.post(() -> { if (isCurrent()) host.onClicked(ad); });
        }

        @Override public void onAdShowFailed(BaseAd ignored, int code, String message) {
            host.post(() -> {
                if (!isCurrent()) return;
                ad.isShowing = false;
                BaseAd fallback = !displayed && host.now() - startedAt < 5000L
                        ? select(host.getAds(ad.getAdType()), attempted) : null;
                // Reserve the next attempt before invoking any potentially reentrant callbacks.
                Attempt next = fallback == null ? null : new Attempt(fallback);
                active = next;
                if (next == null) attempted.clear();
                host.onFinished(ad);
                host.onFailed(ad, code, message, next != null);
                if (next != null) start(next);
            });
        }
    }
}
