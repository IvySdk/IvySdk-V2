package com.ivy.ads;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;

/**
 * MAX激励视频广告
 */
public class MaxRewardedAd extends MaxBaseAd implements MaxRewardedAdListener{

    private com.applovin.mediation.ads.MaxRewardedAd rewardedAd;
    private boolean wasRewarded = false;

    @Override
    protected void onInited() {
        rewardedAd = com.applovin.mediation.ads.MaxRewardedAd.getInstance(adUnit, context);
        rewardedAd.setListener(this);
        rewardedAd.setRevenueListener(this);
        if (disableAutoRetry) {
            rewardedAd.setExtraParameter("disable_auto_retries", "true");
            rewardedAd.setExtraParameter("disable_precache", "true");
        }
    }

    
    /**
     * 设置广告收益回调
     * @param callback 收益回调
     */
    public void setAdRevenueCallback(AdRevenueCallback callback) {
        this.revenueCallback = callback;
    }

    @Override
    public void loadAd() {
        wasRewarded = false;
        rewardedAd.loadAd();
    }
    
    /**
     * 展示激励视频广告
     * @param callback 展示回调
     * @return 是否成功展示广告
     */
    public void show(AdShowCallback callback) {
        if (!isReady()) return;
        
        this.showCallback = callback;
        
        // 重置奖励状态
        wasRewarded = false;

        isShowing = true;
        rewardedAd.showAd(context);
    }
    
    /**
     * 检查广告是否准备好展示
     * @return 是否可以展示广告
     */
    public boolean isReady() {
        return !isShowing && (rewardedAd != null && rewardedAd.isReady());
    }

    /**
     * 销毁广告实例，释放资源
     */
    public void destroy() {
        if (rewardedAd != null) {
            rewardedAd.destroy();
        }
        isShowing = false;
        rewardedAd = null;
        loadCallback = null;
        showCallback = null;
        revenueCallback = null;
    }
    
    @Override
    public void onAdHidden(MaxAd ad) {
        isShowing = false;
        if (showCallback != null) {
            showCallback.onAdClosed(this, wasRewarded);
        }
    }

    @Override
    public void onUserRewarded(MaxAd ad, MaxReward reward) {
        wasRewarded = true;
    }
}