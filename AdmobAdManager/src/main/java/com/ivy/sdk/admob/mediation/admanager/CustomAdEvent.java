package com.ivy.sdk.admob.mediation.admanager;

public class CustomAdEvent {

    public static final String DOMAIN = "Google ad manager";

    // Google ad manager app id
    public static final String KEY_APP_ID = "appId";

    /**
     * Google ad manager ad Id
     */
    public static final String KEY_PLACEMENT_ID = "adId";

    public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

    /**
     * Google ad manager returned a placement with no fill.
     */
    public static final int ERROR_PLACEMENT_STATE_NO_FILL = 102;

    /**
     * Google ad manager returned a disabled placement.
     */
    public static final int ERROR_PLACEMENT_STATE_DISABLED = 103;

    /**
     * Tried to show an ad with a `null` context.
     */
    public static final int ERROR_NULL_CONTEXT = 104;

    /**
     * Tried to show an ad with a non-Activity context.
     */
    public static final int ERROR_CONTEXT_NOT_ACTIVITY = 105;

    /**
     * Tried to show an ad that's not ready to be shown.
     */
    public static final int ERROR_AD_NOT_READY = 106;

    /**
     * Google ad manager is not supported on the device.
     */
    public static final int ERROR_UNITY_ADS_NOT_SUPPORTED = 107;

    /**
     * Google ad manager can only load 1 ad per placement at a time.
     */
    public static final int ERROR_AD_ALREADY_LOADING = 108;

    /**
     * Google ad manager finished with an error state.
     */
    public static final int ERROR_FINISH = 109;

    /**
     * The requested ad size does not match a UnityAds supported banner size.
     */
    public static final int ERROR_BANNER_SIZE_MISMATCH = 110;

    /**
     * Google ad manager returned an initialization error.
     */
    public static final int ERROR_INITIALIZATION_FAILURE = 111;

    // 私有构造防止实例化
    private CustomAdEvent() {
    }
}
