package com.ivy.ads;

import android.app.Activity;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.ivy.base.ad.AdType;

import java.util.concurrent.atomic.AtomicBoolean;

/** Banner presentation, refresh and suspension share one owner. */
final class BannerController {
    private static final String TAG = "hoho";
    private final Handler handler;
    private final BaseAd.AdShowCallback adShowCallback;
    private Activity context;
    private MyAdListener listener;
    private boolean pendingBanner = false;
    private int pendingBannerPos = -1;
    private boolean schedulerSuspendedBanner = false;
    private int schedulerSuspendedBannerPos = -1;
    private int currentBannerPos = -1;
    private FrameLayout bannerContainer = null;
    private BaseAd showingBannerAd = null;
    private long bannerShownTimeStamp = -1;
    private int bannerRefreshSeconds = 0;
    private final AtomicBoolean isClientCallShowBanner = new AtomicBoolean(false);

    BannerController(Handler handler, BaseAd.AdShowCallback adShowCallback) {
        this.handler = handler;
        this.adShowCallback = adShowCallback;
    }

    void attach(Activity context, MyAdListener listener) {
        this.context = context;
        this.listener = listener;
    }

    void onAdLoaded(BaseAd ad) {
        // 横幅广告加载成功后的特殊处理
        if (isPromotion(showingBannerAd) && !isPromotion(ad) && ad.isReady()) {
            handler.post(this::replacePromotionBannerIfPossible);
        } else if (pendingBanner) {
            pendingBanner = false;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    showBanner(pendingBannerPos);
                }
            });
        }
    }

    boolean isSuspended() {
        return schedulerSuspendedBanner;
    }

    void destroy() {
        pendingBanner = false;
        schedulerSuspendedBanner = true;
        schedulerSuspendedBannerPos = -1;
        closeBanner(false);
        if (bannerContainer != null && bannerContainer.getParent() instanceof ViewGroup) {
            ((ViewGroup) bannerContainer.getParent()).removeView(bannerContainer);
        }
        bannerContainer = null;
    }

    private int pos2Gravity(int pos) {
        return pos == 3 ? Gravity.TOP | Gravity.CENTER_HORIZONTAL : Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
    }

    private int dpToPx(float dp) {
        // 获取屏幕的密度缩放因子
        float scale = context.getResources().getDisplayMetrics().density;
        // dp 乘以缩放因子，并加上 0.5f 进行四舍五入
        return (int) (dp * scale + 0.5f);
    }

    private boolean isBannerShowEnoughTime() {
        return SystemClock.elapsedRealtime() - bannerShownTimeStamp >= bannerRefreshSeconds * 1000L;
    }

    private void tryRefresh(int seconds) {
        handler.postDelayed(() -> {
            if (pendingBanner) {
                showBanner(pendingBannerPos);
            }
        }, seconds * 1000L);
    }

    void showBanner(int pos) {
        BaseAd ad = AdCore.getHighestPriceAd(AdType.BANNER);
        if (ad != null) {
            if (!isBannerShowEnoughTime()) return;
            displayBanner(ad, pos);
        } else {
            pendingBanner = true;
            pendingBannerPos = pos;
        }
    }

    private void replacePromotionBannerIfPossible() {
        if (!isPromotion(showingBannerAd) || currentBannerPos < 0) {
            return;
        }
        BaseAd replacement = AdCore.getHighestPriceAd(AdType.BANNER);
        if (replacement != null && !isPromotion(replacement)) {
            displayBanner(replacement, currentBannerPos);
        }
    }

    private void displayBanner(BaseAd ad, int pos) {
        if (showingBannerAd != null) {
            closeBanner(true);
        }
        pendingBanner = false;
        currentBannerPos = pos;
        bannerRefreshSeconds = ad.getRefreshSeconds();
        if (bannerRefreshSeconds > 0 && !isPromotion(ad)) {
            tryRefresh(bannerRefreshSeconds);
        }
        bannerShownTimeStamp = SystemClock.elapsedRealtime();
        if (bannerContainer == null) {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(60),
                    pos2Gravity(pos));
            bannerContainer = new FrameLayout(context);
            context.addContentView(bannerContainer, layoutParams);
        }
        showingBannerAd = ad;
        ((FrameLayout.LayoutParams) bannerContainer.getLayoutParams()).gravity = pos2Gravity(pos);
        bannerContainer.removeAllViews();
        ad.show(adShowCallback);
        bannerContainer.addView(ad.getView(), bannerContainer.getLayoutParams());
        try {
            if (isClientCallShowBanner.compareAndSet(false, true)) {
                listener.onAdShowSuccess(
                        ad.getAdUnit(),
                        ad.getPlatform(),
                        ad.getNetworkName(),
                        ad.getSegments());
            }
        } catch (Throwable ignored) {
        }
    }

    static boolean isPromotion(BaseAd ad) {
        return ad != null && "promotion".equals(ad.getPlatform());
    }

    void closeBanner() {
        isClientCallShowBanner.set(false);
        closeBanner(true);
    }

    private void closeBanner(boolean clearSchedulerSuspend) {
        if (clearSchedulerSuspend) {
            schedulerSuspendedBanner = false;
            schedulerSuspendedBannerPos = -1;
        }
        pendingBanner = false;
        try {
            if (bannerContainer != null) {
                for (int i = bannerContainer.getChildCount() - 1; i >= 0; i--) {
                    View child = bannerContainer.getChildAt(i);
                    if (child != null) {
                        bannerContainer.removeView(child);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        if (showingBannerAd != null) {
            try {
                showingBannerAd.close();
            } catch (Throwable ignored) {
            } finally {
                showingBannerAd = null;
            }
        }
    }

    void suspendBannerByScheduler() {
        cancelBannerScheduledReloads();
        if (pendingBanner) {
            schedulerSuspendedBanner = true;
            schedulerSuspendedBannerPos = pendingBannerPos;
        } else if (showingBannerAd != null) {
            schedulerSuspendedBanner = true;
            schedulerSuspendedBannerPos = currentBannerPos;
        }
        Log.d(TAG, "scheduler suspend banner pos=" + schedulerSuspendedBannerPos);
        closeBanner(false);
    }

    void restoreSchedulerBanner() {
        if (!schedulerSuspendedBanner || schedulerSuspendedBannerPos < 0) {
            return;
        }
        int pos = schedulerSuspendedBannerPos;
        schedulerSuspendedBanner = false;
        schedulerSuspendedBannerPos = -1;
        Log.d(TAG, "scheduler restore banner pos=" + pos);
        if (AdCore.isReady(AdType.BANNER)) {
            showBanner(pos);
            return;
        } else {
            pendingBanner = true;
            pendingBannerPos = pos;
            AdCore.load(AdType.BANNER, null);
        }
    }

    private void cancelBannerScheduledReloads() {
        for (BaseAd ad : AdCore.getAds(AdType.BANNER)) {
            if (ad == null) {
                continue;
            }
            handler.removeCallbacks(ad);
            ad.delayed = false;
        }
    }
}
