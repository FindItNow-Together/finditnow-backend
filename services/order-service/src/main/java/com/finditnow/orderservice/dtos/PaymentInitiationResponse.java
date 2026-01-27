package com.finditnow.orderservice.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentInitiationResponse {
    private String razorpayKey;
    private Integer amount;
    private String razorpayOrderId;
    private UUID paymentId;
}
