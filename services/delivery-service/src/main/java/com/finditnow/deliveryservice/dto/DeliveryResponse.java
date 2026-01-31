package com.finditnow.deliveryservice.dto;

import com.finditnow.deliveryservice.entity.DeliveryStatus;
import com.finditnow.deliveryservice.entity.DeliveryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryResponse {
    private UUID id;
    private UUID orderId;
    private Long shopId;
    private UUID customerId;
    private UUID assignedAgentId;
    private DeliveryStatus status;
    private DeliveryType type;
    private String pickupAddress;
    private String deliveryAddress;
    private String instructions;
    private Double deliveryCharge;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}