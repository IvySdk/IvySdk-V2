package com.ivy.ads.events;

import com.ivy.ads.adapters.BaseAdapter;
import com.ivy.base.track.ITrack;

public class NonRewardedEventHandler extends BaseEventHandler {
    public NonRewardedEventHandler(ITrack eventLogger) {
        super(eventLogger);
    }

    public void fetchCalled(BaseAdapter adapter) {
        String eventName = "sdk_ad_request";
        EventParams params = new EventParams();
        params.putAll(adapter.getEventParams());
        params.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
        params.addParam("ad_unit", adapter.getPlacementId());
        params.addParam("ad_format", "interstitial");
        String displayedTag = adapter.getDisplayedTag();
        if (displayedTag != null) {
            params.addParam(EventParams.PARAM_LABEL, displayedTag);
        }
        logEvent(eventName, params, this.eventLogger);
    }

    public void showCalled(BaseAdapter adapter) {
    }

    public void timeoutCalled(BaseAdapter adapter) {
    }

    public void onAdLoadSuccessCalled(BaseAdapter adapter) {
        String eventName = "sdk_ad_loaded";
        EventParams params = new EventParams();
        params.putAll(adapter.getEventParams());
        params.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
        params.addParam("ad_unit", adapter.getPlacementId());
        params.addParam(EventParams.PARAM_MEDIATION, adapter.getMediation());
        params.addParam("ad_format", "interstitial");
        String displayedTag = adapter.getDisplayedTag();
        if (displayedTag != null) {
            params.addParam(EventParams.PARAM_LABEL, displayedTag);
        }
        logEvent(eventName, params, this.eventLogger);
    }

    public void onAdLoadFailCalled(BaseAdapter adapter, String failReason) {
        EventParams eventParams = new EventParams();
        eventParams.putAll(adapter.getEventParams());

        eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
        eventParams.addParam("ad_unit", adapter.getPlacementId());
        eventParams.addParam("ad_format", "interstitial");
        eventParams.addParam("fail_reason", failReason);
        String displayedTag = adapter.getDisplayedTag();
        if (displayedTag != null) {
            eventParams.addParam(EventParams.PARAM_LABEL, displayedTag);
        }
        logEvent("sdk_ad_load_failed", eventParams, this.eventLogger);
    }

    public void onAdShowSuccessCalled(BaseAdapter adapter) {
        String eventName = "sdk_ad_shown";
        EventParams params = new EventParams();
        params.putAll(adapter.getEventParams());
        params.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
        params.addParam("ad_unit", adapter.getPlacementId());
        params.addParam(EventParams.PARAM_MEDIATION, adapter.getMediation());
        params.addParam("ad_format", "interstitial");
        String displayedTag = adapter.getDisplayedTag();
        if (displayedTag != null) {
            params.addParam(EventParams.PARAM_LABEL, displayedTag);
        }
        logEvent(eventName, params, this.eventLogger);
    }

    public void onAdShowFailCalled(BaseAdapter adapter) {
        String eventName = "sdk_ad_show_failed";
        EventParams params = new EventParams();
        params.putAll(adapter.getEventParams());
        params.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
        params.addParam("ad_unit", adapter.getPlacementId());
        params.addParam(EventParams.PARAM_MEDIATION, adapter.getMediation());
        params.addParam("ad_format", "interstitial");
        String displayedTag = adapter.getDisplayedTag();
        if (displayedTag != null) {
            params.addParam(EventParams.PARAM_LABEL, displayedTag);
        }
        logEvent(eventName, params, this.eventLogger);
    }

    public void onAdClickCalled(BaseAdapter adapter) {
        String eventName = "sdk_ad_clicked";
        EventParams params = new EventParams();
        params.putAll(adapter.getEventParams());
        params.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
        params.addParam("ad_unit", adapter.getPlacementId());
        params.addParam(EventParams.PARAM_SHOWNTIME, adapter.getShowTimeMs());
        params.addParam(EventParams.PARAM_MEDIATION, adapter.getMediation());
        params.addParam("ad_format", "interstitial");
        String displayedTag = adapter.getDisplayedTag();
        if (displayedTag != null) {
            params.addParam(EventParams.PARAM_LABEL, displayedTag);
        }
        logEvent(eventName, params, this.eventLogger);
    }

    public void onAdClosedCalled(BaseAdapter adapter, boolean isReward) {
        EventParams eventParams = new EventParams();
        eventParams.putAll(adapter.getEventParams());
        eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
        eventParams.addParam("ad_unit", adapter.getPlacementId());
        eventParams.addParam(EventParams.PARAM_SHOWNTIME, adapter.getShowTimeMs());
        eventParams.addParam(EventParams.PARAM_MEDIATION, adapter.getMediation());
        eventParams.addParam("ad_format", "interstitial");
        String displayedTag = adapter.getDisplayedTag();
        if (displayedTag != null) {
            eventParams.addParam(EventParams.PARAM_LABEL, displayedTag);
        }
        logEvent("sdk_ad_closed", eventParams, this.eventLogger);
    }
}
