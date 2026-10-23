package com.challenge.eventstream.dto;

import com.challenge.eventstream.model.Event;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class EventResponse {

    private Long id;
    private Long userId;
    private String eventType;
    private Long productId;
    private String category;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public EventResponse() {
    }

    public EventResponse(Long id, Long userId, String eventType, Long productId, String category, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.eventType = eventType;
        this.productId = productId;
        this.category = category;
        this.timestamp = timestamp;
    }

    public static EventResponse fromEntity(Event event) {
        if (event == null) {
            return null;
        }
        return new EventResponse(
                event.getId(),
                event.getUserId(),
                event.getEventType(),
                event.getProductId(),
                event.getCategory(),
                event.getTimestamp()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
