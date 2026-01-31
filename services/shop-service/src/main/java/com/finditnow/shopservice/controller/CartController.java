package com.finditnow.shopservice.controller;

import com.finditnow.shopservice.dto.AddToCartRequest;
import com.finditnow.shopservice.dto.CartPricingResponse;
import com.finditnow.shopservice.dto.CartResponse;
import com.finditnow.shopservice.dto.UpdateCartItemRequest;
import com.finditnow.shopservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController extends BaseController {

    private final CartService cartService;

    /**
     * Add an item to the cart
     * POST /api/cart/add
     */
    @PostMapping("/add")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartResponse> addItemToCart(
            Authentication authentication,
            @Valid @RequestBody AddToCartRequest request) {
        UUID userId = extractUserId(authentication);
        CartResponse response = cartService.addItemToCart(userId, request.getShopId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update cart item quantity
     * PUT /api/cart/item/{itemId}
     * <p>
     * SECURITY FIX: Now validates that the cart item belongs to the authenticated user
     */
    @PutMapping("/item/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartResponse> updateCartItem(
            Authentication authentication,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        UUID userId = extractUserId(authentication);
        CartResponse response = cartService.updateCartItem(userId, itemId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove an item from the cart
     * DELETE /api/cart/item/{itemId}
     * <p>
     * SECURITY FIX: Now validates that the cart item belongs to the authenticated user
     */
    @DeleteMapping("/item/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeCartItem(
            Authentication authentication,
            @PathVariable UUID itemId) {
        UUID userId = extractUserId(authentication);
        cartService.removeCartItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current user's cart for a specific shop
     * GET /api/cart/user/me/shop/{shopId}
     * <p>
     * RECOMMENDED ENDPOINT: Uses authenticated user from token
     */
    @GetMapping("/user/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartResponse> getMyCart(
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        CartResponse response = cartService.getUserCart(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get cart by user ID and shop ID
     * GET /api/cart/user/{userId}/shop/{shopId}
     * <p>
     * LEGACY ENDPOINT: Kept for backward compatibility
     * Note: The userId path variable is ignored and replaced with authenticated user ID for security
     *
     * @deprecated Use /user/me instead
     */
    @Deprecated
    @GetMapping("/user/{userId}/shop/{shopId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartResponse> getCartByUserAndShop(
            Authentication authentication,
            @PathVariable UUID userId,
            @PathVariable Long shopId) {

        // SECURITY: Always use authenticated user ID, ignore path variable
        UUID authUserId = extractUserId(authentication);

        // Log deprecation warning if you have logging configured
        // log.warn("Deprecated endpoint /user/{}/shop/{} called. Use /user/me/shop/{} instead",
        //          userId, shopId, shopId);

        CartResponse response = cartService.getCartByUserAndShop(authUserId, shopId);
        return ResponseEntity.ok(response);
    }

    /**
     * Clear all items from a cart
     * DELETE /api/cart/{cartId}/clear
     * <p>
     * SECURITY FIX: Now validates that the cart belongs to the authenticated user
     */
    @DeleteMapping("/{cartId}/clear")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clearCart(
            Authentication authentication,
            @PathVariable UUID cartId) {
        UUID userId = extractUserId(authentication);
        cartService.clearCart(userId, cartId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clear all items from a cart
     * DELETE /api/cart/{cartId}/consume
     * <p>
     * Internal use only
     */
    @DeleteMapping("/{cartId}/internal/consume")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Void> consumeCart(@PathVariable UUID cartId) {
        cartService.consumeCart(cartId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get cart for other services
     * Currently scoped for internal use only
     */
    @GetMapping("/{cartId}")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<CartResponse> getCart(@PathVariable UUID cartId) {
        return ResponseEntity.ok(cartService.getCartById(cartId));
    }

    /**
     * Get pricing for a cart
     * GET /api/cart/{cartId}/pricing
     */
    @GetMapping("/{cartId}/pricing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CartPricingResponse> getCartPricing(
            Authentication authentication,
            @PathVariable UUID cartId
    ) {
        UUID userId = extractUserId(authentication);
        CartPricingResponse pricing = cartService.calculatePricing(userId, cartId);
        return ResponseEntity.ok(pricing);
    }
}
