package com.ivy.max.axon;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.applovin.sdk.Axon;
import com.applovin.sdk.AxonInitializationConfiguration;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.ad.AdType;
import com.ivy.base.billing.PurchaseState;
import com.ivy.base.track.AbsTrack;
import com.ivy.base.track.EventIDs;
import com.ivy.base.track.EventParams;
import com.ivy.base.track.EventSrc;
import com.ivy.base.track.EventType;
import com.ivy.base.track.IConversationCallback;
import com.ivy.base.track.TrackPlatform;
import com.ivy.base.util.Util;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AxonTrack extends AbsTrack implements IUnProguard {

    public static final String TAG = "AXON";
    private final Map<String, Object> userProperties = new HashMap<>();

    @Override
    public void setup(Context context, String appId, String config, boolean debug, IConversationCallback conversationCallback, Runnable readyListener) {
        super.setup(context, appId, config, debug, conversationCallback, readyListener);
        String appKey = null;
        try {
            JSONObject json = new JSONObject(config);
            appKey = json.optString("app_key");
        } catch (Exception ignored) {
        }
        Axon.getInstance().setVerboseLoggingEnabled(debug);
        ILog.i(TAG, "axon start initialize with key:" + appKey);
        if (TextUtils.isEmpty(appKey)) {
            ILog.e(TAG, "invalid axon key");
            return;
        }
        AxonInitializationConfiguration initConfig = AxonInitializationConfiguration.builder(appKey).build();
        Axon.getInstance().initialize(initConfig, new Axon.InitializationListener() {
            @Override
            public void onInitialized() {
                ILog.i(TAG, "axon initialized");
            }
        });

        enableStatus = true;
    }

    public void processIntent(final Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            final Uri uri = intent.getData();
            if (uri != null) {
                Axon.getInstance().processDeepLink(uri);
            }
        }
    }

    @Override
    public void logEvent(String eventName, String eventType, String eventSrc, Map<String, Object> params, List<TrackPlatform> platforms) {
        try {
            String axonEventName = null;
            if (Objects.equals(eventSrc, EventSrc.EVENT_SRC_SDK)) {
                if (eventType.equals(EventType.EVENT_TYPE_PURCHASE)) {
                    Object _state = params.get(EventParams.EVENT_PARAM_PAY_STATE);
                    if (_state != null) {
                        int state = (int) _state;
                        if (PurchaseState.PRE_ORDER == state) {
                            axonEventName = "begin_checkout";
                        } else if (PurchaseState.PAY_RESULT == state) {
                            axonEventName = "purchase";
                        }
                    }
                } else if (eventName.equals(EventIDs.AD_SHOW_SUCCESS) && params != null) {
                    try {
                        String platform = Objects.requireNonNull(params.get("ad_network")).toString();
                        if (!platform.equalsIgnoreCase("max")) {
                            axonEventName = "ad_view";
                        }
                    }catch (Exception ignored){}
                } else if (eventName.equals(EventIDs.AD_CLICKED)) {
                    try {
                        String platform = Objects.requireNonNull(params.get("ad_network")).toString();
                        if (!platform.equalsIgnoreCase("max")) {
                            axonEventName = "ad_clicked";
                        }
                    }catch (Exception ignored){}
                } else if (eventName.equals(EventIDs.AD_TRIGGER_EVENT) && params != null && params.containsKey("ad_format")) {
                    String adType = (String) params.get("ad_format");
                    if (adType != null && adType.equals(AdType.REWARDED.value)) {
                        axonEventName = "rewarded_ad_opportunity";
                    }
                } else if (eventName.equals(EventIDs.EVENT_APP_OPEN)) {
                    axonEventName = "app_open";
                }
            } else if (Objects.equals(eventSrc, EventSrc.EVENT_SRC_CLIENT)) {
                axonEventName = eventName;
            }
            if (!TextUtils.isEmpty(axonEventName)) {
                Map<String, Object> fixedParams = new HashMap<>();
                if (params != null) {
                    fixedParams.putAll(params);
                }
                fixedParams.put(EventParams.EVENT_PARAM_EVENT_SRC, eventSrc);
                fixedParams.put(EventParams.EVENT_PARAM_ROLE_ID, Util.roleId());
                try {
                    fixedParams.putAll(userProperties);
                } catch (Exception ignored) {
                }
                Axon.getInstance().getEventService().trackEvent(eventName, fixedParams);
                ILog.i(TAG, "axon track event: " + axonEventName);
            }
        } catch (Exception e) {
            ILog.e(TAG, "axon track event failed:" + e.getMessage());
        }
    }

    @Override
    public void setUserProperty(String key, Object value) {
        try {
            userProperties.put(key, value);
        } catch (Exception ignored) {
        }
    }

}
