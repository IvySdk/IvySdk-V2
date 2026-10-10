package com.ivy.ads;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.ivy.base.ad.AdType;
import com.zeus.gmc.sdk.mobileads.columbus.ad.AdError;
import com.zeus.gmc.sdk.mobileads.columbus.ad.bannerad.AdSize;
import com.zeus.gmc.sdk.mobileads.columbus.ad.bannerad.BannerAdListener;
import com.zeus.gmc.sdk.mobileads.columbus.ad.bannerad.BannerAdView;

import org.json.JSONObject;

import java.util.Collections;

public class XiaomiBannerAd extends XiaomiBaseAd {
    private BannerAdView adView;
    private boolean loaded;
    private boolean shown;
    private int width = 320;
    private int height = 50;
    private boolean webViewAllowed = true;

    @Override
    public void init(AdType adType, JSONObject data, AdRevenueCallback revenueCallback,
                     Activity context) {
        super.init(adType, data, revenueCallback, context);
        width = data.optInt("width", 320);
        height = data.optInt("height", 50);
        webViewAllowed = data.optBoolean("webViewAllowed", true);
        createAdView();
    }

    private final BannerAdListener listener = new BannerAdListener() {
        @Override
        public void onAdLoaded(int width, int height) {
            isLoading = false;
            loaded = true;
            if (loadCallback != null) {
                loadCallback.onLoadSuccess(XiaomiBannerAd.this);
            }
        }

        @Override
        public void onAdError(AdError error) {
            isLoading = false;
            loaded = false;
            if (loadCallback != null) {
                loadCallback.onLoadFailed(XiaomiBannerAd.this,
                        error.getErrorCode(), error.getErrorMessage());
            }
        }

        @Override
        public void onAdClicked() {
            if (showCallback != null) {
                showCallback.onAdClicked(XiaomiBannerAd.this);
            }
        }

        @Override
        public void onLoggingImpression() {
        }

        @Override
        public void onAdClosed() {
            shown = false;
            loaded = false;
            if (showCallback != null) {
                showCallback.onAdClosed(XiaomiBannerAd.this, false);
            }
        }
    };

    private void createAdView() {
        adView = new BannerAdView(context);
        adView.setPlaceId(adUnit);
        adView.setAdSizeList(Collections.singletonList(new AdSize(width, height)));
        adView.setWebViewAllowed(webViewAllowed);
        adView.setAdEventListener(listener);
        adView.setVisibility(View.GONE);
    }

    @Override
    protected void loadAd() {
        if (adView == null) {
            createAdView();
        }
        loaded = false;
        shown = false;
        adView.loadAd();
    }

    @Override
    public boolean isReady() {
        return !isLoading && !shown && loaded && adView != null && adView.isAdLoaded();
    }

    @Override
    public void show(AdShowCallback callback) {
        if (!isReady()) {
            callback.onAdShowFailed(this, -1, "Ad not ready");
            return;
        }
        showCallback = callback;
        shown = true;
        adView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void close() {
        shown = false;
        if (adView != null) {
            adView.setVisibility(View.GONE);
        }
    }

    @Override
    public View getView() {
        return adView;
    }

    @Override
    public void destroy() {
        if (adView != null) {
            if (adView.getParent() instanceof ViewGroup) {
                ((ViewGroup) adView.getParent()).removeView(adView);
            }
            adView.destroy();
            adView = null;
        }
        loaded = false;
        shown = false;
        isLoading = false;
        loadCallback = null;
        showCallback = null;
        revenueCallback = null;
    }
}
