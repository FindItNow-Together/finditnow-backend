package com.finditnow.shopservice.mapper;

import com.finditnow.shopservice.dto.CartItemResponse;
import com.finditnow.shopservice.dto.CartResponse;
import com.finditnow.shopservice.entity.Cart;
import com.finditnow.shopservice.entity.CartItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartResponse toCartResponse(Cart cart) {
        if (cart == null) {
            return null;
        }

        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());

        int totalItems = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .shopId(cart.getShopId())
                .status(cart.getStatus())
                .items(itemResponses)
                .totalItems(totalItems)
                .build();
    }

    public CartItemResponse toCartItemResponse(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }

        return CartItemResponse.builder()
                .itemId(cartItem.getId())
                .inventoryId(cartItem.getShopInventory().getId())
                .quantity(cartItem.getQuantity())
                .addedAt(cartItem.getAddedAt())
                .build();
    }
}
