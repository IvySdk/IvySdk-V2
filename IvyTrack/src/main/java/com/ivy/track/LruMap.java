package com.ivy.track;

import java.util.LinkedHashMap;
import java.util.Map;

public class LruMap<K, V> extends LinkedHashMap<K, V> {

    private final int maxSize;

    public LruMap(int maxSize) {
        // true = 按访问顺序（LRU）
        super(maxSize + 1, 0.75f, true);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}