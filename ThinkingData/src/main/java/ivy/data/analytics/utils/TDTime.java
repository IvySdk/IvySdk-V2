/*
 * Copyright (C) 2022 ThinkingData
 */

package ivy.data.analytics.utils;

import android.os.Build;

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
 * Encapsulation time class
 * */
public class TDTime implements ITime {

    private final TimeZone mTimeZone;

    private final Date mDate;

    private boolean enableZoneOffset = true;

    public boolean mCalibrationDisuse= false;

    public TDTime(Date date, TimeZone timeZone) {
        mDate = date == null ? new Date() : date;
        mTimeZone = timeZone;
    }

    /**
     * getTime
     *
     * @return {@link String}
     */
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


    public void disableZoneOffset() {
        enableZoneOffset = false;
    }

    /**
     * getZoneOffset
     *
     * @return {@link Double}
     */
    public Double getZoneOffset() {
        if (enableZoneOffset && mTimeZone != null) {
            return TDUtils.getTimezoneOffset(mDate.getTime(), mTimeZone);
        }
        return null;
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
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TDConstants.TIME_PATTERN).withLocale(Locale.ENGLISH);
               return utcDateTime.format(formatter);
            } else {
                SimpleDateFormat utcFormatter = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.ENGLISH);
                utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                return utcFormatter.format(mDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setCalibrationDisuse(boolean isDisuse){
        this.mCalibrationDisuse = isDisuse;
    }

}
