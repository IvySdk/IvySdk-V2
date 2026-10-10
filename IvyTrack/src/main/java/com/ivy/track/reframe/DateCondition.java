package com.ivy.track.reframe;

public final class DateCondition {
    private final String operation;
    private final int days;
    private final int times;

    public DateCondition(String operation, int days, int times) {
        this.operation = operation;
        this.days = days;
        this.times = times;
    }

    public String getOperation() {
        return operation;
    }

    public int getDays() {
        return days;
    }

    public int getTimes() {
        return times;
    }

    @Override
    public String toString() {
        return "DateCondition{" +
                "operation='" + operation + '\'' +
                ", days=" + days +
                ", times=" + times +
                '}';
    }
}