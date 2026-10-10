package com.ivy.ads;

import androidx.annotation.NonNull;

import com.bytedance.sdk.openadsdk.api.model.PAGAdEcpmInfo;
import com.bytedance.sdk.openadsdk.api.model.PAGErrorModel;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardItem;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdInteractionCallback;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdLoadCallback;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest;
import com.ivy.base.ActivityUtil;

public class PangleRewardedAd extends PangleBaseAd {
    private PAGRewardedAd ad;
    private boolean rewarded;

    @Override
    protected void onInited() {

    }

    @Override
    protected void loadAd() {
        PAGRewardedAd.loadAd(adUnit, new PAGRewardedRequest(), new PAGRewardedAdLoadCallback() {
            @Override
            public void onError(@NonNull PAGErrorModel pagErrorModel) {
                isLoading = false;
                ad = null;
                price = 0;
                if (loadCallback != null)
                    loadCallback.onLoadFailed(PangleRewardedAd.this, pagErrorModel.getErrorCode(), pagErrorModel.getErrorMessage());
            }

            @Override
            public void onAdLoaded(PAGRewardedAd pagRewardedAd) {
                isLoading = false;
                ad = pagRewardedAd;
                PAGAdEcpmInfo winEcpm = ad.getWinEcpm();
                if (winEcpm != null) {
                    try {
                        price = Double.parseDouble(winEcpm.getRevenue());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (loadCallback != null) loadCallback.onLoadSuccess(PangleRewardedAd.this);
            }
        });
    }

    @Override
    public boolean isReady() {
        return ad != null && ad.isReady() && !isShowing;
    }

    @Override
    public void show(AdShowCallback callback) {
        if (!isReady()) {
            callback.onAdShowFailed(this, -1, "Ad not ready");
            return;
        }

        showCallback = callback;
        isShowing = true;
        rewarded = false;
        ad.setAdInteractionCallback(new PAGRewardedAdInteractionCallback() {
            @Override
            public void onAdShowed() {
                ad = null;
                if (callback != null) callback.onAdDisplayed(PangleRewardedAd.this);
            }

            @Override
            public void onAdReturnRevenue(PAGAdEcpmInfo pagAdEcpmInfo) {
                if (pagAdEcpmInfo != null) {
                    try {
                        price = Double.parseDouble(pagAdEcpmInfo.getRevenue());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (revenueCallback != null) revenueCallback.onAdRevenuePaid(PangleRewardedAd.this);
            }

            @Override
            public void onAdClicked() {
                if (callback != null) callback.onAdClicked(PangleRewardedAd.this);
            }

            @Override
            public void onUserEarnedReward(PAGRewardItem pagRewardItem) {
                rewarded = true;
            }

            @Override
            public void onAdDismissed() {
                isShowing = false;
                ad = null;
                if (callback != null) callback.onAdClosed(PangleRewardedAd.this, rewarded);
            }

            @Override
            public void onAdShowFailed(@NonNull PAGErrorModel pagErrorModel) {
                isShowing = false;
                ad = null;
                if (callback != null)
                    callback.onAdShowFailed(PangleRewardedAd.this, pagErrorModel.getErrorCode(), pagErrorModel.getErrorMessage());
            }
        });
        ad.show(ActivityUtil.getInstance().getActivity());
    }
}
