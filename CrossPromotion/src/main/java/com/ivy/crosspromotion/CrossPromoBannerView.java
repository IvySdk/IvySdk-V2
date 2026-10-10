package com.ivy.crosspromotion;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

public final class CrossPromoBannerView extends FrameLayout {
    private static final String TAG = "CrossPromotion";
    private static final long STORE_OPEN_TIMEOUT_MS = 3_000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ImageView imageView;
    private final ImageView iconView;
    private final TextView titleView;
    private final TextView descriptionView;
    private final TextView callToActionView;
    private final ProgressBar loadingView;
    private boolean impressionSent;
    private CrossPromotionConfig.Ad ad;
    private Context storeLaunchContext;

    private final Runnable storeOpenTimeout = () -> {
        if (storeLaunchContext != null && ad != null) {
            Log.w(TAG, "Banner AppsFlyer store launch timed out; falling back to direct openStore");
            clearStoreOpening();
            Context context = getContext();
            if (context != null) {
                CrossPromotion.getInstance().openStore(
                        context, ad.getTargetPackage(), ad.getId(), CrossPromotionConfig.Format.BANNER);
            }
        }
    };

    public CrossPromoBannerView(Context context) {
        this(context, null);
    }

    public CrossPromoBannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMinimumHeight(dp(50));
        setBackgroundResource(R.drawable.ivy_cross_promotion_banner_background);
        LayoutInflater.from(context).inflate(R.layout.ivy_cross_promotion_banner, this, true);
        imageView = findViewById(R.id.ivy_cross_promotion_banner_image);
        iconView = findViewById(R.id.ivy_cross_promotion_banner_icon);
        titleView = findViewById(R.id.ivy_cross_promotion_banner_title);
        descriptionView = findViewById(R.id.ivy_cross_promotion_banner_description);
        callToActionView = findViewById(R.id.ivy_cross_promotion_banner_cta);
        loadingView = findViewById(R.id.ivy_cross_promotion_banner_loading);
    }

    void bind(CrossPromotionConfig.Ad ad, File creative, File iconCreative) {
        clearStoreOpening();
        this.ad = ad;
        bindText(titleView, ad.getTitle());
        bindText(descriptionView, ad.getDescription());
        bindText(callToActionView, ad.getCallToAction());
        setContentDescription((ad.getTitle() + " " + ad.getDescription()).trim());
        OnClickListener clickListener = view -> openPromotedApp();
        setOnClickListener(clickListener);
        callToActionView.setOnClickListener(clickListener);

        Bitmap bitmap = BitmapFactory.decodeFile(creative.getAbsolutePath());
        if (bitmap == null) {
            setVisibility(GONE);
            return;
        }
        imageView.setImageBitmap(bitmap);
        Bitmap iconBitmap = BitmapFactory.decodeFile(iconCreative.getAbsolutePath());
        if (iconBitmap == null) {
            setVisibility(GONE);
            return;
        }
        iconView.setImageBitmap(iconBitmap);
        sendImpressionIfVisible();
    }

    private void openPromotedApp() {
        if (storeLaunchContext != null || ad == null) {
            return;
        }
        Context baseContext = getContext();
        if (baseContext == null) {
            return;
        }
        setLoading(true);
        mainHandler.postDelayed(storeOpenTimeout, STORE_OPEN_TIMEOUT_MS);

        storeLaunchContext = new ContextWrapper(baseContext) {
            @Override
            public void startActivity(Intent intent) {
                startActivity(intent, null);
            }

            @Override
            public void startActivity(Intent intent, Bundle options) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    launchStore(this, intent, options);
                } else {
                    mainHandler.post(() -> {
                        try {
                            launchStore(this, intent, options);
                        } catch (RuntimeException error) {
                            Log.w(TAG, "Unable to open banner promotion destination", error);
                        }
                    });
                }
            }
        };

        CrossPromotion.getInstance().performClick(
                storeLaunchContext, CrossPromotionConfig.Format.BANNER, ad, false);
    }

    private void launchStore(Context request, Intent intent, Bundle options) {
        if (storeLaunchContext != request) {
            Log.d(TAG, "Ignoring expired banner promotion store launch");
            return;
        }
        clearStoreOpening();
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            context.startActivity(intent, options);
        } catch (Exception error) {
            Log.w(TAG, "Failed to launch intent from AppsFlyer, falling back to openStore", error);
            if (ad != null) {
                CrossPromotion.getInstance().openStore(
                        context, ad.getTargetPackage(), ad.getId(), CrossPromotionConfig.Format.BANNER);
            }
        }
    }

    private void clearStoreOpening() {
        storeLaunchContext = null;
        mainHandler.removeCallbacks(storeOpenTimeout);
        setLoading(false);
    }

    private void setLoading(boolean loading) {
        if (loadingView != null) {
            loadingView.setVisibility(loading ? VISIBLE : GONE);
        }
        if (callToActionView != null) {
            if (loading) {
                callToActionView.setText("");
            } else {
                callToActionView.setText(ad != null ? ad.getCallToAction() : "");
            }
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != VISIBLE) {
            clearStoreOpening();
        }
        sendImpressionIfVisible();
    }

    @Override
    protected void onDetachedFromWindow() {
        clearStoreOpening();
        super.onDetachedFromWindow();
    }

    public void onAdShown() {
        impressionSent = false;
        setVisibility(VISIBLE);
        post(this::sendImpressionIfVisible);
    }

    private void sendImpressionIfVisible() {
        if (!impressionSent
                && ad != null
                && imageView.getDrawable() != null
                && isShown()
                && getWindowVisibility() == VISIBLE) {
            impressionSent = true;
            CrossPromotion.getInstance().recordImpression(
                    CrossPromotionConfig.Format.BANNER, ad, false);
        }
    }

    private void bindText(TextView view, String value) {
        view.setText(value);
        view.setVisibility(value == null || value.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
