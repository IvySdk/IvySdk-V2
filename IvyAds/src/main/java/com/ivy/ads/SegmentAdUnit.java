package com.ivy.ads;

// SegmentAdUnit Java 实现
public class SegmentAdUnit {
    private final String name;
    private final String adUnit;
    private final String amazonSlotId;

    public SegmentAdUnit(String name, String adUnit, String amazonSlotId) {
        this.name = name;
        this.adUnit = adUnit;
        this.amazonSlotId = amazonSlotId;
    }

    public String getName() {
        return name;
    }

    public String getAdUnit() {
        return adUnit;
    }

    public String getAmazonSlotId() {
        return amazonSlotId;
    }
}
