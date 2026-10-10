package com.ivy.ads;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdListener;
import com.facebook.biddingkit.gen.BidWithNotification;
import com.facebook.biddingkit.gen.FacebookAdBidFormat;

public class FacebookBK3RewardedAd extends FacebookBK3BaseAd {
    private RewardedVideoAd rewardedAd;
    private boolean isRewarded;

    private final RewardedVideoAdListener rewardedAdListener = new RewardedVideoAdListener() {
        @Override
        public void onRewardedVideoCompleted() {
            isRewarded = true;
        }

        @Override
        public void onRewardedVideoClosed() {
            isShowing = false;
            destroyAd();
            if (showCallback != null) {
                showCallback.onAdClosed(FacebookBK3RewardedAd.this, isRewarded);
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
                    showCallback.onAdShowFailed(FacebookBK3RewardedAd.this, adError.getErrorCode(), adError.getErrorMessage());
                }
            } else if (loadCallback != null) {
                loadCallback.onLoadFailed(FacebookBK3RewardedAd.this, adError.getErrorCode(), adError.getErrorMessage());
            }
        }

        @Override
        public void onAdLoaded(Ad ad) {
            isLoading = false;
            if (loadCallback != null) {
                loadCallback.onLoadSuccess(FacebookBK3RewardedAd.this);
            }
        }

        @Override
        public void onAdClicked(Ad ad) {
            if (showCallback != null) {
                showCallback.onAdClicked(FacebookBK3RewardedAd.this);
            }
        }

        @Override
        public void onLoggingImpression(Ad ad) {
            notifyWin();
            if (revenueCallback != null) {
                revenueCallback.onAdRevenuePaid(FacebookBK3RewardedAd.this);
            }
            if (showCallback != null) {
                showCallback.onAdDisplayed(FacebookBK3RewardedAd.this);
            }
        }
    };

    @Override
    protected void loadAd() {
        requestBid(FacebookAdBidFormat.REWARDED_VIDEO, new FacebookBK3BidRequester.BidCallback() {
            @Override
            public void onSuccess(BidWithNotification bid) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            onBidLoaded(bid);
                            rewardedAd = new RewardedVideoAd(context, adUnit);
                            RewardedVideoAd.RewardedVideoLoadAdConfig config = rewardedAd.buildLoadAdConfig()
                                    .withBid(bid.getPayload())
                                    .withAdListener(rewardedAdListener)
                                    .build();
                            rewardedAd.loadAd(config);
                        } catch (Throwable e) {
                            isLoading = false;
                            destroyAd();
                            if (loadCallback != null) {
                                loadCallback.onLoadFailed(FacebookBK3RewardedAd.this, -6, "Exception loading Facebook BK3 rewarded ad: " + e.getMessage());
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
                            loadCallback.onLoadFailed(FacebookBK3RewardedAd.this, code, message);
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
        isRewarded = false;
        try {
            isShowing = true;
            if (!rewardedAd.show()) {
                isShowing = false;
                callback.onAdShowFailed(this, -7, "Facebook BK3 rewarded ad failed to show");
            }
        } catch (Throwable e) {
            isShowing = false;
            callback.onAdShowFailed(this, -7, "Exception showing Facebook BK3 rewarded ad: " + e.getMessage());
        }
    }

    @Override
    public boolean isReady() {
        return !isShowing && rewardedAd != null && rewardedAd.isAdLoaded() && !rewardedAd.isAdInvalidated() && !isLoading;
    }

    public void destroy() {
        destroyAd();
        loadCallback = null;
        showCallback = null;
        revenueCallback = null;
        isLoading = false;
        isShowing = false;
        isRewarded = false;
    }

    private void destroyAd() {
        if (rewardedAd != null) {
            try {
                rewardedAd.destroy();
            } catch (Throwable ignored) {
            }
            rewardedAd = null;
        }
    }
}
