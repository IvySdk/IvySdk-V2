package com.ivy.base.track;


public final class EventIDs {
    public static final String EVENT_APP_OPEN = "sdk_app_open";
    public static final String SDK_FIRST_APP_OPEN = "sdk_first_open";

    public static final String EVENT_APP_OPEN_IVY = "sdk_app_open_ivy";
    public static final String SDK_FIRST_APP_OPEN_IVY = "sdk_first_open_ivy";
    public static final String SDK_FIRST_AF_APP_OPEN_ATTRIBUTION = "af_app_open_attribution";
    public static final String SDK_FIRST_AF_CONVERSION = "af_conversion";
    public static final String GMS_AD_PAID = "gms_ad_paid_event";

    public static final String PLATFORM_INIT_START = "sdk_ads_init";
    public static final String PLATFORM_INIT_COMPLETED = "sdk_ads_complete";
    public static final String AD_REQUEST = "sdk_ad_request";
    public static final String AD_LOAD_SUCCESS = "sdk_ad_loaded";
    public static final String AD_LOAD_FAILED = "sdk_ad_load_failed";
    public static final String AD_SHOW_SUCCESS = "sdk_ad_shown";
    public static final String AD_SHOW_FAILED = "sdk_ad_show_failed";
    public static final String AD_SHOW_NOT_READY = "sdk_ad_show_not_ready";
    public static final String AD_CLICKED = "sdk_ad_clicked";
    public static final String AD_CLOSED = "sdk_ad_closed";
    public static final String AD_REWARD_USER = "sdk_ad_rewarded";
    public static final String AD_IMPRESSION_REVENUE = "af_ad_revenue";
    public static final String AD_IMPRESSION_REVENUE_FIREBASE = "ad_impression_revenue";
    public static final String AD_REACH_PAGE = "sdk_ad_fired";
    public static final String AD_STATE_CHECK_EVENT = "sdk_has_ad";
    public static final String AD_TRIGGER_EVENT = "sdk_ad_triggered";

    public static final String IAP_PRE_ORDER = "sdk_iap_pre_order";
    public static final String IAP_VERIFICATION = "sdk_iap_verification";
    public static final String IAP_PURCHASED = "sdk_iap_purchased";
    public static final String AF_IAP_PURCHASED = "af_purchase";
    public static final String IAP_USER_CANCEL = "sdk_iap_canceled";

    public static final String DEVICE_INFO = "sdk_device_info";
    public static final String EVENT_START_TRACK_EVENT = "start_track_event";
    public static final String EVENT_ADVERT_PROCESS = "sdk_advert_process";
//    public static final String EVENT_PUSH_CLICKED = "sdk_push_clicked";
    public static final String EVENT_PUSH_REWARDED = "sdk_push_rewarded";
    public static final String EVENT_PUSH_REACH = "sdk_push_reach";
}
