package com.finditnow.shopservice.dto;

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
public class CartItemResponse {

    private UUID itemId;
    private Long inventoryId;
    private Integer quantity;
    private LocalDateTime addedAt;

    // Product details for display (NEW FIELDS)
    private Long productId;
    private String productName;
    private Float price;
    private Float itemSubtotal;
}
