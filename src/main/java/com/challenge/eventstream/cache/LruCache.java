package com.challenge.eventstream.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe Least Recently Used (LRU) Cache backed by LinkedHashMap with access-order eviction.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class LruCache<K, V> {

    private final int capacity;
    private final Map<K, V> internalMap;
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    public LruCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Cache capacity must be greater than 0");
        }
        this.capacity = capacity;
        // initialCapacity, loadFactor = 0.75f, accessOrder = true (LRU order)
        this.internalMap = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    public synchronized Optional<V> get(K key) {
        if (key == null) {
            return Optional.empty();
        }
        V value = internalMap.get(key);
        if (value != null) {
            hitCount.incrementAndGet();
            return Optional.of(value);
        } else {
            missCount.incrementAndGet();
            return Optional.empty();
        }
    }

    public synchronized void put(K key, V value) {
        if (key == null || value == null) {
            return;
        }
        internalMap.put(key, value);
    }

    public synchronized boolean containsKey(K key) {
        return key != null && internalMap.containsKey(key);
    }

    public synchronized V remove(K key) {
        if (key == null) {
            return null;
        }
        return internalMap.remove(key);
    }

    public synchronized void clear() {
        internalMap.clear();
    }

    public synchronized int size() {
        return internalMap.size();
    }

    public int getCapacity() {
        return capacity;
    }

    public long getHitCount() {
        return hitCount.get();
    }

    public long getMissCount() {
        return missCount.get();
    }

    public synchronized void resetStats() {
        hitCount.set(0);
        missCount.set(0);
    }
}
