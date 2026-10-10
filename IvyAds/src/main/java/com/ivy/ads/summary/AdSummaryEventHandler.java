package com.ivy.ads.summary;

import android.app.Activity;

import com.ivy.base.ad.ivy.IvyAdType;
import com.ivy.base.track.ITrack;

public interface AdSummaryEventHandler {
  void init(Activity activity, ITrack eventTracker);

  void onImpression(IvyAdType adType, String str, float revenue);

  void onAdPaid(IvyAdType adType, String str, float revenue);

  void onLoad(IvyAdType adType, String str);

  void onRequest(IvyAdType adType, String str);

  void onAction(IvyAdType adType, String str);

  void saveAdSummaryData();

  long[] getSummaryData(IvyAdType adType, String provider);
}
