package com.ivy.ads.adapters;

import com.ivy.base.ad.ivy.IvyAdInfo;
import com.ivy.base.ad.ivy.IvyAdType;
import com.ivy.base.ad.ivy.IvyAdapterInfo;

public interface AdOpenCloseCallback {
  void onAdClicked(IvyAdapterInfo o7AdapterInfo);

  void onAdClosed(IvyAdapterInfo o7AdapterInfo, boolean z);

  void onAdShown(IvyAdapterInfo o7AdapterInfo);

  void onAdShownFail(IvyAdType o7AdType);

  void softPause(IvyAdInfo o7AdInfo);

  void softResume(IvyAdInfo o7AdInfo);
}
