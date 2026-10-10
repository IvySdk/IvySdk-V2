package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.ivy.ads.utils.Stopwatch;
import com.ivy.base.ad.ivy.IvyAdType;

public abstract class BannerAdapter<T extends BannerAdapter.GridParams> extends BaseAdapter<T> {
  public static int SMART_BANNER_WIDTH = -1;
  public static int MATCH_PARENT_WIDTH = -2;

  public static int STANDARD_BANNER_HEIGHT = 50;

  private boolean isSmartBannerEnabled = true;
  private Stopwatch mStopwatch = new Stopwatch(getClass().getSimpleName());

  protected ViewGroup mBannerContainer;

  public BannerAdapter(Context context, String gridName, IvyAdType adType) {
    super(context, gridName, adType);
    this.isSmartBannerEnabled = false; //context.getResources().getBoolean(R.bool.isSmartBannerEnabled);
  }

  public abstract View getView();

  public void render(ViewGroup bannerContainer) {

  }

  public void show(Activity activity, AdOpenCloseCallback adOpenCloseCallback) {
    super.show(activity, adOpenCloseCallback);
    this.mStopwatch.resume();
  }

  public void setBannerContainer(ViewGroup container) {
    this.mBannerContainer = container;
  }

  @Override
  public void show() {
    super.show();
  }

  public void hide() {
    super.hide();
    this.mStopwatch.pause();
  }

  public void resetOperationCount() {
    super.resetOperationCount();
    this.mStopwatch.clear();
  }

  public long getShownTime() {
    return this.mStopwatch.getElapsedTimeMillis();
  }

  public int getWidth() {
    return -1;
  }

  public int getHeight() {
    return STANDARD_BANNER_HEIGHT;
  }

  public boolean isTablet() {
    return false;
  }

  public void show(Activity activity) {
  }

  public boolean isSmartBannerEnabled() {
    return this.isSmartBannerEnabled;
  }
}
