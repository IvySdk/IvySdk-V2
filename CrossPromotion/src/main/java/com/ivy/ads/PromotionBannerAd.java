package com.ivy.ads;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.ivy.crosspromotion.CrossPromoBannerView;
import com.ivy.crosspromotion.CrossPromotion;
import com.ivy.crosspromotion.CrossPromotionConfig;

public final class PromotionBannerAd extends PromotionBaseAd {
    private static final int DEFAULT_REFRESH_SECONDS = 30;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = this::refreshBanner;
    private FrameLayout container;
    private CrossPromoBannerView bannerView;
    private boolean shown;

    @Override
    protected CrossPromotionConfig.Format promotionFormat() {
        return CrossPromotionConfig.Format.BANNER;
    }

    @Override
    protected void loadAd() {
        handler.removeCallbacks(refreshRunnable);
        shown = false;
        isShowing = false;
        if (container == null) {
            container = new FrameLayout(context);
        }
        CrossPromoBannerView loadedView = CrossPromotion.getInstance().createBanner(context);
        isLoading = false;
        if (loadedView != null) {
            replaceBannerView(loadedView, false);
            if (loadCallback != null) {
                loadCallback.onLoadSuccess(this);
            }
        } else {
            if (loadCallback != null) {
                loadCallback.onLoadFailed(this, 204, "No eligible banner resource");
            }
        }
    }

    @Override
    public boolean isReady() {
        return !shown && (bannerView != null || CrossPromotion.getInstance().isReady(promotionFormat()));
    }

    @Override
    public void show(AdShowCallback callback) {
        if (container == null) {
            container = new FrameLayout(context);
        }
        if (bannerView == null && CrossPromotion.getInstance().isReady(promotionFormat())) {
            CrossPromoBannerView loadedView = CrossPromotion.getInstance().createBanner(context);
            if (loadedView != null) {
                replaceBannerView(loadedView, false);
            }
        }
        if (!isReady() || bannerView == null) {
            showNotReady(callback);
            return;
        }
        showCallback = callback;
        shown = true;
        isShowing = true;
        container.setVisibility(View.VISIBLE);
        bannerView.onAdShown();
        scheduleRefresh();
    }

    @Override
    protected void close() {
        shown = false;
        isShowing = false;
        handler.removeCallbacks(refreshRunnable);
        if (container != null) {
            container.setVisibility(View.GONE);
        }
    }

    @Override
    public View getView() {
        return container;
    }

    private void scheduleRefresh() {
        handler.removeCallbacks(refreshRunnable);
        int seconds = getRefreshSeconds() > 0 ? getRefreshSeconds() : DEFAULT_REFRESH_SECONDS;
        handler.postDelayed(refreshRunnable, seconds * 1000L);
    }

    private void refreshBanner() {
        if (!shown || container == null) {
            return;
        }
        CrossPromoBannerView loadedView = CrossPromotion.getInstance().createBanner(context);
        if (loadedView != null) {
            replaceBannerView(loadedView, true);
        }
        scheduleRefresh();
    }

    private void replaceBannerView(CrossPromoBannerView loadedView, boolean recordImpression) {
        CrossPromoBannerView previousView = bannerView;
        bannerView = loadedView;
        if (loadedView.getParent() instanceof ViewGroup) {
            ((ViewGroup) loadedView.getParent()).removeView(loadedView);
        }
        if (recordImpression) {
            loadedView.onAdShown();
        }
        container.addView(loadedView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        if (previousView != null) {
            container.removeView(previousView);
        }
    }
}
