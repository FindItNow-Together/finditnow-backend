package com.finditnow.orderservice;

import com.finditnow.orderservice.dtos.CartDTO;
import com.finditnow.orderservice.dtos.CartItemDTO;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Static test data for Cart operations during development.
 * This matches the frontend test cart data for consistent testing.
 */
public final class TestCartData {

    private TestCartData() {
        // Prevent instantiation
    }

    public static final UUID TEST_CART_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    public static final UUID TEST_USER_ID = UUID.fromString("0cadc2b2-2ae9-4159-8ddc-e3f5978b25cd");
    public static final Long TEST_SHOP_ID = 42L;

    // Cart Items
    private static final CartItemDTO ITEM_1 = createCartItem(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
            101L,
            "Organic Whole Wheat Flour (5kg)",
            285.00,
            2
    );

    private static final CartItemDTO ITEM_2 = createCartItem(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440002"),
            102L,
            "Fresh Farm Eggs (30 count)",
            180.00,
            1
    );

    private static final CartItemDTO ITEM_3 = createCartItem(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440003"),
            103L,
            "Premium Basmati Rice (10kg)",
            950.00,
            1
    );

    private static final CartItemDTO ITEM_4 = createCartItem(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440004"),
            104L,
            "Extra Virgin Olive Oil (1L)",
            650.00,
            2
    );

    private static final CartItemDTO ITEM_5 = createCartItem(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440005"),
            105L,
            "Organic Honey (500g)",
            425.00,
            1
    );

    // Main Test Cart
    public static final CartDTO TEST_CART = createCart(
            TEST_CART_ID,
            Arrays.asList(ITEM_1, ITEM_2, ITEM_3, ITEM_4, ITEM_5)
    );

    // Empty Cart for testing
    public static final CartDTO EMPTY_CART = createCart(
            UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae8"),
            List.of()
    );

    // Single Item Cart for testing
    public static final CartDTO SINGLE_ITEM_CART = createCart(
            UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae9"),
            List.of(
                    createCartItem(
                            UUID.fromString("550e8400-e29b-41d4-a716-446655440006"),
                            106L,
                            "Dark Chocolate Bar (100g)",
                            150.00,
                            1
                    )
            )
    );

    /**
     * Helper method to create CartItemDTO
     */
    private static CartItemDTO createCartItem(UUID id, Long productId, String productName,
                                              Double price, Integer quantity) {
        CartItemDTO item = new CartItemDTO();
        item.setId(id);
        item.setProductId(productId);
        item.setProductName(productName);
        item.setPrice(price);
        item.setQuantity(quantity);
        return item;
    }

    /**
     * Helper method to create CartDTO
     */
    private static CartDTO createCart(UUID id, List<CartItemDTO> items) {
        CartDTO cart = new CartDTO();
        cart.setId(id);
        cart.setUserId(TestCartData.TEST_USER_ID);
        cart.setShopId(TestCartData.TEST_SHOP_ID);
        cart.setItems(items);
        return cart;
    }

    /**
     * Get test cart by ID (useful for mocking fetchCart)
     */
    public static CartDTO getCartById(UUID cartId) {
        if (TEST_CART_ID.equals(cartId)) {
            return TEST_CART;
        } else if (EMPTY_CART.getId().equals(cartId)) {
            return EMPTY_CART;
        } else if (SINGLE_ITEM_CART.getId().equals(cartId)) {
            return SINGLE_ITEM_CART;
        }
        return null;
    }

    /**
     * Get test cart by user ID (useful for mocking user's active cart)
     */
    public static CartDTO getCartByUserId(UUID userId) {
        if (TEST_USER_ID.equals(userId)) {
            return TEST_CART;
        }
        return null;
    }
}