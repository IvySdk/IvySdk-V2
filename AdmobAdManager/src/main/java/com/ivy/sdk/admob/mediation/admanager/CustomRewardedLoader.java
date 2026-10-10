package com.ivy.sdk.admob.mediation.admanager;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import org.json.JSONObject;

public class CustomRewardedLoader extends FullScreenContentCallback implements MediationRewardedAd {

    private final MediationRewardedAdConfiguration adConfiguration;
    private final MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback;

    private MediationRewardedAdCallback mediationRewardedAdCallback;
    private RewardedAd rewardedAd;

    public CustomRewardedLoader(
            MediationRewardedAdConfiguration adConfiguration,
            MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback
    ) {
        this.adConfiguration = adConfiguration;
        this.callback = callback;
    }

    public void loadAd() {
        String placementId = null;
        try {
            String serverParameter = adConfiguration.getServerParameters().getString("parameter");
            placementId = new JSONObject(serverParameter).getString(CustomAdEvent.KEY_PLACEMENT_ID);
        } catch (Exception e) {
            // ignore
        }

        if (placementId != null && !placementId.isEmpty()) {
            AdRequest adRequest = new AdRequest.Builder().build();
            RewardedAd.load(
                    adConfiguration.getContext(),
                    placementId,
                    adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(LoadAdError loadAdError) {
                            super.onAdFailedToLoad(loadAdError);
                            callback.onFailure(loadAdError);
                        }

                        @Override
                        public void onAdLoaded(RewardedAd rewardedAd) {
                            super.onAdLoaded(rewardedAd);
                            CustomRewardedLoader.this.rewardedAd = rewardedAd;
                            mediationRewardedAdCallback = callback.onSuccess(CustomRewardedLoader.this);
                        }
                    }
            );
        } else {
            callback.onFailure(new AdError(
                    CustomAdEvent.ERROR_NULL_CONTEXT,
                    "Google ad manager requires a non-null ad Id.",
                    CustomAdEvent.DOMAIN
            ));
        }
    }

    @Override
    public void showAd(Context context) {
        if (rewardedAd != null) {
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                rewardedAd.setFullScreenContentCallback(this);
                rewardedAd.show(activity, rewardItem -> {
                    // 用户观看完广告获得奖励时触发
                    // 如果要触发回调，请取消注释以下行：
                    // if (mediationRewardedAdCallback != null) mediationRewardedAdCallback.onUserEarnedReward();
                });
            } else {
                if (mediationRewardedAdCallback != null) {
                    mediationRewardedAdCallback.onAdFailedToShow(
                            new AdError(
                                    CustomAdEvent.ERROR_AD_NOT_READY,
                                    "Activity instance required to show rewarded ad",
                                    CustomAdEvent.DOMAIN
                            )
                    );
                }
            }
        } else {
            if (mediationRewardedAdCallback != null) {
                mediationRewardedAdCallback.onAdFailedToShow(
                        new AdError(
                                CustomAdEvent.ERROR_AD_NOT_READY,
                                "Rewarded ad not ready",
                                CustomAdEvent.DOMAIN
                        )
                );
            }
        }
    }

    @Override
    public void onAdFailedToShowFullScreenContent(AdError adError) {
        super.onAdFailedToShowFullScreenContent(adError);
        if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.onAdFailedToShow(adError);
        }
    }

    @Override
    public void onAdShowedFullScreenContent() {
        super.onAdShowedFullScreenContent();
        if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.onAdOpened();
        }
    }

    @Override
    public void onAdImpression() {
        super.onAdImpression();
        if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.reportAdImpression();
        }
    }

    @Override
    public void onAdDismissedFullScreenContent() {
        super.onAdDismissedFullScreenContent();
        if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.onAdClosed();
        }
    }

    @Override
    public void onAdClicked() {
        super.onAdClicked();
        if (mediationRewardedAdCallback != null) {
            mediationRewardedAdCallback.reportAdClicked();
        }
    }
}

