package com.ivy.track.property;

import android.os.Build;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtil {
    private final long timestamp;
    private String localTimeStr;
    private String utcTimeStr;

    public TimeUtil(long timestamp) {
        this.timestamp = timestamp;
        formatTime();
    }

    private void formatTime() {
        String timeFormat = "yyyy-MM-dd";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant instant = Instant.ofEpochMilli(timestamp);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat)
                            .withLocale(Locale.US)
                            .withDecimalStyle(DecimalStyle.STANDARD);
            localTimeStr = instant.atZone(ZoneId.systemDefault()).format(formatter);
            utcTimeStr = instant.atZone(ZoneOffset.UTC).format(formatter);
        } else {
            Date date = new Date(timestamp);
            SimpleDateFormat formatter = new SimpleDateFormat(timeFormat, Locale.US);
            // local
            localTimeStr = formatter.format(date);
            // utc
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            utcTimeStr = formatter.format(date);
        }
    }

    public String getLocalTimeStr() {
        return localTimeStr;
    }

    public String getUtcTimeStr() {
        return utcTimeStr;
    }
}