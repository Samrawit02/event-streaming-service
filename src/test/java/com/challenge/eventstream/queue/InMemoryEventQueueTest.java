package com.challenge.eventstream.queue;

import com.challenge.eventstream.dto.EventResponse;
import com.challenge.eventstream.model.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryEventQueueTest {

    @Test
    @DisplayName("Should maintain maximum capacity of 500 events and evict oldest FIFO")
    void testCapacityEnforcement() {
        InMemoryEventQueue queue = new InMemoryEventQueue(500);

        for (int i = 1; i <= 600; i++) {
            Event event = new Event((long) i, "VIEW", (long) i, "electronics", LocalDateTime.now());
            event.setId((long) i);
            queue.add(event);
        }

        assertEquals(500, queue.size(), "Queue size should be capped at 500");

        // The recent 20 events should be ids 600 down to 581
        List<EventResponse> recent20 = queue.getRecent(20);
        assertEquals(20, recent20.size());
        assertEquals(600L, recent20.get(0).getId());
        assertEquals(581L, recent20.get(19).getId());
    }

    @Test
    @DisplayName("Should return latest 20 events in newest-first order")
    void testGetRecent() {
        InMemoryEventQueue queue = new InMemoryEventQueue(500);

        for (int i = 1; i <= 30; i++) {
            Event event = new Event((long) i, "VIEW", 100L, "books", LocalDateTime.now());
            event.setId((long) i);
            queue.add(event);
        }

        List<EventResponse> recent = queue.getRecent(20);
        assertEquals(20, recent.size());
        assertEquals(30L, recent.get(0).getId());
        assertEquals(11L, recent.get(19).getId());
    }

    @Test
    @DisplayName("Should calculate summary grouped by eventType correctly")
    void testGetSummary() {
        InMemoryEventQueue queue = new InMemoryEventQueue(500);

        // 3 VIEW, 2 ADD_TO_CART, 1 PURCHASE
        queue.add(new Event(1L, "VIEW", 10L, "phones", LocalDateTime.now()));
        queue.add(new Event(2L, "VIEW", 11L, "phones", LocalDateTime.now()));
        queue.add(new Event(3L, "VIEW", 12L, "laptops", LocalDateTime.now()));
        queue.add(new Event(4L, "ADD_TO_CART", 10L, "phones", LocalDateTime.now()));
        queue.add(new Event(5L, "ADD_TO_CART", 12L, "laptops", LocalDateTime.now()));
        queue.add(new Event(6L, "PURCHASE", 10L, "phones", LocalDateTime.now()));

        Map<String, Long> overallSummary = queue.getSummary(null);
        assertEquals(3L, overallSummary.get("VIEW"));
        assertEquals(2L, overallSummary.get("ADD_TO_CART"));
        assertEquals(1L, overallSummary.get("PURCHASE"));

        // Filtered by category "phones"
        Map<String, Long> phonesSummary = queue.getSummary("phones");
        assertEquals(2L, phonesSummary.get("VIEW"));
        assertEquals(1L, phonesSummary.get("ADD_TO_CART"));
        assertEquals(1L, phonesSummary.get("PURCHASE"));

        // Filtered by non-existent category
        Map<String, Long> emptySummary = queue.getSummary("clothing");
        assertEquals(0L, emptySummary.get("VIEW"));
        assertEquals(0L, emptySummary.get("ADD_TO_CART"));
        assertEquals(0L, emptySummary.get("PURCHASE"));
    }
}
