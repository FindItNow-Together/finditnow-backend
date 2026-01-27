package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.AddToCartRequest;
import com.finditnow.shopservice.dto.CartResponse;
import com.finditnow.shopservice.dto.UpdateCartItemRequest;

import java.util.UUID;

public interface CartService {

    CartResponse getOrCreateActiveCart(UUID userId, Long shopId);

    CartResponse addItemToCart(UUID userId, Long shopId, AddToCartRequest request);

    CartResponse updateCartItem(UUID cartItemId, UpdateCartItemRequest request);

    void removeCartItem(UUID cartItemId);

    CartResponse getCartByUserAndShop(UUID userId, Long shopId);

    void clearCart(UUID cartId);

    void markCartAsConverted(UUID cartId);
}
