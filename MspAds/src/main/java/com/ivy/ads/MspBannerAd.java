package com.ivy.ads;

import android.view.View;

import androidx.annotation.NonNull;

import com.particles.msp.api.AdFormat;
import com.particles.msp.api.AdListener;
import com.particles.msp.api.AdLoader;
import com.particles.msp.api.AdRequest;
import com.particles.msp.api.AdSize;
import com.particles.msp.api.BannerAdView;
import com.particles.msp.api.MSPAd;

import java.util.HashMap;
import java.util.Map;

/**
 * MSP横幅广告
 */
public class MspBannerAd extends BaseAd {
    private boolean isLoaded = false;
    private BannerAdView bannerAdView = null;

    /**
     * 检查广告是否准备好展示
     * @return 是否可以展示广告
     */
    public boolean isReady() {
        return isLoaded;
    }

    private HashMap<String, Object> getTestParams() {
        HashMap<String, Object> testParams = new HashMap<>();
        testParams.put("test_ad", true);
        testParams.put("ad_network", "Prebid");
        return testParams;
    }

    public void loadAd() {
        AdRequest request = new AdRequest.Builder(AdFormat.BANNER)
                .setContext(context)
                .setAdSize(new AdSize(320, 50, false, false))
                .setAdaptiveBannerSize(new AdSize(384, 0, false, true))
                .setPlacement(adUnit)
                .setTestParams(getTestParams())
                .build();

        new AdLoader().loadAd(adUnit, new AdListener() {
            @Override
            public void onAdLoaded(@NonNull String s, @NonNull Map<String, ?> map) {
                onAdLoaded(s);
            }

            @Override
            public void onError(@NonNull String s, Map<String, ?> map) {
                onError(s);
            }

            @Override
            public void onError(@NonNull String s) {
                isLoading = false;
                isLoaded = false;
                loadCallback.onLoadFailed(MspBannerAd.this, 0, s);
            }

            @Override
            public void onAdLoaded(@NonNull String s) {
                isLoading = false;

                try {
                    bannerAdView = (BannerAdView) new AdLoader().getAd(adUnit);
                    price = (Double) bannerAdView.getAdInfo().get("price");
                    networkName = (String) bannerAdView.getAdInfo().get("networkName");
                    isLoaded = true;
                    loadCallback.onLoadSuccess(MspBannerAd.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAdClicked(@NonNull MSPAd mspAd) {
                showCallback.onAdClicked(MspBannerAd.this);
            }

            @Override
            public void onAdImpression(@NonNull MSPAd mspAd) {
                if (mspAd == bannerAdView) {
                    if (revenueCallback != null) {
                        revenueCallback.onAdRevenuePaid(MspBannerAd.this);
                    }
                    isLoaded = false;
                    showCallback.onAdDisplayed(MspBannerAd.this);
                }
            }

            @Override
            public void onAdDismissed(@NonNull MSPAd mspAd) {

            }
        }, request);
    }

    @Override
    public View getView() {
        return (View)bannerAdView.getAdView();
    }

    @Override
    public void show(AdShowCallback callback) {
        showCallback = callback;
        View adView = (View) bannerAdView.getAdView();
        adView.setVisibility(View.VISIBLE);
    }

    public void close(){
        if (bannerAdView != null){
            View adView = (View) bannerAdView.getAdView();
            adView.setVisibility(View.GONE);
        }
    }

    @Override
    protected boolean isNofillError(int errorCode, String errorMsg) {
        return errorMsg != null && errorMsg.startsWith("No winning");
    }

    /**
     * 销毁广告实例，释放资源
     */
    public void destroy() {
        if (bannerAdView != null){
            bannerAdView.destroy();
        }
        revenueCallback = null;
        isLoading = false;
        isLoaded = false;
    }
}
