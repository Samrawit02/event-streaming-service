package com.challenge.eventstream.controller;

import com.challenge.eventstream.dto.EventRequest;
import com.challenge.eventstream.dto.EventResponse;
import com.challenge.eventstream.dto.EventSearchCriteria;
import com.challenge.eventstream.service.EventService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public EventController(EventService eventService, ObjectMapper objectMapper, Validator validator) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * POST /events
     * Supports both a single event object and a batch list of events.
     * Header: Authorization: Bearer <token>
     */
    @PostMapping
    public ResponseEntity<?> createEvents(@RequestBody JsonNode rawPayload) {
        if (rawPayload == null || rawPayload.isNull() || rawPayload.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request body cannot be empty"));
        }

        if (rawPayload.isArray()) {
            List<EventRequest> requests = objectMapper.convertValue(
                    rawPayload,
                    new TypeReference<List<EventRequest>>() {}
            );

            for (int i = 0; i < requests.size(); i++) {
                validateEventRequest(requests.get(i), "Item [" + i + "]");
            }

            List<EventResponse> created = eventService.createEventsBatch(requests);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } else {
            EventRequest request = objectMapper.convertValue(rawPayload, EventRequest.class);
            validateEventRequest(request, "Payload");
            EventResponse created = eventService.createEvent(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        }
    }

    /**
     * GET /events
     * Queryparams (all optional): userId, eventType, category, productId, from, to
     * Header: Authorization: Bearer <token>
     * Output: List of matching events
     * Function:
     * - Check LRU in-memory cache first
     * - If not cached, query PostgreSQL
     * - Cache the result for future calls
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> searchEvents(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        EventSearchCriteria criteria = new EventSearchCriteria(userId, eventType, category, productId, from, to);

        boolean isCached = eventService.getLruCache().containsKey(criteria);
        List<EventResponse> results = eventService.searchEvents(criteria);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Cache", isCached ? "HIT" : "MISS");

        return new ResponseEntity<>(results, headers, HttpStatus.OK);
    }

    /**
     * GET /events/recent
     * Header: Authorization: Bearer <token>
     * Output: Latest 20 events from in-memory queue
     * Function: Quickly fetch recent activity without hitting DB
     */
    @GetMapping("/recent")
    public ResponseEntity<List<EventResponse>> getRecentEvents() {
        List<EventResponse> recentEvents = eventService.getRecentEvents();
        return ResponseEntity.ok(recentEvents);
    }

    /**
     * GET /events/summary
     * Query params: category (optional)
     * Header: Authorization: Bearer <token>
     * Output: Count of events grouped by eventType
     * {
     *   "VIEW": 150,
     *   "ADD_TO_CART": 35,
     *   "PURCHASE": 10
     * }
     * Function: Summarize last 500 events from in-memory queue.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getSummary(
            @RequestParam(required = false) String category
    ) {
        Map<String, Long> summary = eventService.getSummary(category);
        return ResponseEntity.ok(summary);
    }

    private void validateEventRequest(EventRequest request, String context) {
        Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder(context + " validation failed: ");
            for (ConstraintViolation<EventRequest> v : violations) {
                sb.append(v.getPropertyPath()).append(" ").append(v.getMessage()).append("; ");
            }
            throw new ValidationException(sb.toString());
        }
    }
}
