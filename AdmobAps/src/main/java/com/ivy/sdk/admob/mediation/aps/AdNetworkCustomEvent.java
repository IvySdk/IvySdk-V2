package com.ivy.sdk.admob.mediation.aps;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.VersionInfo;
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

import java.util.List;

public class AdNetworkCustomEvent extends Adapter {

    private CustomBannerLoader bannerLoader;
    private CustomInterstitialLoader interstitialLoader;

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
    public void initialize(@NonNull Context context, @NonNull InitializationCompleteCallback initializationCompleteCallback, @NonNull List<MediationConfiguration> list) {
        initializationCompleteCallback.onInitializationSucceeded();
    }

    @Override
    public void loadBannerAd(MediationBannerAdConfiguration adConfiguration, MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
        super.loadBannerAd(adConfiguration, callback);
        bannerLoader = new CustomBannerLoader(adConfiguration, callback);
        if (bannerLoader != null) {
            bannerLoader.loadAd();
        }
    }

    @Override
    public void loadInterstitialAd(MediationInterstitialAdConfiguration adConfiguration, MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        super.loadInterstitialAd(adConfiguration, callback);
        interstitialLoader = new CustomInterstitialLoader(adConfiguration, callback);
        if (interstitialLoader != null) {
            interstitialLoader.loadAd();
        }
    }

    @Override
    public void loadRewardedAd(MediationRewardedAdConfiguration adConfiguration, MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
        super.loadRewardedAd(adConfiguration, callback);
        callback.onFailure(new AdError(CustomAdEvent.ERROR_UNITY_ADS_NOT_SUPPORTED, "not support", CustomAdEvent.domain));
    }
}