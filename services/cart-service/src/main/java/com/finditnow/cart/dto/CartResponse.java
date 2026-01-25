package com.finditnow.cart.dto;

import com.finditnow.cart.entity.CartStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private UUID cartId;
    private UUID userId;
    private Long shopId;
    private CartStatus status;
    private List<CartItemResponse> items;
    private Integer totalItems;
}

