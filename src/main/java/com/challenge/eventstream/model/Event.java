package com.challenge.eventstream.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_events_user_id", columnList = "user_id"),
    @Index(name = "idx_events_event_type", columnList = "event_type"),
    @Index(name = "idx_events_category", columnList = "category"),
    @Index(name = "idx_events_product_id", columnList = "product_id"),
    @Index(name = "idx_events_timestamp", columnList = "timestamp")
})
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public Event() {
    }

    public Event(Long userId, String eventType, Long productId, String category, LocalDateTime timestamp) {
        this.userId = userId;
        this.eventType = eventType;
        this.productId = productId;
        this.category = category;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
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

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", userId=" + userId +
                ", eventType='" + eventType + '\'' +
                ", productId=" + productId +
                ", category='" + category + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
