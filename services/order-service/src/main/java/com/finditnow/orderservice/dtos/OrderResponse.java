package com.finditnow.orderservice.dtos;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class OrderResponse {
    private UUID id;
    private UUID userId;
    private Long shopId;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private Double totalAmount;
    private UUID deliveryAddressId;
    private String createdAt;
    private List<OrderItemResponse> items;
}
