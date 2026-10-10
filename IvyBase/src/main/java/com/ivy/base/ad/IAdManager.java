package com.ivy.base.ad;

import android.app.Activity;

import com.ivy.base.ab.IAbManager;
import com.ivy.base.track.ITrack;

import java.util.Map;

public interface IAdManager {
    void setup(Activity context, ITrack tracker, IAbManager abManager, IAdListener adListener, boolean debug);
    boolean isAvailable(AdType adType);
    default boolean isInterstitialAdAllowed() {
        return true;
    }
    void loadAd(AdType adType);
    void showAd(AdType adType, String tag, String placement, String clientInfo);
    void showBannerAd(int position, String tag, String placement, String clientInfo);
    void closeBannerAd(String placement);
    void onResume();
    void onPause();
    void onDestroy();
    float getPrice(AdType adType);

    void logEvent(String eventName, Map<String, Object> map);
}
