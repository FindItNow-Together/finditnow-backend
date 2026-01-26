package com.finditnow.orderservice.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class CartItemDTO {
    private UUID id;
    private Long productId;
    private String productName;
    private Double price;
    private Integer quantity;
}
