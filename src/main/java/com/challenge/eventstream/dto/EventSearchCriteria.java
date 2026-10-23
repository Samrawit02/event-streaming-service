package com.challenge.eventstream.dto;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.Objects;

public class EventSearchCriteria {

    private Long userId;
    private String eventType;
    private String category;
    private Long productId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;

    public EventSearchCriteria() {
    }

    public EventSearchCriteria(Long userId, String eventType, String category, Long productId, LocalDateTime from, LocalDateTime to) {
        this.userId = userId;
        this.eventType = eventType != null ? eventType.trim().toUpperCase().replace(" ", "_") : null;
        this.category = category != null ? category.trim() : null;
        this.productId = productId;
        this.from = from;
        this.to = to;
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
        this.eventType = eventType != null ? eventType.trim().toUpperCase().replace(" ", "_") : null;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category != null ? category.trim() : null;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public void setFrom(LocalDateTime from) {
        this.from = from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public void setTo(LocalDateTime to) {
        this.to = to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventSearchCriteria that = (EventSearchCriteria) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(eventType, that.eventType) &&
                Objects.equals(category, that.category) &&
                Objects.equals(productId, that.productId) &&
                Objects.equals(from, that.from) &&
                Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, eventType, category, productId, from, to);
    }

    @Override
    public String toString() {
        return "EventSearchCriteria{" +
                "userId=" + userId +
                ", eventType='" + eventType + '\'' +
                ", category='" + category + '\'' +
                ", productId=" + productId +
                ", from=" + from +
                ", to=" + to +
                '}';
    }
}
