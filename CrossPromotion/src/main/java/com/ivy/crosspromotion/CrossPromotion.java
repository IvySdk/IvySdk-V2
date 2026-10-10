package com.ivy.crosspromotion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.appsflyer.share.CrossPromotionHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Selects one ready promoted app at show time. The public model intentionally contains only ads;
 * the requested format determines whether banner, image, or video is used.
 */
public final class CrossPromotion {
    private static final String TAG = "CrossPromotion";
    private static final CrossPromotion INSTANCE = new CrossPromotion();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SmoothWeightedSelector weightedSelector = new SmoothWeightedSelector();
    private final InterstitialCreativeSelector interstitialCreativeSelector =
            new InterstitialCreativeSelector();

    private Context applicationContext;
    private CrossPromotionConfig config;
    private CrossPromotionListener listener = new CrossPromotionListener() {
    };
    private CreativeCache creativeCache;
    private RemoteConfigLoader remoteConfigLoader;
    private long configRequestGeneration;

    private CrossPromotion() {
    }

    public static CrossPromotion getInstance() {
        return INSTANCE;
    }

    public void initialize(
            Context context,
            CrossPromotionConfig config,
            CrossPromotionListener listener
    ) {
        initialize(context, config, "", listener);
    }

    /**
     * Starts immediately with a last-known-good or fallback config, then checks configUrl once in
     * the background. A remote config is applied only after it has been completely validated.
     */
    public synchronized void initialize(
            Context context,
            CrossPromotionConfig fallbackConfig,
            String configUrl,
            CrossPromotionListener listener
    ) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        CrossPromotionConfig initialFallback = fallbackConfig == null
                ? CrossPromotionConfig.empty()
                : fallbackConfig;
        applicationContext = context.getApplicationContext();
        this.listener = listener == null ? new CrossPromotionListener() {
        } : listener;
        if (remoteConfigLoader != null) {
            remoteConfigLoader.shutdown();
            remoteConfigLoader = null;
        }
        long requestGeneration = ++configRequestGeneration;

        String normalizedUrl = configUrl == null ? "" : configUrl.trim();
        Log.d(TAG, "initialize: fallbackVersion=" + initialFallback.getVersion()
                + ", fallbackAds=" + initialFallback.getAds().size()
                + ", configUrl=" + normalizedUrl);
        if (normalizedUrl.isEmpty()) {
            applyConfig(initialFallback);
            return;
        }

