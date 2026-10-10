package com.ivy.ads.events;

import com.ivy.ads.adapters.BaseAdapter;
import com.ivy.base.track.ITrack;

public class RewardedEventHandler extends BaseEventHandler {
  public RewardedEventHandler(ITrack eventLogger) {
    super(eventLogger);
  }

  public void fetchCalled(BaseAdapter adapter) {
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());

    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam("ad_unit", adapter.getPlacementId());
    eventParams.addParam("ad_format", "rewarded");
    String displayedTag = adapter.getDisplayedTag();
    if (displayedTag != null) {
      eventParams.addParam(EventParams.PARAM_LABEL, displayedTag);
    }
    logEvent("sdk_ad_request", eventParams, this.eventLogger);
  }

  public void showCalled(BaseAdapter adapter) {
  }

  public void timeoutCalled(BaseAdapter adapter) {
  }

  public void onAdLoadSuccessCalled(BaseAdapter adapter) {
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());

    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam("ad_unit", adapter.getPlacementId());
    eventParams.addParam("ad_format", "rewarded");
    String displayedTag = adapter.getDisplayedTag();
    if (displayedTag != null) {
      eventParams.addParam(EventParams.PARAM_LABEL, displayedTag);
    }
    logEvent("sdk_ad_loaded", eventParams, this.eventLogger);
  }

  public void onAdLoadFailCalled(BaseAdapter adapter, String failReason) {
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());

    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam("ad_unit", adapter.getPlacementId());
    eventParams.addParam("ad_format", "rewarded");
    eventParams.addParam("fail_reason", failReason);
    String displayedTag = adapter.getDisplayedTag();
    if (displayedTag != null) {
      eventParams.addParam(EventParams.PARAM_LABEL, displayedTag);
    }
    logEvent("sdk_ad_load_failed", eventParams, this.eventLogger);
  }

  public void onAdShowSuccessCalled(BaseAdapter adapter) {
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());

    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam("ad_unit", adapter.getPlacementId());
    eventParams.addParam(EventParams.PARAM_MEDIATION, adapter.getMediation());
    eventParams.addParam("ad_format", "rewarded");
    String displayedTag = adapter.getDisplayedTag();
    if (displayedTag != null) {
      eventParams.addParam(EventParams.PARAM_LABEL, displayedTag);
    }
    logEvent("sdk_ad_shown", eventParams, this.eventLogger);
  }

  public void onAdShowFailCalled(BaseAdapter adapter) {
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());
    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam("ad_unit", adapter.getPlacementId());
    eventParams.addParam(EventParams.PARAM_REASON, adapter.getShowStatus().toString());
    eventParams.addParam(EventParams.PARAM_MEDIATION, adapter.getMediation());
    eventParams.addParam("ad_format", "rewarded");
    String displayedTag = adapter.getDisplayedTag();
    if (displayedTag != null) {
      eventParams.addParam(EventParams.PARAM_LABEL, displayedTag);
    }
    logEvent("sdk_ad_show_failed", eventParams, this.eventLogger);
  }

  public void onAdClickCalled(BaseAdapter adapter) {
  }

  public void onAdClosedCalled(BaseAdapter adapter, boolean isReward) {
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());
    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam("ad_unit", adapter.getPlacementId());
    eventParams.addParam(EventParams.PARAM_SHOWNTIME, adapter.getShowTimeMs());
    eventParams.addParam(EventParams.PARAM_MEDIATION, adapter.getMediation());
    eventParams.addParam("ad_format", "rewarded");
    String displayedTag = adapter.getDisplayedTag();
    if (displayedTag != null) {
      eventParams.addParam(EventParams.PARAM_LABEL, displayedTag);
    }
    if (isReward) {
      logEvent("sdk_ad_rewarded", eventParams, this.eventLogger);
    }
    logEvent("sdk_ad_closed", eventParams, this.eventLogger);
  }
}
