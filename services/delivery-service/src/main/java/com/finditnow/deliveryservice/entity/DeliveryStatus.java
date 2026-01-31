package com.finditnow.deliveryservice.entity;

public enum DeliveryStatus {
    CREATED, // delivery record exists
    ASSIGNED, // agent assigned, waiting for acceptance
    PENDING_ACCEPTANCE, // agent must explicitly accept before proceeding
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED,
    FAILED,
    CANCELLED_BY_AGENT,
    UNASSIGNED
}
