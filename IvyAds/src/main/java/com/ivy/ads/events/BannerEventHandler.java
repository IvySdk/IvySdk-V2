package com.ivy.ads.events;

import androidx.annotation.NonNull;

import com.ivy.ads.adapters.BannerAdapter;
import com.ivy.ads.adapters.BaseAdapter;
import com.ivy.base.track.ITrack;

public class BannerEventHandler extends BaseEventHandler {
  public BannerEventHandler(ITrack eventLogger) {
    super(eventLogger);
  }

  public void fetchCalled(BaseAdapter adapter) {
    String eventName = "sdk_ad_request";
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());
    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam("ad_unit", adapter.getPlacementId());
    eventParams.addParam("ad_format", "banner");
    logEvent(eventName, eventParams, eventLogger);
  }

  public void showCalled(BaseAdapter adapter) {
  }

  public void timeoutCalled(BaseAdapter adapter) {
  }

  public void onAdLoadSuccessCalled(BaseAdapter adapter) {
    String eventName ="sdk_ad_loaded";
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());
    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam("ad_unit", adapter.getPlacementId());
    eventParams.addParam(EventParams.PARAM_MEDIATION, adapter.getMediation());
    eventParams.addParam("ad_format", "banner");
    logEvent(eventName, eventParams, eventLogger);
  }

  public void onAdLoadFailCalled(BaseAdapter adapter, String failReason) {
    String eventName = "sdk_ad_load_failed";
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());
    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam("ad_unit", adapter.getPlacementId());
    eventParams.addParam(EventParams.PARAM_MEDIATION, adapter.getMediation());
    eventParams.addParam("ad_format", "banner");
    eventParams.addParam("fail_reason", failReason);
    logEvent(eventName, eventParams, eventLogger);
  }

  public void onAdShowSuccessCalled(@NonNull BaseAdapter adapter) {
    String eventName = "sdk_ad_shown";
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());
    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam(EventParams.PARAM_LOADTIME, adapter.getLoadTimeMs());
    eventParams.addParam("ad_unit", adapter.getPlacementId());
    eventParams.addParam(EventParams.PARAM_MEDIATION, adapter.getMediation());
    eventParams.addParam("ad_format", "banner");
    logEvent(eventName, eventParams, eventLogger);
  }

  public void onAdShowFailCalled(BaseAdapter adapter) {
  }

  public void onAdClickCalled(BaseAdapter adapter) {
    String eventName = "sdk_ad_clicked";
    EventParams eventParams = new EventParams();
    eventParams.putAll(adapter.getEventParams());
    eventParams.addParam(EventParams.PARAM_PROVIDER, adapter.getName());
    eventParams.addParam(EventParams.PARAM_LOADTIME, adapter.getLoadTimeMs());
    eventParams.addParam("ad_unit", adapter.getPlacementId());
    eventParams.addParam(EventParams.PARAM_MEDIATION, adapter.getMediation());
    eventParams.addParam("ad_format", "banner");
    logEvent(eventName, eventParams, eventLogger);
  }

  public void onAdClosedCalled(BaseAdapter adapter, boolean isReward) {
  }

  public void onBannerDisplayed(BannerAdapter adapter) {
  }
}
