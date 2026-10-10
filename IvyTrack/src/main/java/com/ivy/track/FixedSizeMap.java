package com.ivy.track;

import java.util.LinkedHashMap;
import java.util.Map;

public class FixedSizeMap<K, V> extends LinkedHashMap<K, V> {

    private final int maxSize;

    public FixedSizeMap(int maxSize) {
        super(maxSize + 1, 0.75f, false); // FIFO
        this.maxSize = maxSize;
    }

    @Override
    public V put(K key, V value) {
        if (containsKey(key)) {
            // 手动移除，重新插入，刷新顺序
            super.remove(key);
        }
        return super.put(key, value);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