        RemoteConfigLoader loader = new RemoteConfigLoader(
                applicationContext,
                normalizedUrl,
                initialFallback,
                new RemoteConfigLoader.Callback() {
                    @Override
                    public void onSuccess(
                            CrossPromotionConfig remoteConfig,
                            boolean notModified
                    ) {
                        boolean updated = false;
                        synchronized (CrossPromotion.this) {
                            if (requestGeneration != configRequestGeneration) {
                                return;
                            }
                            if (!notModified && isDifferentVersion(remoteConfig)) {
                                applyConfig(remoteConfig);
                                updated = true;
                            }
                        }
                        boolean finalUpdated = updated;
                        mainHandler.post(() -> CrossPromotion.this.listener.onConfigChecked(
                                remoteConfig.getVersion(), finalUpdated));
                    }

                    @Override
                    public void onError(Exception error) {
                        synchronized (CrossPromotion.this) {
                            if (requestGeneration != configRequestGeneration) {
                                return;
                            }
                        }
                        String reason = error.getMessage() == null
                                ? error.getClass().getSimpleName()
                                : error.getMessage();
                        mainHandler.post(() -> CrossPromotion.this.listener
                                .onConfigCheckFailed(reason));
                    }
                });
        remoteConfigLoader = loader;
        CrossPromotionConfig savedConfig = loader.loadLastGood();
        CrossPromotionConfig activeConfig = savedConfig == null ? initialFallback : savedConfig;
        applyConfig(activeConfig);
        loader.setCurrentConfig(activeConfig);
        loader.refresh();
    }

    /** Rechecks the configured server without replacing the current config first. */
    public synchronized void refreshConfig() {
        requireInitialized();
        if (remoteConfigLoader == null) {
            mainHandler.post(() -> listener.onConfigCheckFailed(
                    "No remote config URL configured"));
            return;
        }
        remoteConfigLoader.refresh();
    }

    private void applyConfig(CrossPromotionConfig newConfig) {
        Log.d(TAG, "applyConfig: version=" + newConfig.getVersion()
                + ", ads=" + newConfig.getAds().size()
                + ", enabled=" + newConfig.isEnabled());
        if (creativeCache != null) {
            creativeCache.shutdown();
        }
        config = newConfig;
        weightedSelector.clear();
        interstitialCreativeSelector.clear();
        creativeCache = new CreativeCache(applicationContext);
        cacheResources();
    }

    private void cacheResources() {
        if (config == null || !config.isEnabled() || creativeCache == null) {
            return;
        }
        String version = config.getVersion();
        for (CrossPromotionConfig.Ad ad : config.getAds()) {
            if (!isEligible(ad)) {
                continue;
            }
            if (ad.getIcon() != null && !ad.getIcon().isEmpty()) {
                creativeCache.cache(ad.getIcon(), version, CreativeCache.Type.IMAGE);
            }
            if (ad.getBanner() != null && !ad.getBanner().isEmpty()) {
                creativeCache.cache(ad.getBanner(), version, CreativeCache.Type.IMAGE);
            }
            if (ad.getImage() != null && !ad.getImage().isEmpty()) {
                creativeCache.cache(ad.getImage(), version, CreativeCache.Type.IMAGE);
            }
            if (ad.getVideo() != null && !ad.getVideo().isEmpty()) {
                creativeCache.cache(ad.getVideo(), version, CreativeCache.Type.VIDEO);
            }
            if (ad.getImage2() != null && !ad.getImage2().isEmpty()) {
                creativeCache.cache(ad.getImage2(), version, CreativeCache.Type.IMAGE);
            }
            if (ad.getVideo2() != null && !ad.getVideo2().isEmpty()) {
                creativeCache.cache(ad.getVideo2(), version, CreativeCache.Type.VIDEO);
            }
        }
    }

    private boolean isDifferentVersion(CrossPromotionConfig remoteConfig) {
        if (config == null) {
            return true;
        }
        String currentVersion = config.getVersion();
        String remoteVersion = remoteConfig.getVersion();
        return remoteVersion.isEmpty() || !remoteVersion.equals(currentVersion);
    }

    public synchronized boolean isReady(CrossPromotionConfig.Format format) {
        return isReady(format, applicationContext);
    }

    public synchronized boolean isReady(
            CrossPromotionConfig.Format format,
            Context context
    ) {
        return !readySelections(format, isLandscape(context)).isEmpty();
    }

    public void showInterstitial(Activity activity) {
        showFullscreen(activity, CrossPromotionConfig.Format.INTERSTITIAL);
    }

    public void showRewarded(Activity activity) {
        showFullscreen(activity, CrossPromotionConfig.Format.REWARDED);
    }

    private void showFullscreen(Activity activity, CrossPromotionConfig.Format format) {
        requireInitialized();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        Selection selection = selectReady(format, isLandscape(activity));
        if (selection == null) {
            Log.w(TAG, "showFullscreen: Promotion is not ready for " + format.value());
            return;
        }

        Log.d(TAG, "showFullscreen: format=" + format.value()
                + ", adId=" + selection.ad.getId()
                + ", video=" + selection.video
                + ", file=" + selection.file.getAbsolutePath());
        File icon = readyFile(selection.ad.getIcon(), CreativeCache.Type.IMAGE);
        Intent intent = CrossPromoFullscreenActivity.createIntent(
                activity,
                format,
                selection.ad.getId(),
                selection.file.getAbsolutePath(),
                icon == null ? null : icon.getAbsolutePath(),
                selection.video);
        try {
            activity.startActivity(intent);
        } catch (RuntimeException error) {
            Log.e(TAG, "Failed to start fullscreen activity", error);
        }
    }

    public CrossPromoBannerView createBanner(Context context) {
        requireInitialized();
        Selection selection = selectReady(CrossPromotionConfig.Format.BANNER, false);
        if (selection == null) {
            Log.w(TAG, "createBanner: Promotion banner is not ready");
            return null;
        }
        Log.d(TAG, "createBanner: adId=" + selection.ad.getId());
        File icon = readyFile(selection.ad.getIcon(), CreativeCache.Type.IMAGE);
        if (icon == null) {
            return null;
        }
        CrossPromoBannerView view = new CrossPromoBannerView(context);
        view.bind(selection.ad, selection.file, icon);
        return view;
    }

    synchronized CrossPromotionConfig.Ad getAd(String id) {
        return config == null ? null : config.getAd(id);
    }

    void recordImpression(
            CrossPromotionConfig.Format format,
            CrossPromotionConfig.Ad ad,
            boolean video
    ) {
        if (ad == null) {
            return;
        }
        Log.d(TAG, "recordImpression: format=" + format.value()
                + ", adId=" + ad.getId()
                + ", targetPackage=" + ad.getTargetPackage());
        try {
            String sourcePackage = applicationContext != null ? applicationContext.getPackageName() : null;
            String campaign = (sourcePackage != null && !sourcePackage.isEmpty()) ? sourcePackage : "crosspromo";
            CrossPromotionHelper.logCrossPromoteImpression(
                    applicationContext,
                    ad.getTargetPackage(),
                    campaign,
                    attributionParameters(format, ad, video));
        } catch (RuntimeException ignored) {
            // Attribution must never break rendering.
        }
        mainHandler.post(() -> listener.onImpression(format.value(), ad.getId()));
    }

    void performClick(
            Context context,
            CrossPromotionConfig.Format format,
            CrossPromotionConfig.Ad ad,
            boolean video
    ) {
        if (ad == null) {
            return;
        }
        Log.d(TAG, "performClick: format=" + format.value()
                + ", adId=" + ad.getId()
                + ", targetPackage=" + ad.getTargetPackage());
        mainHandler.post(() -> listener.onClick(format.value(), ad.getId()));
        try {
            Map<String, String> parameters = attributionParameters(format, ad, video);
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + Uri.encode(ad.getTargetPackage())));
            if (marketIntent.resolveActivity(context.getPackageManager()) == null) {
                // AF opens its redirect asynchronously and handles launch failures internally.
                parameters.put("af_r", "https://play.google.com/store/apps/details?id="
                        + Uri.encode(ad.getTargetPackage()));
                Log.d(TAG, "No market handler; requesting AppsFlyer HTTPS redirect");
            }
            String sourcePackage = context.getPackageName();
            String campaign = (sourcePackage != null && !sourcePackage.isEmpty()) ? sourcePackage : "crosspromo";
            CrossPromotionHelper.logAndOpenStore(
                    context,
                    ad.getTargetPackage(),
                    campaign,
                    parameters);
        } catch (RuntimeException error) {
            Log.w(TAG, "AppsFlyer cross-promotion click failed; opening store directly", error);
            openStore(context, ad.getTargetPackage(), ad.getId(), format);
        }
    }

    void notifyReward(CrossPromotionConfig.Ad ad) {
        if (ad != null) {
            Log.d(TAG, "notifyReward: adId=" + ad.getId());
            mainHandler.post(() -> listener.onReward(
                    CrossPromotionConfig.Format.REWARDED.value(), ad.getId()));
        }
    }

    void notifyClosed(CrossPromotionConfig.Format format, String adId) {
        Log.d(TAG, "notifyClosed: format=" + format.value() + ", adId=" + adId);
        mainHandler.post(() -> listener.onClosed(format.value(), adId));
    }

    private Map<String, String> attributionParameters(
            CrossPromotionConfig.Format format,
            CrossPromotionConfig.Ad ad,
            boolean video
    ) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("af_ad", ad.getId());
        parameters.put("af_ad_type", adType(format));
        parameters.put("af_ad_format", video ? "video" : "image");
        String version = config == null ? "" : config.getVersion();
        if (version != null && !version.isEmpty()) {
            parameters.put("af_ad_version", version);
        }
        parameters.put("utm_source", "ivy");
        String sourcePackage = applicationContext != null ? applicationContext.getPackageName() : null;
        parameters.put("utm_campaign", (sourcePackage != null && !sourcePackage.isEmpty()) ? sourcePackage : "crosspromo");
        parameters.put("utm_medium", adType(format));
        parameters.put("utm_content", ad.getId());
        return parameters;
    }

    private String adType(CrossPromotionConfig.Format format) {
        if (format == CrossPromotionConfig.Format.BANNER) {
            return "banner";
        }
        if (format == CrossPromotionConfig.Format.REWARDED) {
            return "rewarded_video";
        }
        return "interstitial";
    }

    private synchronized Selection selectReady(
            CrossPromotionConfig.Format format,
            boolean landscape
    ) {
        List<Selection> candidates = readySelections(format, landscape);
        if (candidates.isEmpty()) {
            return null;
        }

        LinkedHashMap<String, Integer> weights = new LinkedHashMap<>();
        for (Selection candidate : candidates) {
            weights.put(candidate.ad.getId(), candidate.ad.getWeight());
        }
        String selectedId = weightedSelector.select(format, weights);
        for (Selection selection : candidates) {
            if (selection.ad.getId().equals(selectedId)) {
                return format == CrossPromotionConfig.Format.INTERSTITIAL
                        ? selectInterstitialCreative(selection.ad, landscape)
                        : selection;
            }
        }
        return null;
    }

    private Selection selectInterstitialCreative(
            CrossPromotionConfig.Ad ad,
            boolean landscape
    ) {
        File video = readyFile(ad.getVideo(landscape), CreativeCache.Type.VIDEO);
        File image = readyFile(ad.getImage(landscape), CreativeCache.Type.IMAGE);
        if (video == null && image == null) {
            return null;
        }
        boolean selectVideo = interstitialCreativeSelector.selectVideo(
                ad, landscape, video != null, image != null);
        return selectVideo
                ? new Selection(ad, video, true)
                : new Selection(ad, image, false);
    }

    private List<Selection> readySelections(
            CrossPromotionConfig.Format format,
            boolean landscape
    ) {
        List<Selection> candidates = new ArrayList<>();
        if (config == null || !config.isEnabled()) {
            return candidates;
        }
        for (CrossPromotionConfig.Ad ad : config.getAds()) {
            if (!isEligible(ad) || !ad.supports(format)) {
                continue;
            }
            Selection selection = readySelection(ad, format, landscape);
            if (selection != null) {
                candidates.add(selection);
            }
        }
        return candidates;
    }

    private Selection readySelection(
            CrossPromotionConfig.Ad ad,
            CrossPromotionConfig.Format format,
            boolean landscape
    ) {
        if (format == CrossPromotionConfig.Format.BANNER) {
            if (ad.getIcon() == null || ad.getIcon().isEmpty()
                    || readyFile(ad.getIcon(), CreativeCache.Type.IMAGE) == null) {
                return null;
            }
            File file = readyFile(ad.getBanner(), CreativeCache.Type.IMAGE);
            return file == null ? null : new Selection(ad, file, false);
        }
        String videoUrl = ad.getVideo(landscape);
        String imageUrl = ad.getImage(landscape);
        if (format == CrossPromotionConfig.Format.REWARDED) {
            if (videoUrl != null && !videoUrl.isEmpty()) {
                File file = readyFile(videoUrl, CreativeCache.Type.VIDEO);
                return file == null ? null : new Selection(ad, file, true);
            }
            File file = readyFile(imageUrl, CreativeCache.Type.IMAGE);
            return file == null ? null : new Selection(ad, file, false);
        }
        File video = readyFile(videoUrl, CreativeCache.Type.VIDEO);
        if (video != null) {
            return new Selection(ad, video, true);
        }
        File image = readyFile(imageUrl, CreativeCache.Type.IMAGE);
        return image == null ? null : new Selection(ad, image, false);
    }

    private boolean isLandscape(Context context) {
        return context != null
                && context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }

    private File readyFile(String url, CreativeCache.Type type) {
        if (url == null || url.isEmpty() || creativeCache == null) {
            return null;
        }
        String version = config == null ? "" : config.getVersion();
        return creativeCache.getCachedFile(url, version, type);
    }

    private boolean isEligible(CrossPromotionConfig.Ad ad) {
        return ad.isEnabled() && !isPackageInstalled(ad.getTargetPackage());
    }

    private boolean isPackageInstalled(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        PackageManager packageManager = applicationContext.getPackageManager();
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    void openStore(
            Context context,
            String packageName,
            String adId,
            CrossPromotionConfig.Format format
    ) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) {
            return;
        }
        String pkg = packageName.trim();
        String medium = adType(format);
        String sourcePackage = context.getPackageName();
        String campaign = (sourcePackage != null && !sourcePackage.isEmpty()) ? sourcePackage : "crosspromo";
        StringBuilder referrerBuilder = new StringBuilder();
        referrerBuilder.append("pid=af_cross_promotion")
                .append("&c=").append(campaign);
        if (sourcePackage != null && !sourcePackage.isEmpty()) {
            referrerBuilder.append("&af_siteid=").append(sourcePackage);
        }
        if (adId != null && !adId.isEmpty()) {
            referrerBuilder.append("&af_ad=").append(adId);
        }
        referrerBuilder.append("&af_ad_type=").append(medium);
        String version = config == null ? "" : config.getVersion();
        if (version != null && !version.isEmpty()) {
            referrerBuilder.append("&af_ad_version=").append(version);
        }
        referrerBuilder.append("&utm_source=ivy")
                .append("&utm_campaign=").append(campaign)
                .append("&utm_medium=").append(medium);
        if (adId != null && !adId.isEmpty()) {
            referrerBuilder.append("&utm_content=").append(adId);
        }
        String referrer = Uri.encode(referrerBuilder.toString());
        String marketUri = "market://details?id=" + Uri.encode(pkg) + "&referrer=" + referrer;
        String webUri = "https://play.google.com/store/apps/details?id=" + Uri.encode(pkg) + "&referrer=" + referrer;

        // 1. Try launching Google Play Store directly
        try {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(marketUri));
            marketIntent.setPackage("com.android.vending");
            if (!(context instanceof Activity)) {
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(marketIntent);
            Log.d(TAG, "Opened Google Play via market:// and com.android.vending: " + marketUri);
            return;
        } catch (Exception ignored) {
        }

        // 2. Try generic market:// URI
        try {
            Intent marketGenericIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(marketUri));
            if (!(context instanceof Activity)) {
                marketGenericIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(marketGenericIntent);
            Log.d(TAG, "Opened store via generic market:// URI: " + marketUri);
            return;
        } catch (Exception ignored) {
        }

        // 3. Fallback to HTTPS web browser URL
        try {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
            if (!(context instanceof Activity)) {
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(webIntent);
            Log.d(TAG, "Opened store via web browser URL: " + webUri);
        } catch (Exception error) {
            Log.e(TAG, "Failed to open store for " + pkg, error);
        }
    }

    private void requireInitialized() {
        if (applicationContext == null || config == null || creativeCache == null) {
            throw new IllegalStateException("Call CrossPromotion.initialize() first");
        }
    }

    private static final class Selection {
        private final CrossPromotionConfig.Ad ad;
        private final File file;
        private final boolean video;

        private Selection(CrossPromotionConfig.Ad ad, File file, boolean video) {
            this.ad = ad;
            this.file = file;
            this.video = video;
        }
    }
}
