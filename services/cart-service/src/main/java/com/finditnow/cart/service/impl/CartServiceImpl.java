package com.finditnow.cart.service.impl;

import com.finditnow.cart.dto.AddToCartRequest;
import com.finditnow.cart.dto.CartResponse;
import com.finditnow.cart.dto.UpdateCartItemRequest;
import com.finditnow.cart.entity.Cart;
import com.finditnow.cart.entity.CartItem;
import com.finditnow.cart.entity.CartStatus;
import com.finditnow.cart.exception.CartItemNotFoundException;
import com.finditnow.cart.exception.CartNotFoundException;
import com.finditnow.cart.mapper.CartMapper;
import com.finditnow.cart.repository.CartItemRepository;
import com.finditnow.cart.repository.CartRepository;
import com.finditnow.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getOrCreateActiveCart(UUID userId, Long shopId) {
        Cart cart = cartRepository
                .findByUserIdAndShopIdAndStatus(userId, shopId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .shopId(shopId)
                            .status(CartStatus.ACTIVE)
                            .build();
                    return cartRepository.save(newCart);
                });

        return cartMapper.toCartResponse(cart);
    }

    @Override
    public CartResponse addItemToCart(UUID userId, Long shopId, AddToCartRequest request) {
        Cart cart = cartRepository
                .findByUserIdAndShopIdAndStatus(userId, shopId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .shopId(shopId)
                            .status(CartStatus.ACTIVE)
                            .build();
                    return cartRepository.save(newCart);
                });

        CartItem existingItem = cartItemRepository
                .findByCartIdAndInventoryId(cart.getId(), request.getInventoryId())
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .inventoryId(request.getInventoryId())
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toCartResponse(savedCart);
    }

    @Override
    public CartResponse updateCartItem(UUID cartItemId, UpdateCartItemRequest request) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(
                        "Cart item not found with id: " + cartItemId));

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        Cart cart = cartItem.getCart();
        return cartMapper.toCartResponse(cart);
    }

    @Override
    public void removeCartItem(UUID cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(
                        "Cart item not found with id: " + cartItemId));

        cartItemRepository.delete(cartItem);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartByUserAndShop(UUID userId, Long shopId) {
        Cart cart = cartRepository
                .findByUserIdAndShopIdAndStatus(userId, shopId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException(
                        "Active cart not found for user: " + userId + " and shop: " + shopId));

        return cartMapper.toCartResponse(cart);
    }

    @Override
    public void clearCart(UUID cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(
                        "Cart not found with id: " + cartId));

        cart.getItems().clear();
        cartItemRepository.deleteAllByCartId(cartId);
        cartRepository.save(cart);
    }

    @Override
    public void markCartAsConverted(UUID cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(
                        "Cart not found with id: " + cartId));

        cart.setStatus(CartStatus.CONVERTED);
        cartRepository.save(cart);
    }
}

