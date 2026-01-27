package com.finditnow.orderservice.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class OrderItemResponse {
    private UUID id;
    private Long productId;
    private String productName;
    private Double priceAtOrder;
    private Integer quantity;
}
