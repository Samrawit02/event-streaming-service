package com.challenge.eventstream.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

public class EventRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "eventType is required")
    @Pattern(regexp = "(?i)^(VIEW|ADD_TO_CART|ADD TO CART|PURCHASE)$", 
             message = "eventType must be one of: VIEW, ADD_TO_CART, PURCHASE")
    private String eventType;

    private Long productId;

    private String category;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public EventRequest() {
    }

    public EventRequest(Long userId, String eventType, Long productId, String category, LocalDateTime timestamp) {
        this.userId = userId;
        this.eventType = eventType;
        this.productId = productId;
        this.category = category;
        this.timestamp = timestamp;
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
