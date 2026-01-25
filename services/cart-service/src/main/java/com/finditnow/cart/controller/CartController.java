package com.finditnow.cart.controller;

import com.finditnow.cart.dto.AddToCartRequest;
import com.finditnow.cart.dto.CartResponse;
import com.finditnow.cart.dto.UpdateCartItemRequest;
import com.finditnow.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

//cart controller
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addItemToCart(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Shop-Id") Long shopId,
            @Valid @RequestBody AddToCartRequest request) {
        CartResponse response = cartService.addItemToCart(userId, shopId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/item/{itemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse response = cartService.updateCartItem(itemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<Void> removeCartItem(@PathVariable UUID itemId) {
        cartService.removeCartItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}/shop/{shopId}")
    public ResponseEntity<CartResponse> getCartByUserAndShop(
            @PathVariable UUID userId,
            @PathVariable Long shopId) {
        CartResponse response = cartService.getCartByUserAndShop(userId, shopId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{cartId}/clear")
    public ResponseEntity<Void> clearCart(@PathVariable UUID cartId) {
        cartService.clearCart(cartId);
        return ResponseEntity.noContent().build();
    }
}

