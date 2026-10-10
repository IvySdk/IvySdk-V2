package com.ivy.crosspromotion;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public final class CrossPromoFullscreenActivity extends Activity {
    private static final String TAG = "CrossPromotion";
    private static final String EXTRA_FORMAT = "format";
    private static final String EXTRA_AD_ID = "ad_id";
    private static final String EXTRA_CREATIVE_PATH = "creative_path";
    private static final String EXTRA_ICON_PATH = "icon_path";
    private static final String EXTRA_VIDEO = "video";
    private static final int INTERSTITIAL_SKIP_SECONDS = 5;
    private static final int REWARDED_IMAGE_SECONDS = 5;
    private static final int DEFAULT_REWARD_SECONDS = 15;
    private static final long STORE_OPEN_TIMEOUT_MS = 10_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private CrossPromotionConfig.Format format;
    private String adId;
    private CrossPromotionConfig.Ad ad;
    private View mediaContainer;
    private VideoView videoView;
    private View bottomCard;
    private View playbackAdBadge;
    private View landingPage;
    private View storeLoading;
    private Context storeLaunchContext;
    private long storeLaunchDeadlineMs;
    private TextView cardTitleView;
    private TextView cardDescriptionView;
    private TextView cardCallToActionView;
    private TextView landscapeCallToActionView;
    private TextView closeView;
    private TextView countdownView;
    private ImageView soundToggleView;
    private MediaPlayer mediaPlayer;
    private boolean landingShown;
    private boolean isMuted;
    private boolean videoCreative;
    private boolean videoPrepared;
    private boolean videoCompleted;
    private boolean impressionSent;
    private boolean rewardSent;
    private boolean canClose;
    private int pausedPosition;
    private int videoDurationMs;
    private int creativeWidth;
    private int creativeHeight;
    private long timerStartTimeMs;
    private long totalDurationMs = INTERSTITIAL_SKIP_SECONDS * 1000L;
    private long pausedElapsedMs;

    private final Runnable storeOpenTimeout = () -> {
        if (storeLaunchContext != null) {
            Log.w(TAG, "Store opening timed out; falling back to direct store launch");
            Context context = storeLaunchContext;
            clearStoreOpening();
            if (ad != null) {
                CrossPromotion.getInstance().openStore(context, ad.getTargetPackage(), ad.getId(), format);
            }
            finish();
        }
    };

    private final Runnable countdownTicker = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed() || canClose) {
                return;
            }

            long elapsed = pausedElapsedMs
                    + (timerStartTimeMs > 0
                    ? SystemClock.elapsedRealtime() - timerStartTimeMs
                    : 0);
            long remainingMs = Math.max(0, totalDurationMs - elapsed);
            int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);

            if (remainingSeconds > 0 && !videoCompleted) {
                if (countdownView.getVisibility() != View.VISIBLE) {
                    countdownView.setVisibility(View.VISIBLE);
                }
                String newText = remainingSeconds + "s";
                if (!newText.contentEquals(countdownView.getText())) {
                    countdownView.setText(newText);
                }
                closeView.setVisibility(View.GONE);
                handler.removeCallbacks(this);
                handler.postDelayed(this, 250);
            } else {
                if (!videoCreative || format == CrossPromotionConfig.Format.REWARDED) {
                    showLandingPage();
                }
                allowClose();
                if (format == CrossPromotionConfig.Format.REWARDED
                        && !rewardSent
                        && ad != null) {
                    rewardSent = true;
                    CrossPromotion.getInstance().notifyReward(ad);
                }
            }
        }
    };

    static Intent createIntent(
            Context context,
            CrossPromotionConfig.Format format,
            String adId,
            String creativePath,
            String iconPath,
            boolean video
    ) {
        return new Intent(context, CrossPromoFullscreenActivity.class)
                .putExtra(EXTRA_FORMAT, format.value())
                .putExtra(EXTRA_AD_ID, adId)
                .putExtra(EXTRA_CREATIVE_PATH, creativePath)
                .putExtra(EXTRA_ICON_PATH, iconPath)
                .putExtra(EXTRA_VIDEO, video);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enterImmersiveMode();
        setContentView(R.layout.ivy_cross_promotion_interstitial);

        format = CrossPromotionConfig.Format.parse(getIntent().getStringExtra(EXTRA_FORMAT));
        adId = getIntent().getStringExtra(EXTRA_AD_ID);
        videoCreative = getIntent().getBooleanExtra(EXTRA_VIDEO, false);
        ad = CrossPromotion.getInstance().getAd(adId);
        if (ad == null || format == CrossPromotionConfig.Format.BANNER) {
            finish();
            return;
        }

        mediaContainer = findViewById(R.id.ivy_cross_promotion_media_container);
        ImageView imageView = findViewById(R.id.ivy_cross_promotion_creative);
        videoView = findViewById(R.id.ivy_cross_promotion_video);
        ProgressBar loading = findViewById(R.id.ivy_cross_promotion_video_loading);
        closeView = findViewById(R.id.ivy_cross_promotion_close);
        countdownView = findViewById(R.id.ivy_cross_promotion_countdown);
        soundToggleView = findViewById(R.id.ivy_cross_promotion_sound_toggle);
        ImageView appIconView = findViewById(R.id.ivy_cross_promotion_app_icon);
        ImageView landingIconView = findViewById(R.id.ivy_cross_promotion_landing_icon);
        bottomCard = findViewById(R.id.ivy_cross_promotion_bottom_card);
        playbackAdBadge = findViewById(R.id.ivy_cross_promotion_ad_badge);
        landingPage = findViewById(R.id.ivy_cross_promotion_landing_page);
        storeLoading = findViewById(R.id.ivy_cross_promotion_store_loading);

        String iconPath = getIntent().getStringExtra(EXTRA_ICON_PATH);
        setupAppIcon(appIconView, iconPath);
        setupAppIcon(landingIconView, iconPath);
        cardTitleView = findViewById(R.id.ivy_cross_promotion_card_title);
        cardDescriptionView = findViewById(R.id.ivy_cross_promotion_card_description);
        cardCallToActionView = findViewById(R.id.ivy_cross_promotion_card_cta);
        landscapeCallToActionView = findViewById(
                R.id.ivy_cross_promotion_landscape_cta);
        configurePlaybackCard();
        bindText(findViewById(R.id.ivy_cross_promotion_landing_title), ad.getTitle());
        bindText(findViewById(R.id.ivy_cross_promotion_landing_description),
                ad.getDescription());
        TextView landingCallToAction = findViewById(R.id.ivy_cross_promotion_landing_cta);
        bindText(landingCallToAction, ad.getCallToAction());

        View.OnClickListener clickListener = view -> openPromotedApp();
        mediaContainer.setOnClickListener(clickListener);
        imageView.setOnClickListener(clickListener);
        videoView.setOnClickListener(clickListener);
        bottomCard.setOnClickListener(clickListener);
        cardCallToActionView.setOnClickListener(clickListener);
        landscapeCallToActionView.setOnClickListener(clickListener);
        landingPage.setOnClickListener(clickListener);
        landingCallToAction.setOnClickListener(clickListener);
        closeView.setOnClickListener(view -> {
            if (!canClose) {
                return;
            }
            if (landingShown) {
                finish();
            } else {
                showLandingPage();
            }
        });
        soundToggleView.setOnClickListener(view -> toggleSound());

        String creativePath = getIntent().getStringExtra(EXTRA_CREATIVE_PATH);
        if (videoCreative) {
            setupVideo(creativePath, imageView, loading);
        } else {
            Bitmap bitmap = BitmapFactory.decodeFile(creativePath);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                creativeWidth = bitmap.getWidth();
                creativeHeight = bitmap.getHeight();
                configureMediaPosition();
            }
            imageView.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.GONE);
            startTimer();
        }
    }

    private void openPromotedApp() {
        if (storeLaunchContext != null || isFinishing() || isDestroyed()) {
            return;
        }
        storeLaunchDeadlineMs = SystemClock.elapsedRealtime() + STORE_OPEN_TIMEOUT_MS;
        // AF 6.17.2 launches through the supplied Context on a worker thread. Keep this
        // wrapper alive while waiting and gate the final launch on the main thread.
        storeLaunchContext = new ContextWrapper(this) {
            @Override
            public void startActivity(Intent intent) {
                startActivity(intent, null);
            }

            @Override
            public void startActivity(Intent intent, Bundle options) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    launchStore(this, intent, options);
                } else {
                    handler.post(() -> {
                        try {
                            launchStore(this, intent, options);
                        } catch (RuntimeException error) {
                            // Keep the loading deadline when no application can open the URL.
                            Log.w(TAG, "Unable to open promotion destination", error);
                        }
                    });
                }
            }
        };
        storeLoading.setVisibility(View.VISIBLE);
        handler.postDelayed(storeOpenTimeout, STORE_OPEN_TIMEOUT_MS);
        CrossPromotion.getInstance().performClick(storeLaunchContext, format, ad, videoCreative);
    }

    private void launchStore(Context request, Intent intent, Bundle options) {
        if (storeLaunchContext != request || isFinishing() || isDestroyed()) {
            Log.d(TAG, "Ignoring expired promotion store launch");
            return;
        }
        // Check the clock as well as the timer, in case the main thread was busy.
        if (SystemClock.elapsedRealtime() >= storeLaunchDeadlineMs) {
            storeOpenTimeout.run();
            return;
        }
        startActivity(intent, options);
        clearStoreOpening();
    }

    private void clearStoreOpening() {
        storeLaunchContext = null;
        handler.removeCallbacks(storeOpenTimeout);
        if (storeLoading != null) {
            storeLoading.setVisibility(View.GONE);
        }
    }

    private void startTimer() {
        if (canClose || timerStartTimeMs > 0) {
            return;
        }
        if (format == CrossPromotionConfig.Format.REWARDED) {
            if (videoCreative) {
                int duration = videoDurationMs > 0
                        ? videoDurationMs
                        : (mediaPlayer != null ? mediaPlayer.getDuration() : 0);
                totalDurationMs = duration > 0
                        ? duration
                        : DEFAULT_REWARD_SECONDS * 1000L;
            } else {
                totalDurationMs = REWARDED_IMAGE_SECONDS * 1000L;
            }
        } else {
            totalDurationMs = INTERSTITIAL_SKIP_SECONDS * 1000L;
        }
        timerStartTimeMs = SystemClock.elapsedRealtime();
        pausedElapsedMs = 0;
        handler.removeCallbacks(countdownTicker);
        handler.post(countdownTicker);
    }

    private void toggleSound() {
        isMuted = !isMuted;
        updateSoundIcon();
        applyVolume();
    }

    private void updateSoundIcon() {
        if (soundToggleView != null) {
            soundToggleView.setImageResource(isMuted
                    ? R.drawable.ivy_cross_promotion_ic_volume_off
                    : R.drawable.ivy_cross_promotion_ic_volume_up);
        }
    }

    private void applyVolume() {
        if (mediaPlayer != null) {
            try {
                float volume = isMuted || landingShown ? 0f : 1f;
                mediaPlayer.setVolume(volume, volume);
            } catch (Exception ignored) {
            }
        }
    }

    private void setupAppIcon(ImageView appIconView, String iconPath) {
        appIconView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                        view.getResources().getDisplayMetrics().density * 10f);
            }
        });
        appIconView.setClipToOutline(true);

        if (iconPath != null && !iconPath.isEmpty()) {
            Bitmap bitmap = BitmapFactory.decodeFile(iconPath);
            if (bitmap != null) {
                appIconView.setImageBitmap(bitmap);
                appIconView.setVisibility(View.VISIBLE);
                return;
            }
        }
        try {
            Drawable icon = getPackageManager().getApplicationIcon(ad.getTargetPackage());
            appIconView.setImageDrawable(icon);
            appIconView.setVisibility(View.VISIBLE);
        } catch (Exception ignored) {
            appIconView.setVisibility(View.GONE);
        }
    }

    private void configurePlaybackCard() {
        boolean portrait = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
        ViewGroup.LayoutParams layoutParams = bottomCard.getLayoutParams();
        layoutParams.width = portrait
                ? ViewGroup.LayoutParams.MATCH_PARENT
                : Math.round(60 * getResources().getDisplayMetrics().density);
        bottomCard.setLayoutParams(layoutParams);
        bottomCard.setBackgroundResource(portrait
                ? R.drawable.ivy_cross_promotion_card_portrait_background
                : R.drawable.ivy_cross_promotion_card_background);

        bindText(cardTitleView, ad.getTitle());
        bindText(cardDescriptionView, ad.getDescription());
        bindText(cardCallToActionView, ad.getCallToAction());
        bindText(landscapeCallToActionView, ad.getCallToAction());
        if (!portrait) {
            cardTitleView.setVisibility(View.GONE);
            cardDescriptionView.setVisibility(View.GONE);
            cardCallToActionView.setVisibility(View.GONE);
        } else {
            landscapeCallToActionView.setVisibility(View.GONE);
        }
    }

    private void configureMediaPosition() {
        if (mediaContainer == null || bottomCard == null) {
            return;
        }
        boolean portrait = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
        boolean landscapeCreative = creativeWidth > creativeHeight;
        int bottomPadding = 0;
        if (portrait && landscapeCreative) {
            ViewGroup.LayoutParams layoutParams = bottomCard.getLayoutParams();
            bottomPadding = Math.max(0, layoutParams.height);
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                bottomPadding += Math.max(0,
                        ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin);
            }
        }
        mediaContainer.setPadding(0, 0, 0, bottomPadding);
    }

    private void setupVideo(
            String creativePath,
            ImageView imageView,
            ProgressBar loading
    ) {
        imageView.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);
        loading.setVisibility(View.VISIBLE);
        videoView.setVideoPath(creativePath);
        videoView.setOnPreparedListener(mp -> {
            videoPrepared = true;
            mediaPlayer = mp;
            videoDurationMs = mp.getDuration();
            creativeWidth = mp.getVideoWidth();
            creativeHeight = mp.getVideoHeight();
            configureMediaPosition();
            applyVolume();
            updateSoundIcon();
            mp.setLooping(false);
            loading.setVisibility(View.GONE);
            if (landingShown) {
                showLandingPage();
                return;
            }
            soundToggleView.setVisibility(View.VISIBLE);
            sendImpression();
            videoView.start();
            startTimer();
        });
        videoView.setOnCompletionListener(mp -> {
            videoCompleted = true;
            pauseVideo();
            showLandingPage();
            allowClose();
            if (format == CrossPromotionConfig.Format.REWARDED
                    && !rewardSent
                    && ad != null) {
                rewardSent = true;
                CrossPromotion.getInstance().notifyReward(ad);
            }
        });
        videoView.setOnErrorListener((mp, what, extra) -> {
            loading.setVisibility(View.GONE);
            Log.e(TAG, "Video playback failed: " + what + "/" + extra);
            finish();
            return true;
        });
    }

    private void allowClose() {
        canClose = true;
        handler.removeCallbacks(countdownTicker);
        if (countdownView != null) {
            countdownView.setVisibility(View.GONE);
        }
        if (closeView != null) {
            closeView.setVisibility(View.VISIBLE);
        }
    }

    private void showLandingPage() {
        if (landingPage == null) {
            return;
        }
        boolean alreadyShown = landingShown;
        landingShown = true;
        pauseAndSilenceVideo();
        if (soundToggleView != null) {
            soundToggleView.setVisibility(View.GONE);
        }
        if (bottomCard != null) {
            bottomCard.setVisibility(View.GONE);
        }
        if (landscapeCallToActionView != null) {
            landscapeCallToActionView.setVisibility(View.GONE);
        }
        if (playbackAdBadge != null) {
            playbackAdBadge.setVisibility(View.GONE);
        }
        if (alreadyShown) {
            landingPage.animate().cancel();
            landingPage.setAlpha(1f);
            landingPage.setVisibility(View.VISIBLE);
            return;
        }
        landingPage.setAlpha(0f);
        landingPage.setVisibility(View.VISIBLE);
        landingPage.animate().alpha(1f).setDuration(220).start();
    }

    private void pauseVideo() {
        if (videoView == null || !videoCreative) {
            return;
        }
        try {
            videoView.pause();
        } catch (Exception ignored) {
        }
    }

    private void pauseAndSilenceVideo() {
        pauseVideo();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setVolume(0f, 0f);
            } catch (Exception ignored) {
            }
        }
    }

    private void bindText(TextView view, String value) {
        view.setText(value);
        view.setVisibility(value == null || value.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void sendImpression() {
        if (!impressionSent && ad != null) {
            impressionSent = true;
            CrossPromotion.getInstance().recordImpression(format, ad, videoCreative);
        }
    }

    private void enterImmersiveMode() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars()
                        | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersiveMode();
        }
        if (hasFocus && landingShown) {
            showLandingPage();
        } else if (hasFocus && !videoCreative) {
            sendImpression();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configurePlaybackCard();
        configureMediaPosition();
    }

    @Override
    public void onBackPressed() {
        if (storeLaunchContext != null) {
            clearStoreOpening();
            finish();
            return;
        }
        if (!canClose) {
            return;
        }
        if (landingShown) {
            super.onBackPressed();
        } else {
            showLandingPage();
        }
    }

    @Override
    protected void onPause() {
        clearStoreOpening();
        handler.removeCallbacks(countdownTicker);
        if (timerStartTimeMs > 0) {
            pausedElapsedMs += SystemClock.elapsedRealtime() - timerStartTimeMs;
            timerStartTimeMs = 0;
        }
        if (videoCreative && videoView != null && videoPrepared && !videoCompleted) {
            if (!landingShown) {
                pausedPosition = videoView.getCurrentPosition();
            }
            pauseVideo();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersiveMode();
        if (landingShown) {
            showLandingPage();
            if (videoView != null) {
                videoView.post(this::showLandingPage);
            }
        } else if (videoCreative
                && videoView != null
                && videoPrepared
                && !videoCompleted
                && pausedPosition > 0) {
            videoView.seekTo(pausedPosition);
            videoView.start();
            applyVolume();
        }
        if (!landingShown && !canClose && (!videoCreative || videoPrepared)) {
            if (timerStartTimeMs == 0) {
                timerStartTimeMs = SystemClock.elapsedRealtime();
            }
            handler.removeCallbacks(countdownTicker);
            handler.post(countdownTicker);
        }
    }

    @Override
    protected void onDestroy() {
        clearStoreOpening();
        handler.removeCallbacksAndMessages(null);
        if (videoView != null) {
            videoView.stopPlayback();
        }
        if (isFinishing() && ad != null) {
            CrossPromotion.getInstance().notifyClosed(format, adId);
        }
        super.onDestroy();
    }
}
