package com.ivy.sdk.admob.mediation.admanager;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;


public class AdNetworkCustomEvent extends Adapter {

    private CustomBannerLoader bannerLoader;
    private CustomInterstitialLoader interstitialLoader;
    private CustomRewardedLoader rewardedLoader;

    @Override
    public VersionInfo getSDKVersionInfo() {
        String versionString = new VersionInfo(1, 0, 0).toString();
        String[] splits = versionString.split("\\.");

        if (splits.length >= 3) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = Integer.parseInt(splits[2]);
            return new VersionInfo(major, minor, micro);
        }

        return new VersionInfo(1, 0, 0);
    }

    @Override
    public VersionInfo getVersionInfo() {
        String versionString = new VersionInfo(1, 0, 0).toString();
        String[] splits = versionString.split("\\.");

        if (splits.length >= 4) {
            int major = Integer.parseInt(splits[0]);
            int minor = Integer.parseInt(splits[1]);
            int micro = (Integer.parseInt(splits[2]) * 100) + Integer.parseInt(splits[3]);
            return new VersionInfo(major, minor, micro);
        }

        return new VersionInfo(1, 0, 0);
    }

    @Override
    public void initialize(Context context, InitializationCompleteCallback initializationCompleteCallback, List<MediationConfiguration> mediationConfigurations) {

        if (context == null) {
            initializationCompleteCallback.onInitializationFailed("Initialization Failed: Context is null.");
            return;
        }

        MobileAds.initialize(context, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                initializationCompleteCallback.onInitializationSucceeded();
            }
        });
    }

    @Override
    public void loadBannerAd(MediationBannerAdConfiguration p0, MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
        super.loadBannerAd(p0, callback);
        bannerLoader = new CustomBannerLoader(p0, callback);
        if (bannerLoader != null) {
            bannerLoader.loadAd();
        }
    }

    @Override
    public void loadInterstitialAd(@NonNull MediationInterstitialAdConfiguration mediationInterstitialAdConfiguration, @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        super.loadInterstitialAd(mediationInterstitialAdConfiguration, callback);
        interstitialLoader = new CustomInterstitialLoader(mediationInterstitialAdConfiguration, callback);
        if (interstitialLoader != null) {
            interstitialLoader.loadAd();
        }
    }

    @Override
    public void loadRewardedAd(@NonNull MediationRewardedAdConfiguration mediationRewardedAdConfiguration, @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
        super.loadRewardedAd(mediationRewardedAdConfiguration, callback);
        rewardedLoader = new CustomRewardedLoader(mediationRewardedAdConfiguration, callback);
        if (rewardedLoader != null) {
            rewardedLoader.loadAd();
        }
    }


}