package com.ivy.base.test.view;

import java.util.Map;

public class PropertyData {
    private final String name;
    private final Map<String, Object> properties;

    public PropertyData(String name, Map<String, Object> properties) {
        this.name = name;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
