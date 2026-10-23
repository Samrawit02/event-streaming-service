package com.challenge.eventstream.model;

public enum EventType {
    VIEW,
    ADD_TO_CART,
    PURCHASE;

    public static EventType fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace(" ", "_");
        for (EventType type : EventType.values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid event type: " + value + ". Allowed values: VIEW, ADD_TO_CART, PURCHASE");
    }
}
