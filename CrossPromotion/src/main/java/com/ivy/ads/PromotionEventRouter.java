package com.ivy.ads;

import com.ivy.crosspromotion.CrossPromotionListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class PromotionEventRouter implements CrossPromotionListener {
    static final PromotionEventRouter INSTANCE = new PromotionEventRouter();

    private final Map<String, PromotionBaseAd> ads = new ConcurrentHashMap<>();

    private PromotionEventRouter() {
    }

    void register(String format, PromotionBaseAd ad) {
        if (format != null && !format.isEmpty() && ad != null) {
            ads.put(format, ad);
        }
    }



    @Override
    public void onImpression(String format, String adId) {
        PromotionBaseAd ad = ads.get(format);
        if (ad != null) {
            ad.handleImpression();
        }
    }

    @Override
    public void onClick(String format, String adId) {
        PromotionBaseAd ad = ads.get(format);
        if (ad != null) {
            ad.handleClick();
        }
    }

    @Override
    public void onReward(String format, String adId) {
        PromotionBaseAd ad = ads.get(format);
        if (ad != null) {
            ad.handleReward();
        }
    }

    @Override
    public void onClosed(String format, String adId) {
        PromotionBaseAd ad = ads.get(format);
        if (ad != null) {
            ad.handleClosed();
        }
    }
}
