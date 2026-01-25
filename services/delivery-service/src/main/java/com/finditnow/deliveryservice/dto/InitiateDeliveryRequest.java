package com.finditnow.deliveryservice.dto;

import com.finditnow.deliveryservice.entity.DeliveryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateDeliveryRequest {
    private UUID orderId;
    private Long shopId;
    private UUID customerId;
    private DeliveryType type;
    private String pickupAddress;
    private String deliveryAddress;
    private String instructions;
    private Double amount;
}
