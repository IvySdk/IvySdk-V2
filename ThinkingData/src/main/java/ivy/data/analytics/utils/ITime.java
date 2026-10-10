/*
 * Copyright (C) 2022 ThinkingData
 */

package ivy.data.analytics.utils;

/**
 * ITime.
 * */
public interface ITime {

    String getTime();

    Double getZoneOffset();

    long getTimeMilliseconds();

    String getUTCTime();

}
