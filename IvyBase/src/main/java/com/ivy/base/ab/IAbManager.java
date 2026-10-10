package com.ivy.base.ab;

public abstract class IAbManager {

    public abstract String getAbtestSSID();

    public abstract void setHeaderInfo(String key, Object value);

    public abstract void setUserInfo(String key, Object value);

    public abstract String getAllAbConfigs();

    public abstract String getAbConfigString(String key, String defaultValue);

    public abstract int getAbConfigInt(String key, int defaultValue);

    public abstract long getAbConfigLong(String key, long defaultValue);

    public abstract double getAbConfigDouble(String key, double defaultValue);

    public abstract boolean getAbConfigBoolean(String key, boolean defaultValue);

    public abstract float getAbConfigFloat(String key, float defaultValue);

}
