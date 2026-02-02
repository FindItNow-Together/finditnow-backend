package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.AddToCartRequest;
import com.finditnow.shopservice.dto.CartResponse;
import com.finditnow.shopservice.entity.*;
import com.finditnow.shopservice.exception.BadRequestException;
import com.finditnow.shopservice.mapper.CartMapper;
import com.finditnow.shopservice.repository.CartItemRepository;
import com.finditnow.shopservice.repository.CartRepository;
import com.finditnow.shopservice.repository.ShopInventoryRepository;
import com.finditnow.shopservice.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

        @Mock
        private CartRepository cartRepository;

        @Mock
        private CartItemRepository cartItemRepository;

        @Mock
        private ShopInventoryRepository shopInventoryRepository;

        @Mock
        private CartMapper cartMapper;

        @InjectMocks
        private CartServiceImpl cartService;

        private UUID userId;
        private Long shopId;
        private Long inventoryId;
        private Cart cart;
        private ShopInventory inventory;
        private Shop shop;

        @BeforeEach
        void setUp() {
                userId = UUID.randomUUID();
                shopId = 1L;
                inventoryId = 10L;

                shop = new Shop();
                shop.setId(shopId);

                cart = Cart.builder()
                                .id(UUID.randomUUID())
                                .userId(userId)
                                .shopId(shopId)
                                .status(CartStatus.ACTIVE)
                                .items(new ArrayList<>())
                                .build();

                inventory = new ShopInventory();
                inventory.setId(inventoryId);
                inventory.setShop(shop);
                inventory.setStock(10);
                inventory.setReservedStock(2); // 8 available
        }

        @Test
        void addItemToCart_Success() {
                AddToCartRequest request = new AddToCartRequest(inventoryId, shopId, 2);

                when(cartRepository.findByUserIdAndShopIdAndStatus(userId, shopId, CartStatus.ACTIVE))
                                .thenReturn(Optional.of(cart));
                when(shopInventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));
                when(cartItemRepository.findByCartIdAndShopInventoryId(cart.getId(), inventoryId))
                                .thenReturn(Optional.empty());
                when(cartRepository.save(any(Cart.class))).thenReturn(cart);
                when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(new CartResponse());

                cartService.addItemToCart(userId, shopId, request);

                // Verify reserved stock increased
                assertEquals(4, inventory.getReservedStock()); // 2 existing + 2 new
                verify(shopInventoryRepository).save(inventory);
                verify(cartItemRepository).save(any(CartItem.class));
        }

        @Test
        void addItemToCart_InsufficientStock() {
                AddToCartRequest request = new AddToCartRequest(inventoryId, shopId, 9); // Available is 8 (10-2)

                when(cartRepository.findByUserIdAndShopIdAndStatus(userId, shopId, CartStatus.ACTIVE))
                                .thenReturn(Optional.of(cart));
                when(shopInventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));
                when(cartItemRepository.findByCartIdAndShopInventoryId(cart.getId(), inventoryId))
                                .thenReturn(Optional.empty());

                assertThrows(BadRequestException.class, () -> cartService.addItemToCart(userId, shopId, request));

                // Verify reserved stock NOT modified
                assertEquals(2, inventory.getReservedStock());
                verify(shopInventoryRepository, never()).save(inventory);
        }
}
