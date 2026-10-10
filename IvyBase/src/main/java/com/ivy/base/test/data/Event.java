package com.ivy.base.test.data;

public class Event {

    public int seq = 0;

    public String tag;
    public String info;
    public EventState state;
    public String data;
    public long timestamp;
    public String dateStr;

    public Event(String tag, int seq, String info, EventState state, String data, long timestamp, String dateStr) {
        this.tag = tag;
        this.seq = seq;
        this.info = info;
        this.state = state;
        this.data = data;
        this.timestamp = timestamp;
        this.dateStr = dateStr;
    }
}


