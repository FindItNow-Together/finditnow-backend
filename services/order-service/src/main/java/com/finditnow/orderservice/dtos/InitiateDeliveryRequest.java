package com.finditnow.orderservice.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class InitiateDeliveryRequest {
    private UUID orderId;
    private Long shopId;
    private UUID customerId;
    private String type; // Using String to avoid direct dependency on Delivery Service Enum
    private String pickupAddress;
    private String deliveryAddress;
    private String instructions;
    private Double amount;
}
