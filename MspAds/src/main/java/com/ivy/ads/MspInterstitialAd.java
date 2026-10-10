package com.ivy.ads;

import android.util.Log;

import androidx.annotation.NonNull;

import com.particles.msp.api.AdFormat;
import com.particles.msp.api.AdListener;
import com.particles.msp.api.AdLoader;
import com.particles.msp.api.AdRequest;
import com.particles.msp.api.InterstitialAd;
import com.particles.msp.api.MSPAd;
import com.particles.msp.api.MSPConstants;
import com.particles.prebidadapter.MSP;

import java.util.HashMap;
import java.util.Map;

public class MspInterstitialAd extends BaseAd {
    private InterstitialAd interstitialAd = null;
    private String requestId;

    public boolean isReady(){
        return !isShowing && interstitialAd != null;
    }

    private HashMap<String, Object> getTestParams() {
        HashMap<String, Object> testParams = new HashMap<>();
        testParams.put("test_ad", true);
        //testParams["ad_network"] = "msp_google"
        //testParams["ad_network"] = "msp_fb"
        testParams.put("ad_network", "msp_nova");
        testParams.put("is_vertical", true);
        testParams.put("creative_type", "video");
        return testParams;
    }

    public void loadAd(){
        AdRequest request = new AdRequest.Builder(AdFormat.INTERSTITIAL)
                .setContext(context)
                .setPlacement(adUnit)
//                .setTestParams(getTestParams())
                .build();
        new AdLoader().loadAd(adUnit, new AdListener() {
            @Override
            public void onAdLoaded(@NonNull String s, @NonNull Map<String, ?> map) {
                onAdLoaded(s);
                Object rid = map.get(MSPConstants.LOAD_INFO_KEY_REQUEST_ID);
                if (rid == null){
                    requestId = "";
                } else {
                    requestId = rid.toString();
                }
            }

            @Override
            public void onError(@NonNull String s, Map<String, ?> map) {
                onError(s);
                interstitialAd = null;
                Object rid = map.get(MSPConstants.LOAD_INFO_KEY_REQUEST_ID);
                if (rid == null){
                    requestId = "";
                } else {
                    requestId = rid.toString();
                }
//                onLoss("max", 1);
            }

            @Override
            public void onError(@NonNull String s) {
                if (isShowing){
                    interstitialAd = null;
                    showCallback.onAdShowFailed(MspInterstitialAd.this, 0, s);
                    isShowing = false;
                } else {
                    isLoading = false;
                    loadCallback.onLoadFailed(MspInterstitialAd.this, 0, s);
                }
            }

            @Override
            public void onAdLoaded(@NonNull String s) {
                isLoading = false;

                try {
                    interstitialAd = (InterstitialAd) new AdLoader().getAd(adUnit);
                    Map<String, Object> adInfo = interstitialAd.getAdInfo();
                    price = (Double) adInfo.get("price") / 1000.0;
                    networkName = (String) adInfo.get("networkName");
                    loadCallback.onLoadSuccess(MspInterstitialAd.this);
                } catch (Exception e) {
                    e.printStackTrace();
                    loadCallback.onLoadFailed(MspInterstitialAd.this, 0, e.getMessage());
                }
            }

            @Override
            public void onAdClicked(@NonNull MSPAd mspAd) {
                if (mspAd == interstitialAd){
                    showCallback.onAdClicked(MspInterstitialAd.this);
                }
            }

            @Override
            public void onAdImpression(@NonNull MSPAd mspAd) {
                if (mspAd == interstitialAd){
                    showCallback.onAdDisplayed(MspInterstitialAd.this);
                    revenueCallback.onAdRevenuePaid(MspInterstitialAd.this);
                }
            }

            @Override
            public void onAdDismissed(@NonNull MSPAd mspAd) {
                if (mspAd == interstitialAd){
                    interstitialAd = null;
                    showCallback.onAdClosed(MspInterstitialAd.this, false);
                    isShowing = false;
                }
            }
        }, request);
    }

    @Override
    protected boolean isNofillError(int errorCode, String errorMsg) {
        return errorMsg != null && errorMsg.startsWith("No winning");
    }

    @Override
    public void onLoss(String bidder, float price) {
        Log.e("hoho", "msp loss vs " + bidder + " price " + price);
        MSP.INSTANCE.notifyLoss(bidder, price * 1000, requestId, interstitialAd);
    }

    public void show(AdShowCallback callback){
        if (!isReady()) return;

        this.showCallback = callback;

        isShowing = true;

        interstitialAd.show(context);
    }

    public void destroy() {
        if (interstitialAd != null) {
            interstitialAd.destroy();
        }
        isShowing = false;
        interstitialAd = null;
        showCallback = null;
        revenueCallback = null;
    }
}
