package com.ivy.base.ad.ivy;

public interface IvyAdCallbacks {
    void onAdClicked(IvyAdInfo adInfo);

    void onAdClosed(IvyAdInfo adInfo, boolean gotReward);

    void onAdLoadFail(IvyAdType adInfo);

    void onAdLoadSuccess(IvyAdInfo adInfo);

    void onAdShowFail(IvyAdType adInfo);

    void onAdShowSuccess(IvyAdInfo adInfo);
}
