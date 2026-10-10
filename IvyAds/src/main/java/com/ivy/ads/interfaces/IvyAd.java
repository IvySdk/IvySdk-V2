package com.ivy.ads.interfaces;

import android.app.Activity;

import com.ivy.base.ad.ivy.IvyAdCallbacks;

public interface IvyAd {
  void onPause(Activity activity);

  void onResume(Activity activity);

  void setCallback(IvyAdCallbacks o7AdCallbacks);

}
