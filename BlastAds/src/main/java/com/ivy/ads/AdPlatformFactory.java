package com.ivy.ads;

import com.ivy.base.ad.AdType;

/** Platform SDK classes are optional dependencies, so creation remains reflective. */
final class AdPlatformFactory {
    private AdPlatformFactory() { }

    /**
     * 根据平台名称获取对应的初始化类名
     *
     * @param platform 平台名称
     * @return 初始化类名
     */
    static IPlatformInit createInitializer(String platform) {
        try {
            switch (platform) {
                case "max":
                    return (IPlatformInit) Class.forName("com.ivy.ads.MaxInit").newInstance();
                case "admob":
                    return (IPlatformInit) Class.forName("com.ivy.ads.AdmobInit").newInstance();
                case "msp":
                    return (IPlatformInit) Class.forName("com.ivy.ads.MspInit").newInstance();
                case "aps":
                    return (IPlatformInit) Class.forName("com.ivy.ads.ApsInit").newInstance();
                case "yandex":
                    return (IPlatformInit) Class.forName("com.ivy.ads.YandexInit").newInstance();
                case "cloudx":
                    return (IPlatformInit) Class.forName("com.ivy.ads.CloudXInit").newInstance();
                case "pangle":
                    return (IPlatformInit) Class.forName("com.ivy.ads.PangleInit").newInstance();
                case "xiaomi":
                    return (IPlatformInit) Class.forName("com.ivy.ads.XiaomiInit").newInstance();
                case "facebook":
                    return (IPlatformInit) Class.forName("com.ivy.ads.FacebookInit").newInstance();
                case "facebook_bk3":
                    return (IPlatformInit) Class.forName("com.ivy.ads.FacebookBK3Init").newInstance();
                case "promotion":
                    return (IPlatformInit) Class.forName("com.ivy.ads.PromotionInit").newInstance();
                default:
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据platform获取对应的类名
     * 可以根据需要扩展支持更多平台
     */
    static BaseAd createAd(String platform, AdType adType) throws Exception {
        switch (platform) {
            case "max":
                switch (adType) {
                    case INTERSTITIAL:
                        return (BaseAd) Class.forName("com.ivy.ads.MaxInterstitialAd").newInstance();
                    case REWARDED:
                        return (BaseAd) Class.forName("com.ivy.ads.MaxRewardedAd").newInstance();
                    default:
                        return (BaseAd) Class.forName("com.ivy.ads.MaxBannerAd").newInstance();
                }
            case "admob":
                switch (adType) {
                    case INTERSTITIAL:
                        return (BaseAd) Class.forName("com.ivy.ads.AdmobInterstitialAd").newInstance();
                    case REWARDED:
                        return (BaseAd) Class.forName("com.ivy.ads.AdmobRewardedAd").newInstance();
                    default:
                        return (BaseAd) Class.forName("com.ivy.ads.AdmobBannerAd").newInstance();
                }
            case "msp":
                switch (adType) {
                    case INTERSTITIAL:
                        return (BaseAd) Class.forName("com.ivy.ads.MspInterstitialAd").newInstance();
                    case REWARDED:
                        return (BaseAd) Class.forName("com.ivy.ads.MspRewardedAd").newInstance();
                    default:
                        return (BaseAd) Class.forName("com.ivy.ads.MspBannerAd").newInstance();
                }
            case "yandex":
                switch (adType) {
                    case INTERSTITIAL:
                        return (BaseAd) Class.forName("com.ivy.ads.YandexInterstitialAd").newInstance();
                    case REWARDED:
                        return (BaseAd) Class.forName("com.ivy.ads.YandexRewardedAd").newInstance();
                    default:
                        return (BaseAd) Class.forName("com.ivy.ads.YandexBannerAd").newInstance();
                }
            case "cloudx":
                switch (adType) {
                    case INTERSTITIAL:
                        return (BaseAd) Class.forName("com.ivy.ads.CloudXInterstitialAd").newInstance();
                    case REWARDED:
                        return (BaseAd) Class.forName("com.ivy.ads.CloudXRewardedAd").newInstance();
                    default:
                        return (BaseAd) Class.forName("com.ivy.ads.CloudXBannerAd").newInstance();
                }
            case "xiaomi":
                switch (adType) {
                    case INTERSTITIAL:
                        return (BaseAd) Class.forName("com.ivy.ads.XiaomiInterstitialAd").newInstance();
                    case REWARDED:
                        return (BaseAd) Class.forName("com.ivy.ads.XiaomiRewardedAd").newInstance();
                    default:
                        return (BaseAd) Class.forName("com.ivy.ads.XiaomiBannerAd").newInstance();
                }
            case "pangle":
                switch (adType) {
                    case INTERSTITIAL:
                        return (BaseAd) Class.forName("com.ivy.ads.PangleInterstitialAd").newInstance();
                    case REWARDED:
                        return (BaseAd) Class.forName("com.ivy.ads.PangleRewardedAd").newInstance();
                }
            case "facebook":
                switch (adType) {
                    case INTERSTITIAL:
                        return (BaseAd) Class.forName("com.ivy.ads.FacebookInterstitialAd").newInstance();
                }
            case "facebook_bk3":
                switch (adType) {
                    case INTERSTITIAL:
                        return (BaseAd) Class.forName("com.ivy.ads.FacebookBK3InterstitialAd").newInstance();
                    case REWARDED:
                        return (BaseAd) Class.forName("com.ivy.ads.FacebookBK3RewardedAd").newInstance();
                }
            case "promotion":
                switch (adType) {
                    case INTERSTITIAL:
                        return (BaseAd) Class.forName("com.ivy.ads.PromotionInterstitialAd").newInstance();
                    case REWARDED:
                        return (BaseAd) Class.forName("com.ivy.ads.PromotionRewardedAd").newInstance();
                    default:
                        return (BaseAd) Class.forName("com.ivy.ads.PromotionBannerAd").newInstance();
                }
            default:
                return null;
        }
    }
}
