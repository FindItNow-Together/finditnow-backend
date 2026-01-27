package com.finditnow.orderservice.dtos;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CartDTO {
    private UUID id;
    private UUID userId;
    private Long shopId;
    private List<CartItemDTO> items;
}
