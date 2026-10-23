package com.challenge.eventstream.queue;

import com.challenge.eventstream.dto.EventResponse;
import com.challenge.eventstream.dto.EventSearchCriteria;
import com.challenge.eventstream.model.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory bounded queue holding the latest events (max 500).
 * Uses ConcurrentLinkedDeque to append newer events to the tail and evict older events from the head.
 */
@Component
public class InMemoryEventQueue {

    private final int maxCapacity;
    private final ConcurrentLinkedDeque<Event> queue = new ConcurrentLinkedDeque<>();

    public InMemoryEventQueue(@Value("${app.queue.max-capacity:500}") int maxCapacity) {
        this.maxCapacity = maxCapacity > 0 ? maxCapacity : 500;
    }

    /**
     * Adds an event to the queue and evicts the oldest entry if capacity is exceeded.
     */
    public void add(Event event) {
        if (event == null) {
            return;
        }
        queue.addLast(event);
        trimToCapacity();
    }

    /**
     * Adds a collection of events to the queue and evicts oldest entries if capacity is exceeded.
     */
    public void addAll(Collection<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (Event event : events) {
            if (event != null) {
                queue.addLast(event);
            }
        }
        trimToCapacity();
    }

    private void trimToCapacity() {
        while (queue.size() > maxCapacity) {
            queue.pollFirst();
        }
    }

    /**
     * Returns the latest N events in descending order (most recent first).
     *
     * @param count Number of recent events to return
     * @return List of EventResponse
     */
    public List<EventResponse> getRecent(int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        // ConcurrentLinkedDeque iterator in descending order gives newest to oldest
        List<EventResponse> result = new ArrayList<>(Math.min(count, queue.size()));
        Iterator<Event> descendingIterator = queue.descendingIterator();
        while (descendingIterator.hasNext() && result.size() < count) {
            result.add(EventResponse.fromEntity(descendingIterator.next()));
        }
        return result;
    }

    /**
     * Returns all events currently in the queue in descending order (newest first).
     */
    public List<EventResponse> getAllRecent() {
        return getRecent(queue.size());
    }

    /**
     * Filters events currently in the in-memory queue matching the specified criteria.
     */
    public List<EventResponse> filter(EventSearchCriteria criteria) {
        if (criteria == null) {
            return getAllRecent();
        }

        List<EventResponse> matches = new ArrayList<>();
        Iterator<Event> descendingIterator = queue.descendingIterator();

        while (descendingIterator.hasNext()) {
            Event event = descendingIterator.next();
            if (matchesCriteria(event, criteria)) {
                matches.add(EventResponse.fromEntity(event));
            }
        }

        return matches;
    }

    /**
     * Summarizes the events currently in the queue, grouped by eventType.
     * Optionally filters by category if provided.
     * Guaranteed to return keys for VIEW, ADD_TO_CART, and PURCHASE with count (default 0).
     */
    public Map<String, Long> getSummary(String category) {
        // Initialize standard event types with count 0
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("VIEW", 0L);
        summary.put("ADD_TO_CART", 0L);
        summary.put("PURCHASE", 0L);

        String targetCategory = category != null && !category.isBlank() ? category.trim().toLowerCase() : null;

        for (Event event : queue) {
            if (targetCategory != null) {
                if (event.getCategory() == null || !event.getCategory().trim().equalsIgnoreCase(targetCategory)) {
                    continue;
                }
            }

            String eventType = event.getEventType();
            if (eventType != null) {
                String normalized = eventType.trim().toUpperCase().replace(" ", "_");
                summary.put(normalized, summary.getOrDefault(normalized, 0L) + 1L);
            }
        }

        return summary;
    }

    private boolean matchesCriteria(Event event, EventSearchCriteria criteria) {
        if (criteria.getUserId() != null && !criteria.getUserId().equals(event.getUserId())) {
            return false;
        }

        if (criteria.getEventType() != null && !criteria.getEventType().isBlank()) {
            String normCriteria = criteria.getEventType().trim().toUpperCase().replace(" ", "_");
            String normEvent = event.getEventType() != null ? event.getEventType().trim().toUpperCase().replace(" ", "_") : "";
            if (!normCriteria.equals(normEvent)) {
                return false;
            }
        }

        if (criteria.getCategory() != null && !criteria.getCategory().isBlank()) {
            if (event.getCategory() == null || !event.getCategory().trim().equalsIgnoreCase(criteria.getCategory().trim())) {
                return false;
            }
        }

        if (criteria.getProductId() != null && !criteria.getProductId().equals(event.getProductId())) {
            return false;
        }

        if (criteria.getFrom() != null && (event.getTimestamp() == null || event.getTimestamp().isBefore(criteria.getFrom()))) {
            return false;
        }

        if (criteria.getTo() != null && (event.getTimestamp() == null || event.getTimestamp().isAfter(criteria.getTo()))) {
            return false;
        }

        return true;
    }

    public int size() {
        return queue.size();
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void clear() {
        queue.clear();
    }
}
