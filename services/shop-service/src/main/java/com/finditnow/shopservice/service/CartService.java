package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.AddToCartRequest;
import com.finditnow.shopservice.dto.CartPricingResponse;
import com.finditnow.shopservice.dto.CartResponse;
import com.finditnow.shopservice.dto.UpdateCartItemRequest;

import java.util.UUID;

public interface CartService {

    /**
     * Get or create an active cart for a user and shop
     */
    CartResponse getOrCreateActiveCart(UUID userId, Long shopId);

    /**
     * Add an item to the cart
     */
    CartResponse addItemToCart(UUID userId, Long shopId, AddToCartRequest request);

    /**
     * Update cart item quantity with user ownership validation
     * @param userId The authenticated user ID
     * @param cartItemId The cart item ID to update
     * @param request The update request
     * @return Updated cart response
     */
    CartResponse updateCartItem(UUID userId, UUID cartItemId, UpdateCartItemRequest request);

    /**
     * Remove an item from the cart with user ownership validation
     * @param userId The authenticated user ID
     * @param cartItemId The cart item ID to remove
     */
    void removeCartItem(UUID userId, UUID cartItemId);

    /**
     * Get a user's cart for a specific shop
     */
    CartResponse getCartByUserAndShop(UUID userId, Long shopId);

    /**
     * Get a user's cart(my cart)
     */
    CartResponse getUserCart(UUID userId);

    /**
     * Clear all items from a cart with user ownership validation
     * @param userId The authenticated user ID
     * @param cartId The cart ID to clear
     */
    void clearCart(UUID userId, UUID cartId);

    /**
     * Consume all items from a cart after order for internal use(inter service calling)
     * @param cartId The cart ID to clear
     */
    void consumeCart(UUID cartId);

    /**
     * Mark a cart as converted (to order)
     */
    void markCartAsConverted(UUID cartId);

    CartPricingResponse calculatePricing(UUID userId, UUID cartId);

    CartResponse getCartById(UUID cartId);
}
