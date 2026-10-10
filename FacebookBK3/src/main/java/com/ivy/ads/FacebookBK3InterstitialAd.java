package com.ivy.ads;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.biddingkit.gen.BidWithNotification;
import com.facebook.biddingkit.gen.FacebookAdBidFormat;

public class FacebookBK3InterstitialAd extends FacebookBK3BaseAd {
    private InterstitialAd interstitialAd;

    private final InterstitialAdListener interstitialAdListener = new InterstitialAdListener() {
        @Override
        public void onInterstitialDisplayed(Ad ad) {
            notifyWin();
            if (showCallback != null) {
                showCallback.onAdDisplayed(FacebookBK3InterstitialAd.this);
            }
        }

        @Override
        public void onInterstitialDismissed(Ad ad) {
            isShowing = false;
            destroyAd();
            if (showCallback != null) {
                showCallback.onAdClosed(FacebookBK3InterstitialAd.this, false);
            }
        }

        @Override
        public void onError(Ad ad, AdError adError) {
            boolean showing = isShowing;
            isLoading = false;
            isShowing = false;
            destroyAd();
            if (showing) {
                if (showCallback != null) {
                    showCallback.onAdShowFailed(FacebookBK3InterstitialAd.this, adError.getErrorCode(), adError.getErrorMessage());
                }
            } else if (loadCallback != null) {
                loadCallback.onLoadFailed(FacebookBK3InterstitialAd.this, adError.getErrorCode(), adError.getErrorMessage());
            }
        }

        @Override
        public void onAdLoaded(Ad ad) {
            isLoading = false;
            if (loadCallback != null) {
                loadCallback.onLoadSuccess(FacebookBK3InterstitialAd.this);
            }
        }

        @Override
        public void onAdClicked(Ad ad) {
            if (showCallback != null) {
                showCallback.onAdClicked(FacebookBK3InterstitialAd.this);
            }
        }

        @Override
        public void onLoggingImpression(Ad ad) {
            if (revenueCallback != null) {
                revenueCallback.onAdRevenuePaid(FacebookBK3InterstitialAd.this);
            }
        }
    };

    @Override
    protected void loadAd() {
        requestBid(FacebookAdBidFormat.INTERSTITIAL, new FacebookBK3BidRequester.BidCallback() {
            @Override
            public void onSuccess(BidWithNotification bid) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            onBidLoaded(bid);
                            interstitialAd = new InterstitialAd(context, adUnit);
                            InterstitialAd.InterstitialLoadAdConfig config = interstitialAd.buildLoadAdConfig()
                                    .withBid(bid.getPayload())
                                    .withAdListener(interstitialAdListener)
                                    .build();
                            interstitialAd.loadAd(config);
                        } catch (Throwable e) {
                            isLoading = false;
                            destroyAd();
                            if (loadCallback != null) {
                                loadCallback.onLoadFailed(FacebookBK3InterstitialAd.this, -6, "Exception loading Facebook BK3 interstitial: " + e.getMessage());
                            }
                        }
                    }
                });
            }

            @Override
            public void onFailure(int code, String message) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isLoading = false;
                        if (loadCallback != null) {
                            loadCallback.onLoadFailed(FacebookBK3InterstitialAd.this, code, message);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void show(AdShowCallback callback) {
        if (!isReady()) {
            callback.onAdShowFailed(this, -1, "Ad not ready");
            return;
        }

        showCallback = callback;
        try {
            isShowing = true;
            interstitialAd.show();
        } catch (Throwable e) {
            isShowing = false;
            callback.onAdShowFailed(this, -7, "Exception showing Facebook BK3 interstitial: " + e.getMessage());
        }
    }

    @Override
    public boolean isReady() {
        return !isShowing && interstitialAd != null && interstitialAd.isAdLoaded() && !isLoading;
    }

    public void destroy() {
        destroyAd();
        loadCallback = null;
        showCallback = null;
        revenueCallback = null;
        isLoading = false;
        isShowing = false;
    }

    private void destroyAd() {
        if (interstitialAd != null) {
            try {
                interstitialAd.destroy();
            } catch (Throwable ignored) {
            }
            interstitialAd = null;
        }
    }
}
