package com.ivy.base.track;

import java.util.Map;

public interface IHuoShan {

    void onEvent(String eventName, String eventType, String eventSrc, Map<String, Object>  lowerParams);

}
