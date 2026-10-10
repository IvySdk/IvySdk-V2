package com.ivy.ads;

import com.ivy.crosspromotion.CrossPromotion;
import com.ivy.crosspromotion.CrossPromotionConfig;

public final class PromotionInterstitialAd extends PromotionBaseAd {
    @Override
    protected CrossPromotionConfig.Format promotionFormat() {
        return CrossPromotionConfig.Format.INTERSTITIAL;
    }

    @Override
    public boolean isReady() {
        return !isShowing && CrossPromotion.getInstance().isReady(promotionFormat(), context);
    }

    @Override
    public void show(AdShowCallback callback) {
        if (!isReady()) {
            showNotReady(callback);
            return;
        }
        beginShow(callback);
        CrossPromotion.getInstance().showInterstitial(context);
    }
}
