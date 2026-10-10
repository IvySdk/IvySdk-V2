package com.ivy.ads.interfaces;

import com.ivy.base.ad.ivy.IvyAdInfo;

public interface IvySoftCallbacks {
  void softPause(IvyAdInfo adInfo);

  void softResume(IvyAdInfo adInfo);
}
