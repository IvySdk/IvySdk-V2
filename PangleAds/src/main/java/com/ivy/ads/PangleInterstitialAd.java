package com.ivy.ads;

import androidx.annotation.NonNull;

import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdInteractionCallback;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadCallback;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest;
import com.bytedance.sdk.openadsdk.api.model.PAGAdEcpmInfo;
import com.bytedance.sdk.openadsdk.api.model.PAGErrorModel;
import com.ivy.base.ActivityUtil;

public class PangleInterstitialAd extends PangleBaseAd {
    private PAGInterstitialAd ad;

    @Override
    protected void onInited() {

    }

    @Override
    protected void loadAd() {
        PAGInterstitialAd.loadAd(adUnit, new PAGInterstitialRequest(), new PAGInterstitialAdLoadCallback() {
            @Override
            public void onError(@NonNull PAGErrorModel pagErrorModel) {
                isLoading = false;
                price = 0;
                if (loadCallback != null)
                    loadCallback.onLoadFailed(PangleInterstitialAd.this, pagErrorModel.getErrorCode(), pagErrorModel.getErrorMessage());
            }

            @Override
            public void onAdLoaded(PAGInterstitialAd pagInterstitialAd) {
                isLoading = false;
                ad = pagInterstitialAd;
                PAGAdEcpmInfo winEcpm = ad.getWinEcpm();
                if (winEcpm != null) {
                    try {
                        PangleInterstitialAd.this.price = Double.parseDouble(winEcpm.getRevenue());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (loadCallback != null) loadCallback.onLoadSuccess(PangleInterstitialAd.this);
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
            return;
        }
        isShowing = true;
        showCallback = callback;
        ad.setAdInteractionCallback(new PAGInterstitialAdInteractionCallback() {
            @Override
            public void onAdShowed() {
                ad = null;
                if (callback != null) callback.onAdDisplayed(PangleInterstitialAd.this);
            }

            @Override
            public void onAdReturnRevenue(PAGAdEcpmInfo pagAdEcpmInfo) {
                if (pagAdEcpmInfo != null) {
                    try {
                        PangleInterstitialAd.this.price = Double.parseDouble(pagAdEcpmInfo.getRevenue());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (revenueCallback != null)
                    revenueCallback.onAdRevenuePaid(PangleInterstitialAd.this);
            }

            @Override
            public void onAdClicked() {
                if (callback != null) callback.onAdClicked(PangleInterstitialAd.this);
            }

            @Override
            public void onAdDismissed() {
                isShowing = false;
                ad = null;
                if (callback != null) callback.onAdClosed(PangleInterstitialAd.this, false);
            }

            @Override
            public void onAdShowFailed(@NonNull PAGErrorModel pagErrorModel) {
                isShowing = false;
                ad = null;
                if (callback != null)
                    callback.onAdShowFailed(PangleInterstitialAd.this, pagErrorModel.getErrorCode(), pagErrorModel.getErrorMessage());
            }
        });
        ad.show(ActivityUtil.getInstance().getActivity());
    }
}
