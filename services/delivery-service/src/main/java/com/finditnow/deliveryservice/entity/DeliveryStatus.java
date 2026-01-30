package com.finditnow.deliveryservice.entity;

public enum DeliveryStatus {
    CREATED,     // delivery record exists
    ASSIGNED,    // agent assigned
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED,
    FAILED
}

