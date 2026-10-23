package com.challenge.eventstream.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LruCacheTest {

    @Test
    @DisplayName("Should store and retrieve items within capacity")
    void testBasicPutAndGet() {
        LruCache<String, String> cache = new LruCache<>(3);
        cache.put("k1", "v1");
        cache.put("k2", "v2");

        assertEquals(Optional.of("v1"), cache.get("k1"));
        assertEquals(Optional.of("v2"), cache.get("k2"));
        assertEquals(Optional.empty(), cache.get("k3"));
        assertEquals(2, cache.getHitCount());
        assertEquals(1, cache.getMissCount());
    }

    @Test
    @DisplayName("Should evict least recently used item when capacity is exceeded")
    void testLruEviction() {
        LruCache<String, String> cache = new LruCache<>(3);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");

        // Access "a", making "b" the least recently used
        cache.get("a");

        // Add 4th item, "b" should be evicted
        cache.put("d", "4");

        assertEquals(3, cache.size());
        assertTrue(cache.containsKey("a"));
        assertFalse(cache.containsKey("b"), "Key 'b' should have been evicted as LRU");
        assertTrue(cache.containsKey("c"));
        assertTrue(cache.containsKey("d"));
    }

    @Test
    @DisplayName("Should update access order on get")
    void testAccessOrderUpdate() {
        LruCache<String, Integer> cache = new LruCache<>(2);
        cache.put("x", 10);
        cache.put("y", 20);

        // Access x -> y is now LRU
        cache.get("x");

        // Add z -> y should be evicted
        cache.put("z", 30);

        assertEquals(Optional.of(10), cache.get("x"));
        assertEquals(Optional.empty(), cache.get("y"));
        assertEquals(Optional.of(30), cache.get("z"));
    }

    @Test
    @DisplayName("Should handle clear properly")
    void testClear() {
        LruCache<String, String> cache = new LruCache<>(5);
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        assertEquals(2, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("k1"));
    }
}
