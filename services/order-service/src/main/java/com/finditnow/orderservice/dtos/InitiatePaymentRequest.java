package com.finditnow.orderservice.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class InitiatePaymentRequest {
    private UUID orderId;
}
