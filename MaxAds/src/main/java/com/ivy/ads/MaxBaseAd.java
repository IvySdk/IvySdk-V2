package com.ivy.ads;

import android.app.Activity;
import android.util.Log;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxError;
import com.ivy.base.ad.AdType;

import org.json.JSONArray;
import org.json.JSONObject;

public abstract class MaxBaseAd extends BaseAd implements MaxAdListener, MaxAdRevenueListener {
    // segment operation
    // refreshInterval
    protected int refreshInterval;

    @Override
    public void init(AdType adType, JSONObject data, AdRevenueCallback revenueCallback, Activity context) {
        super.init(adType, data, revenueCallback, context);
        MaxInit.selectiveAdUnitIds.add(adUnit);
        refreshInterval = data.optInt("refreshInterval", 0);
        applySegments(data);
        onInited();
    }

    private void applySegments(JSONObject data){
        if (data.has("segment")){
            JSONArray segs = data.optJSONArray("segment");
            if (segs != null){
                for(int i=0; i<segs.length(); ++i){
                    JSONObject o = segs.optJSONObject(i);
                    String type = o.optString("type", "");
                    if (type.equals("install")){
                        new MaxInstallSegment(o).apply(this);
                    } else if (type.equals("ecpm")){
                        new MaxAvgEcpmSegment(o).apply(this);
                    }
                }
            } else {
                String s = data.optString("segment");
                segments = s;
                MaxSegmentManager.addSegment(s);
            }
        }
    }

    protected abstract void onInited();

    private void printWatfallName(MaxAd maxAd) {
        if (maxAd == null) {
            return;
        }
        String name = maxAd.getWaterfall().getName();
        String adUnitId = maxAd.getAdUnitId();
        Log.e("hoho", "waterfall of " + adType + " " + adUnitId + " is " + name);
    }

    @Override
    protected boolean isNofillError(int errorCode, String errorMsg) {
        return errorCode == 204 || errorMsg.startsWith("Max return");
    }

    //---------------------- MaxAdListener 接口实现 ----------------------

    @Override
    public void onAdLoaded(MaxAd ad) {
        isLoading = false;
        price = ad.getRevenue();
        networkName = ad.getNetworkName();

        if (loadCallback != null) {
            loadCallback.onLoadSuccess(this);
        }

        printWatfallName(ad);
    }

    @Override
    public void onAdLoadFailed(String adUnitId, MaxError error) {
        isLoading = false;
        price = 0.0;

        if (loadCallback != null) {
            loadCallback.onLoadFailed(this, error.getCode(), error.getMessage());
        }
    }

    @Override
    public void onAdDisplayed(MaxAd ad) {
        if (showCallback != null) {
            showCallback.onAdDisplayed(this);
        }
    }

    @Override
    public void onAdClicked(MaxAd ad) {
        if (showCallback != null) {
            showCallback.onAdClicked(this);
        }
    }

    @Override
    public void onAdHidden(MaxAd ad) {
        isShowing = false;
        // 插屏广告没有奖励，所以rewarded参数始终为false
        if (showCallback != null) {
            showCallback.onAdClosed(this, false);
        }
    }

    @Override
    public void onAdDisplayFailed(MaxAd ad, MaxError error) {
        isShowing = false;
        if (showCallback != null) {
            showCallback.onAdShowFailed(this, error.getCode(), error.getMessage());
        }
    }

    //---------------------- MaxAdRevenueListener 接口实现 ----------------------

    @Override
    public void onAdRevenuePaid(MaxAd ad) {
        price = ad.getRevenue();
        networkName = ad.getNetworkName();
        // 触发收益回调
        if (revenueCallback != null) {
            revenueCallback.onAdRevenuePaid(this);
        }
    }
}
