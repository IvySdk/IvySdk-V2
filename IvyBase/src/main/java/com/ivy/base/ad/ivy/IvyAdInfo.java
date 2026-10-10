package com.ivy.base.ad.ivy;

public class IvyAdInfo {
    private final IvyAdType mAdType;
    private final IvyAdapterInfo mAdapter;

    public IvyAdInfo(IvyAdType adType, IvyAdapterInfo adapter) {
        this.mAdType = adType;
        this.mAdapter = adapter;
    }

    public IvyAdType getAdType() {
        return this.mAdType;
    }

    public IvyAdapterInfo getAdapter() {
        return this.mAdapter;
    }
}
