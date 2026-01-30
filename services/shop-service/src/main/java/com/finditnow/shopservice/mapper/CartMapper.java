package com.finditnow.shopservice.mapper;

import com.finditnow.shopservice.dto.CartItemResponse;
import com.finditnow.shopservice.dto.CartResponse;
import com.finditnow.shopservice.entity.Cart;
import com.finditnow.shopservice.entity.CartItem;
import com.finditnow.shopservice.entity.Product;
import com.finditnow.shopservice.entity.ShopInventory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    /**
     * Convert Cart entity to CartResponse DTO with all product details and subtotal
     */
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

        // Calculate cart subtotal from all item subtotals
        float subtotal = itemResponses.stream()
                .map(CartItemResponse::getItemSubtotal)
                .reduce(0.0f, Float::sum);

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .shopId(cart.getShopId())
                .status(cart.getStatus())
                .items(itemResponses)
                .totalItems(totalItems)
                .subtotal(subtotal)
                .build();
    }

    /**
     * Convert CartItem entity to CartItemResponse DTO with product details
     * Extracts product information from the ShopInventory relationship
     */
    public CartItemResponse toCartItemResponse(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }

        // Access the inventory and product through the relationship
        ShopInventory inventory = cartItem.getShopInventory();
        Product product = inventory.getProduct();

        // Get price from inventory
        float price = inventory.getPrice();

        // Calculate item subtotal
        float itemSubtotal = price * cartItem.getQuantity();

        return CartItemResponse.builder()
                .itemId(cartItem.getId())
                .inventoryId(inventory.getId())
                .quantity(cartItem.getQuantity())
                .addedAt(cartItem.getAddedAt())
                // Product details
                .productId(product.getId())
                .productName(product.getName())
                .price(price)
                .itemSubtotal(itemSubtotal)
                .build();
    }
}
