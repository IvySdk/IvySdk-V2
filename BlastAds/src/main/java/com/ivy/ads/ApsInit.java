package com.ivy.ads;

import android.app.Activity;

import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.MRAIDPolicy;

import org.json.JSONObject;

import java.util.Map;

public class ApsInit implements IPlatformInit {
    @Override
    public void init(Activity activity, JSONObject config, boolean debug, InitCallback listener) {
        AdRegistration.getInstance(config.optString("id"), activity);
        AdRegistration.setMRAIDSupportedVersions(new String[]{"1.0", "2.0", "3.0"});
        AdRegistration.setMRAIDPolicy(MRAIDPolicy.CUSTOM);
    }

    @Override
    public void logEvent(String eventName, Map<String, Object> map) {

    }
}
