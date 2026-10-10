package ivy.data.analytics.utils;

/**
 * ITime with constant value.
 */
public class TDTimeConstant implements ITime {

    private final String mTimeString;
    private final Double mZoneOffset;

    public TDTimeConstant(String timeString, Double zoneOffset) {
        mTimeString = timeString;
        mZoneOffset = zoneOffset;
    }

    @Override
    public String getTime() {
        return mTimeString;
    }

    @Override
    public Double getZoneOffset() {
        return mZoneOffset;
    }

    @Override
    public long getTimeMilliseconds() {
        return 0;
    }

    @Override
    public String getUTCTime() {
        return "";
    }

}
