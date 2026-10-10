package com.ivy.base.util;


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.google.android.play.core.review.ReviewException;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.ivy.base.ActivityUtil;
import com.ivy.base.App;
import com.ivy.base.ILog;
import com.ivy.base.local.LocalStorage;

public class AppStoreUtil {

    /**
     * 每5次评价，触发一次应用内评价
     */
    public static void rateUs(int star, boolean debug) {
        int requestCount = LocalStorage.Instance.decodeInt("_last_in_app_rate_", 0);
        if (requestCount % 3 != 0) {
            LocalStorage.Instance.encodeInt("_last_in_app_rate_", requestCount + 1);
            openAppStore(null, null);
            return;
        }
        LocalStorage.Instance.encodeInt("_last_in_app_rate_", 1);

        ReviewManager manager = ReviewManagerFactory.create(App.getInstance());
        manager.requestReviewFlow().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                if (ActivityUtil.getInstance().getActivity() != null) {
                    manager.launchReviewFlow(ActivityUtil.getInstance().getActivity(), reviewInfo)
                            .addOnCompleteListener(flow -> {
                                ILog.i("", "no");
                                // The flow has finished. The API does not indicate whether the user
                                // reviewed or not, or even whether the review dialog was shown. Thus, no
                                // matter the result, we continue our app flow.
                            });
                } else {
                    openAppStore(App.getInstance().getPackageName(), null);
                }
            } else {
                try {
                    int reviewErrorCode = ((ReviewException) task.getException()).getErrorCode();
                    ILog.e("review", "show in-app review err:" + reviewErrorCode);
                } catch (Exception ignored) {
                }
                openAppStore(App.getInstance().getPackageName(), null);
            }
        });
    }

    public static void openAppStore(String url, String referrer) {
        String path = url != null ? url : App.getInstance().getPackageName();
        if (path.toLowerCase().startsWith("market://details?id=")) {
            String fixedUrl = (referrer != null)
                    ? appendReferrer(App.getInstance().getPackageName(), path, referrer)
                    : path;
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl));
                ActivityUtil.getInstance().getActivity().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                String resetUrl = fixedUrl.replaceFirst("(?i)market://details\\?id=",
                        "https://play.google.com/store/apps/details?id=");
                startBrowser(resetUrl);
            } catch (Exception ignored) {
            }
        } else if (path.toLowerCase().startsWith("http")) {
            String fixedUrl = (referrer != null)
                    ? appendReferrer(App.getInstance().getPackageName(), path, referrer)
                    : path;
            if (fixedUrl.toLowerCase().startsWith("https://play.google.com/store/apps/details?id=")) {
                if (hasGooglePlayStoreInstalled()) {
                    launchGooglePlayStore(fixedUrl);
                } else {
                    startBrowser(fixedUrl);
                }
            } else {
                startBrowser(fixedUrl);
            }
        } else {
            String marketUrl = "https://play.google.com/store/apps/details?id=" + path;
            openAppStore(marketUrl, referrer);
        }
    }

    private static boolean hasGooglePlayStoreInstalled() {
        try {
            int playStoreEnabled = App.getInstance().getPackageManager()
                    .getApplicationEnabledSetting("com.android.vending");
            return playStoreEnabled == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                    || playStoreEnabled == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        } catch (Exception ignored) {
        }
        return false;
    }

    private static void launchGooglePlayStore(String url) {
        try {
            String packageName = url.replace("https://play.google.com/store/apps/details?id=", "");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            intent.setPackage("com.android.vending");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ActivityUtil.getInstance().getActivity().startActivity(intent);
        } catch (Exception e) {
            startBrowser(url);
        }
    }

    private static void startBrowser(String url) {
        IvyUtil.openUrlWithBrowser(url);
        // 也可以使用原生方式：
        // try {
        //     Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        //     App.Instance.getActivity().startActivity(webIntent);
        // } catch (Exception ignored) {}
    }

    private static String appendReferrer(String srcPkg, String url, String tag) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        if (url.contains("referrer=")) {
            return url;
        }
        // 使用三元运算符来决定是用 '?' 还是 '&'
        char separator = url.contains("?") ? '&' : '?';
        // 直接在原始url后面拼接
        return url + separator + "referrer=utm_source%3D" + "ivy" +
                "%26utm_campaign%3D" + srcPkg +
                "%26utm_medium%3D" + tag +
                "%26utm_term%3D" + tag +
                "%26utm_content%3D" + tag;
    }
}
