package com.ivy.base.track;

import java.util.Map;

public interface IConversationCallback {
    void onConversionDataSuccess(Map<String, Object> data);

    void onConversionDataFail(String error);

    void onAppOpenAttribution(Map<String, String> attribution);

    void onAttributionFailure(String error);
}