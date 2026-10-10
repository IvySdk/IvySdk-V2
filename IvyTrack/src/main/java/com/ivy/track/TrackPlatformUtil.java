package com.ivy.track;

import com.ivy.base.ILog;
import com.ivy.base.track.AbsTrack;
import com.ivy.base.track.TrackPlatform;

public class TrackPlatformUtil {

    private static final String TAG = "TrackPlatformUtil";

    public static AbsTrack instantiateTrackPlatform(TrackPlatform platform) {
        switch (platform) {
            case NONE:
                ILog.w(TAG, "none platform find for " + platform.getValue());
                return null;

            case PANGLE:
                try {
                    return (AbsTrack) Class.forName("com.ivy.ads.PangleTrack").getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    ILog.e(TAG, "instance platform:" + platform + " failed;" + (e.getMessage() != null ? e.getMessage() : ""));
                }
                return null;

            case FIREBASE:
                try {
                    return (AbsTrack) Class.forName("com.ivy.firebase.FirebaseTrack").getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    ILog.e(TAG, "instance platform:" + platform + " failed;" + (e.getMessage() != null ? e.getMessage() : ""));
                }
                return null;

            case FACEBOOK:
                try {
                    return (AbsTrack) Class.forName("com.ivy.facebook.FacebookTrack").getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    ILog.e(TAG, "instance platform:" + platform + " failed;" + (e.getMessage() != null ? e.getMessage() : ""));
                }
                return null;

            case APPSFLYER:
                try {
                    return (AbsTrack) Class.forName("com.ivy.appsflyer.AppsflyerTrack").getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    ILog.e(TAG, "instance platform:" + platform + " failed;" + (e.getMessage() != null ? e.getMessage() : ""));
                }
                return null;

            case THINKING_DATA:
                try {
                    return (AbsTrack) Class.forName("ivy.data.analytics.ThinkingDataTrack").getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    ILog.e(TAG, "instance platform:" + platform + " failed;" + (e.getMessage() != null ? e.getMessage() : ""));
                }
                return null;
            case AXON:
                try {
                    return (AbsTrack) Class.forName("com.ivy.max.axon.AxonTrack").getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    ILog.e(TAG, "instance platform:" + platform + " failed;" + (e.getMessage() != null ? e.getMessage() : ""));
                }
                return null;

            default:
                return null;
        }
    }

    public static TrackPlatform getTrackPlatform(int v) {
        switch (v) {
            case 0:
                return TrackPlatform.NONE;
            case 1:
                return TrackPlatform.FACEBOOK;
            case 2:
                return TrackPlatform.FIREBASE;
            case 3:
                return TrackPlatform.APPSFLYER;
            case 4:
                return TrackPlatform.THINKING_DATA;
            case 5:
                return TrackPlatform.PANGLE;
            default:
                return TrackPlatform.NONE;
        }
    }
}
