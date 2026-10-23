package com.challenge.eventstream.service;

import com.challenge.eventstream.cache.LruCache;
import com.challenge.eventstream.dto.EventRequest;
import com.challenge.eventstream.dto.EventResponse;
import com.challenge.eventstream.dto.EventSearchCriteria;
import com.challenge.eventstream.model.Event;
import com.challenge.eventstream.model.EventType;
import com.challenge.eventstream.queue.InMemoryEventQueue;
import com.challenge.eventstream.repository.EventRepository;
import com.challenge.eventstream.repository.EventSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final InMemoryEventQueue eventQueue;
    private final LruCache<EventSearchCriteria, List<EventResponse>> lruCache;

    public EventService(EventRepository eventRepository,
                        InMemoryEventQueue eventQueue,
                        @Value("${app.cache.lru-capacity:100}") int lruCapacity) {
        this.eventRepository = eventRepository;
        this.eventQueue = eventQueue;
        this.lruCache = new LruCache<>(lruCapacity);
    }

    /**
     * Ingests a single event:
     * 1. Validates and converts to Entity
     * 2. Saves to PostgreSQL
     * 3. Adds to in-memory queue (maintaining max 500)
     * 4. Invalidates LRU query cache
     */
    @Transactional
    public EventResponse createEvent(EventRequest request) {
        validateEventRequest(request);

        Event entity = toEntity(request);
        Event saved = eventRepository.save(entity);

        eventQueue.add(saved);
        lruCache.clear(); // Invalidate cache on new event insertion

        logger.info("Ingested event id={}, type={}, userId={}", saved.getId(), saved.getEventType(), saved.getUserId());
        return EventResponse.fromEntity(saved);
    }

    /**
     * Ingests a batch of events:
     * 1. Validates each event
     * 2. Saves all to PostgreSQL
     * 3. Adds all to in-memory queue
     * 4. Invalidates LRU query cache
     */
    @Transactional
    public List<EventResponse> createEventsBatch(List<EventRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Batch event list cannot be empty");
        }

        List<Event> entities = new ArrayList<>(requests.size());
        for (EventRequest req : requests) {
            validateEventRequest(req);
            entities.add(toEntity(req));
        }

        List<Event> saved = eventRepository.saveAll(entities);
        eventQueue.addAll(saved);
        lruCache.clear();

        logger.info("Ingested batch of {} events", saved.size());
        return saved.stream()
                .map(EventResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Multi-tiered event search:
     * 1. Check LRU in-memory cache first
     * 2. If not cached, query PostgreSQL via EventSpecifications
     * 3. Store result in LRU cache
     */
    public List<EventResponse> searchEvents(EventSearchCriteria criteria) {
        // Step 1: Check LRU Cache
        Optional<List<EventResponse>> cachedResult = lruCache.get(criteria);
        if (cachedResult.isPresent()) {
            logger.debug("LRU Cache HIT for criteria: {}", criteria);
            return cachedResult.get();
        }

        logger.debug("LRU Cache MISS for criteria: {}. Querying database.", criteria);

        // Step 2: Query PostgreSQL database with dynamic criteria
        List<Event> events = eventRepository.findAll(EventSpecifications.withCriteria(criteria));

        List<EventResponse> responses = events.stream()
                .map(EventResponse::fromEntity)
                .collect(Collectors.toList());

        // Step 3: Cache result in LRU Cache
        lruCache.put(criteria, responses);

        return responses;
    }

    /**
     * Fetches the latest 20 events directly from the in-memory queue without hitting PostgreSQL.
     */
    public List<EventResponse> getRecentEvents() {
        return eventQueue.getRecent(20);
    }

    /**
     * Summarizes the last 500 events from the in-memory queue grouped by eventType.
     * Optionally filtered by category.
     */
    public Map<String, Long> getSummary(String category) {
        return eventQueue.getSummary(category);
    }

    public LruCache<EventSearchCriteria, List<EventResponse>> getLruCache() {
        return lruCache;
    }

    public InMemoryEventQueue getEventQueue() {
        return eventQueue;
    }

    private void validateEventRequest(EventRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Event request cannot be null");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getEventType() == null || request.getEventType().isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        // Validate enum conformity
        EventType.fromString(request.getEventType());
    }

    private Event toEntity(EventRequest request) {
        EventType eventType = EventType.fromString(request.getEventType());
        LocalDateTime ts = request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now();

        return new Event(
                request.getUserId(),
                eventType.name(),
                request.getProductId(),
                request.getCategory() != null ? request.getCategory().trim() : null,
                ts
        );
    }
}
