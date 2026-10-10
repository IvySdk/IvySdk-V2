package com.ivy.base.ad;

import java.util.Map;

public abstract class IAdListener {
    public abstract void onInited();

    public abstract void onAdLoadSuccess(AdType adType);

    public abstract void onAdLoadFailure(AdType adType, String reason);

    public abstract void onAdShowSuccess(AdType adType, String tag, String placement);

    public abstract void onAdShowFailed(AdType adType, String reason, String tag, String placement);

    public abstract void onAdClicked(AdType adType, String tag, String placement);

    public abstract void onAdClosed(AdType adType, boolean gotReward, String tag, String placement);

    public abstract void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, boolean af);

}