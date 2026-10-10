package com.ivy.appsflyer;

import android.content.Context;

import org.json.JSONObject;

import com.appsflyer.AFLogger;
import com.appsflyer.AppsFlyerConversionListener;
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.attribution.AppsFlyerRequestListener;
import com.appsflyer.deeplink.DeepLink;
import com.appsflyer.deeplink.DeepLinkResult;

import com.ivy.base.ILog;
import com.ivy.base.local.LocalStorage;

public class AppsflyerInit {

    public static final String TAG = "Appsflyer";

    private Context context;
    private String config;
    private boolean debug;

    public AppsflyerInit(Context context, String config, boolean debug) {
        this.context = context;
        this.config = config;
        this.debug = debug;
    }

    public void initAppsflyer(final AppsFlyerConversionListener listener, final InitResultCallback initResult) {
        try {
            JSONObject obj = new JSONObject(config);
            String appKey = obj.optString("app_key");

            if (appKey == null || appKey.isEmpty()) {
                ILog.w(TAG, "app key invalid !!!");
                initResult.onResult(false);
                return;
            }

            if (debug) {
                AppsFlyerLib.getInstance().setLogLevel(AFLogger.LogLevel.VERBOSE);
                AppsFlyerLib.getInstance().setDebugLog(true);
                AppsFlyerLib.getInstance().setMinTimeBetweenSessions(0);
            }
            try {
                JSONObject preInstall = obj.optJSONObject("preInstall");
                if (preInstall != null && preInstall.has("mediaSource")) {
                    String mediaSource = preInstall.optString("mediaSource").trim();
                    String campaign = preInstall.optString("campaign");
                    String siteId = preInstall.optString("siteId");
                    if (mediaSource.isEmpty()) {
                        ILog.i(TAG, "no preinstall data");
                    } else {
                        //mediaSource\campaign\siteId
                        AppsFlyerLib.getInstance().setPreinstallAttribution(mediaSource, campaign, siteId);
                    }
                }
            } catch (Exception ignored) {
            }

            String inviterTemplateId = obj.optString("inviter_template_id");
            if (inviterTemplateId != null && !inviterTemplateId.isEmpty()) {
                AppsFlyerLib.getInstance().setAppInviteOneLink(inviterTemplateId);

                AppsFlyerLib.getInstance().subscribeForDeepLink(result -> {
                    if (result == null) return;

                    switch (result.getStatus()) {
                        case ERROR:
                            ILog.e(TAG, "deep link err:\n" + result.getError());
                            break;
                        case NOT_FOUND:
                            ILog.i(TAG, "deep link not found");
                            break;
                        case FOUND:
                            DeepLink deepLink = result.getDeepLink();
                            if (deepLink != null) {
                                ILog.i(TAG, "deep link: " + deepLink + ";\n deferred status: " + deepLink.isDeferred());
                                JSONObject clickEvent = deepLink.getClickEvent();
                                if (clickEvent != null) {
                                    String inviterUserId = clickEvent.optString("deep_link_sub1");
                                    if (inviterUserId == null || inviterUserId.isEmpty()) {
                                        ILog.i(TAG, "not valid inviter user id");
                                        return;
                                    }

                                    String currentUserId = LocalStorage.Instance.decodeString("invite_current_user_id", "-");
                                    if (currentUserId.equals(inviterUserId)) {
                                        ILog.i(TAG, "user id equal inviter id, same user");
                                    } else {
                                        ILog.i(TAG, "inviter user id:" + inviterUserId);
                                        String inviterAppId = clickEvent.optString("deep_link_sub2");
                                        if (inviterAppId == null || inviterAppId.isEmpty()) {
                                            inviterUserId = inviterUserId + "|" + inviterAppId;
                                        }
                                        LocalStorage.Instance.encodeString("af_invite_id", inviterUserId);
                                    }
                                } else {
                                    ILog.i(TAG, "deep link click event is null");
                                }
                            } else {
                                ILog.i(TAG, "deep link is null");
                            }
                            break;
                    }
                });
            }

            AppsFlyerLib.getInstance().init(appKey, listener, context);
         
            AppsFlyerLib.getInstance().start(context, appKey, new AppsFlyerRequestListener() {
                @Override
                public void onSuccess() {
                    ILog.i(TAG, "init success");
                    initResult.onResult(true);
                }

                @Override
                public void onError(int p0, String p1) {
                    ILog.e(TAG, "init failed:\n error code: " + p0 + ";\n error msg:" + p1);
                    initResult.onResult(false);
                }
            });

        } catch (Exception e) {
            ILog.e(TAG, "init failed:" + e.getMessage());
            initResult.onResult(false);
        }
    }

    public interface InitResultCallback {
        void onResult(boolean status);
    }
}
