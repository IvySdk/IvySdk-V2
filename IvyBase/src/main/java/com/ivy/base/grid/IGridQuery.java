package com.ivy.base.grid;

public interface IGridQuery {

    String getGridConfig(String key, String defaultValue);

    int getGridConfig(String key, int defaultValue);

    double getGridConfig(String key, double defaultValue);

    boolean getGridConfig(String key, boolean defaultValue);

    long getGridConfig(String key, long defaultValue);
}
