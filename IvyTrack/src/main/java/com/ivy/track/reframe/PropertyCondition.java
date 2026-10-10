package com.ivy.track.reframe;

public final class PropertyCondition {
    private final String property;
    private final String operation;
    private final Object value;

    public PropertyCondition(String property, String operation, Object value) {
        this.property = property;
        this.operation = operation;
        this.value = value;
    }

    public String getProperty() {
        return property;
    }

    public String getOperation() {
        return operation;
    }

    public Object getValue() {
        return value;
    }



    @Override
    public String toString() {
        return "PropertyCondition{" +
                "property='" + property + '\'' +
                ", operation='" + operation + '\'' +
                ", value=" + value +
                '}';
    }
}