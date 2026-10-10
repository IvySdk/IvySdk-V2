/*
 * Copyright (C) 2022 ThinkingData
 */

package ivy.data.analytics.utils;

import android.os.Build;
import android.os.SystemClock;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * time calibration
 */
public class TDTimeCalibrated implements ITime {
    private final long mSystemElapsedRealtime;
    private final TimeZone mTimeZone;
    private final ICalibratedTime mCalibratedTime;

    private Date mDate;

    /**
     * TDTimeCalibrated
     *
     * @param calibratedTime calibratedTime
     * @param timeZone time zone
     */
    public TDTimeCalibrated(ICalibratedTime calibratedTime, TimeZone timeZone) {
        mCalibratedTime = calibratedTime;
        mTimeZone = timeZone;
        mSystemElapsedRealtime = SystemClock.elapsedRealtime();
    }

    private synchronized Date getDate() {
        if (null == mDate) {
            mDate = mCalibratedTime.get(mSystemElapsedRealtime);
        }
        return mDate;
    }

    @Override
    public String getTime() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Instant instant = Instant.ofEpochMilli(mDate.getTime());
                LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TDConstants.TIME_PATTERN).withLocale(Locale.ENGLISH);
                return localDateTime.format(formatter);
            } else {
                SimpleDateFormat localFormatter = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.ENGLISH);
                return localFormatter.format(mDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Double getZoneOffset() {
        return TDUtils.getTimezoneOffset(getDate().getTime(), mTimeZone);
    }

    @Override
    public long getTimeMilliseconds() {
        return mDate.getTime();
    }

    @Override
    public String getUTCTime() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Instant instant = Instant.ofEpochMilli(mDate.getTime());
                LocalDateTime utcDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
                // 格式化输出
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TDConstants.TIME_PATTERN).withLocale(Locale.ENGLISH);
                return utcDateTime.format(formatter);
            } else {
                // UTC时间格式化
                SimpleDateFormat utcFormatter = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.ENGLISH);
                utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                return utcFormatter.format(mDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
