package com.finditnow.shopservice.service.impl;

import com.finditnow.shopservice.dto.AddToCartRequest;
import com.finditnow.shopservice.dto.CartResponse;
import com.finditnow.shopservice.dto.UpdateCartItemRequest;
import com.finditnow.shopservice.entity.Cart;
import com.finditnow.shopservice.entity.CartItem;
import com.finditnow.shopservice.entity.CartStatus;
import com.finditnow.shopservice.entity.ShopInventory;
import com.finditnow.shopservice.exception.BadRequestException;
import com.finditnow.shopservice.exception.CartItemNotFoundException;
import com.finditnow.shopservice.exception.CartNotFoundException;
import com.finditnow.shopservice.exception.NotFoundException;
import com.finditnow.shopservice.mapper.CartMapper;
import com.finditnow.shopservice.repository.CartItemRepository;
import com.finditnow.shopservice.repository.CartRepository;
import com.finditnow.shopservice.repository.ShopInventoryRepository;
import com.finditnow.shopservice.service.CartService;
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
    private final ShopInventoryRepository shopInventoryRepository;
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

        ShopInventory inventory = shopInventoryRepository.findById(request.getInventoryId())
                .orElseThrow(() -> new NotFoundException("Inventory not found with id: " + request.getInventoryId()));

        if (inventory.getShop().getId() != shopId) {
            throw new BadRequestException("Inventory does not belong to the specified shop");
        }

        CartItem existingItem = cartItemRepository
                .findByCartIdAndShopInventoryId(cart.getId(), inventory.getId())
                .orElse(null);

        int newQuantity = request.getQuantity();
        if (existingItem != null) {
            newQuantity += existingItem.getQuantity();
        }

        validateStockAvailability(inventory, newQuantity, existingItem != null ? existingItem.getQuantity() : 0);

        // Update reserved stock
        int quantityToAdd = request.getQuantity();
        inventory.setReservedStock(inventory.getReservedStock() + quantityToAdd);
        shopInventoryRepository.save(inventory);

        if (existingItem != null) {
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .shopInventory(inventory)
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

        ShopInventory inventory = cartItem.getShopInventory();
        int oldQuantity = cartItem.getQuantity();
        int newQuantity = request.getQuantity();

        validateStockAvailability(inventory, newQuantity, oldQuantity);

        // Update reserved stock
        int diff = newQuantity - oldQuantity;
        inventory.setReservedStock(inventory.getReservedStock() + diff);
        shopInventoryRepository.save(inventory);

        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);

        Cart cart = cartItem.getCart();
        return cartMapper.toCartResponse(cart);
    }

    @Override
    public void removeCartItem(UUID cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(
                        "Cart item not found with id: " + cartItemId));

        ShopInventory inventory = cartItem.getShopInventory();

        // Release reserved stock
        inventory.setReservedStock(inventory.getReservedStock() - cartItem.getQuantity());
        // Simple safety check to prevent negative reserved stock if manual DB edits
        // happened
        if (inventory.getReservedStock() < 0) {
            inventory.setReservedStock(0);
        }
        shopInventoryRepository.save(inventory);

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

        // Release reserved stock for all items
        for (CartItem item : cart.getItems()) {
            ShopInventory inventory = item.getShopInventory();
            inventory.setReservedStock(inventory.getReservedStock() - item.getQuantity());
            if (inventory.getReservedStock() < 0) {
                inventory.setReservedStock(0);
            }
            shopInventoryRepository.save(inventory);
        }

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
        // Note: We do NOT decrease reserved stock here because the order process
        // will likely handle the inventory deduction (converting reserved to sold).
        // If order service is separate, this logic might need coordination,
        // but typically "Converted" means it's becoming an order.

        cartRepository.save(cart);
    }

    private void validateStockAvailability(ShopInventory inventory, int recipientQuantity, int currentQuantity) {
        // available = (stock - reserved) + currentQuantityInCart(which is already in
        // reserved)
        // effectively: remaining_free = stock - reserved
        // we need: request_diff <= remaining_free

        int diff = recipientQuantity - currentQuantity;
        if (diff > 0) {
            int availableStock = inventory.getStock() - inventory.getReservedStock();
            if (diff > availableStock) {
                throw new BadRequestException("Insufficient stock. Available: " + availableStock);
            }
        }
    }
}
