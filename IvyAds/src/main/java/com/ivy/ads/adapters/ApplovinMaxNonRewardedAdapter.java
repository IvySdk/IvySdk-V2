package com.ivy.ads.adapters;

import android.app.Activity;
import android.content.Context;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxInterstitialAd;
//import com.applovin.mediation.openwrap.ALPubMaticOpenWrapConstants;
//import com.applovin.sdk.AppLovinSdk;
import com.ivy.base.ad.ivy.IvyAdType;
import com.ivy.base.util.Logger;

import org.json.JSONObject;

public class ApplovinMaxNonRewardedAdapter extends FullpageAdapter<FullpageAdapter.GridParams> implements MaxAdListener, MaxAdRevenueListener {
  private static final String TAG = "Adapter-Max-NonRewarded";

  private MaxInterstitialAd interstitialAd;
  private String mediation;
  public ApplovinMaxNonRewardedAdapter(Context context, String gridName, IvyAdType adType) {
    super(context, gridName, adType);
  }

  @Override
  public void fetch(Activity activity) {
    Logger.debug(TAG, "fetch()");
    if (!ApplovinManager.getInstance().isInitialized()) {
      Logger.error(TAG, "Applovin SDK initialize failed");
      super.onAdLoadFailed("not_init");
      return;
    }
    try {
      String placementId = segmentAdUnit == null ? getPlacementId() : segmentAdUnit.getAdUnit();
      if (placementId == null){
        placementId = "";
      }
      interstitialAd = new MaxInterstitialAd(placementId, activity);
//      interstitialAd.setLocalExtraParameter(ALPubMaticOpenWrapConstants.NETWORK_TIMEOUT_KEY, 5);
      interstitialAd.setListener(this);
      interstitialAd.setRevenueListener(this);
      interstitialAd.loadAd();
    } catch(Throwable t) {
      t.printStackTrace();
    }
  }

  @Override
  public void show(Activity activity) {
    Logger.debug(TAG, "ApplovinNoReward show");
    if (activity != null && this.interstitialAd != null && this.interstitialAd.isReady()) {
      interstitialAd.showAd();
    }
  }

  @Override
  public String getPlacementId() {
    return ((GridParams) getGridParams()).zoneId;
  }

  protected GridParams newGridParams() {
    return new GridParams();
  }

  @Override
  public void onAdLoaded(MaxAd ad) {
    mediation = ad.getNetworkName();
    Logger.debug(TAG, "onAdLoaded");
    super.onAdLoadSuccess();
  }

  @Override
  public void onAdDisplayed(MaxAd ad) {
    Logger.debug(TAG, "onAdDisplayed");
    super.onAdShowSuccess();
  }

  @Override
  public void onAdHidden(MaxAd ad) {
    onAdClosed(false);
  }




  @Override
  public void onAdRevenuePaid(MaxAd ad) {
    try {
      if (ad == null) {
        return;
      }
      double revenue = ad.getRevenue(); // In USD
      if (revenue <= 0 || revenue >= 10) {
        return;
      }
      String networkName = ad.getNetworkName(); // Display name of the network that showed the ad (e.g. "AdColony")
      String adUnitId = ad.getAdUnitId(); // The MAX Ad Unit ID
      MaxAdFormat adFormat = ad.getFormat(); // The ad format of the ad (e.g. BANNER, MREC, INTERSTITIAL, REWARDED)
      String placement = ad.getPlacement(); // The placement this ad's postbacks are tied to
      String networkPlacement = ad.getNetworkPlacement(); // The placement ID from the network that showed the ad
      super.onGmsPaidEvent(networkName, "interstitial", placement, adUnitId, "USD", 0, (long) (revenue * 1000000.0f));
    } catch(Throwable t) {
      t.printStackTrace();
    }
  }

  @Override
  public void onAdClicked(MaxAd ad) {
    Logger.debug(TAG, "onAdClicked");
    super.onAdClicked();
  }

  @Override
  public void onAdLoadFailed(String adUnitId, MaxError error) {
    Logger.warning(TAG, "onAdLoadFailed");
    super.onAdLoadFailed(error != null ? error.getMessage() : "empty_code");
  }

  @Override
  public void onAdDisplayFailed(MaxAd ad, MaxError error) {
    Logger.warning(TAG, "onAdDisplayFailed");
    super.onAdShowFail();
  }

  @Override
  public String getMediation() {
    if (mediation != null) return mediation;
    return "applovin";
  }

  public static class GridParams extends com.ivy.ads.adapters.BaseAdapter.GridParams {
    public String zoneId = "";

    public GridParams fromJSON(JSONObject jsonObject) {
      this.zoneId = jsonObject.optString("placement");
      return this;
    }

    protected String getParams() {
      return "placement=" + (this.zoneId != null ? ", zoneId=" + this.zoneId : "");
    }
  }
}
