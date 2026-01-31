package com.finditnow.shopservice.service.impl;

import com.finditnow.shopservice.dto.AddToCartRequest;
import com.finditnow.shopservice.dto.CartPricingResponse;
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

        if (!inventory.getShop().getId().equals(shopId)) {
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

        // Use WITH DETAILS query to fetch all relationships for response
        Cart savedCart = cartRepository.findByIdWithDetails(cart.getId())
                .orElseThrow(() -> new CartNotFoundException("Cart not found after save"));

        return cartMapper.toCartResponse(savedCart);
    }

    @Override
    public CartResponse updateCartItem(UUID userId, UUID cartItemId, UpdateCartItemRequest request) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(
                        "Cart item not found with id: " + cartItemId));

        // SECURITY CHECK: Verify the cart item belongs to the authenticated user
        if (!cartItem.getCart().getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to modify this cart item");
        }

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

        // Use WITH DETAILS query to fetch all relationships for response
        Cart cart = cartRepository.findByIdWithDetails(cartItem.getCart().getId())
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        return cartMapper.toCartResponse(cart);
    }

    @Override
    public void removeCartItem(UUID userId, UUID cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(
                        "Cart item not found with id: " + cartItemId));

        // SECURITY CHECK: Verify the cart item belongs to the authenticated user
        if (!cartItem.getCart().getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to remove this cart item");
        }

        ShopInventory inventory = cartItem.getShopInventory();

        // Release reserved stock
        inventory.setReservedStock(inventory.getReservedStock() - cartItem.getQuantity());
        // Simple safety check to prevent negative reserved stock if manual DB edits happened
        if (inventory.getReservedStock() < 0) {
            inventory.setReservedStock(0);
        }
        shopInventoryRepository.save(inventory);

        cartItemRepository.delete(cartItem);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartByUserAndShop(UUID userId, Long shopId) {
        // Use WITH DETAILS query to eagerly fetch all relationships
        // This prevents LazyInitializationException when CartMapper accesses nested entities
        Cart cart = cartRepository
                .findByUserIdAndShopIdAndStatusWithDetails(userId, shopId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException(
                        "Active cart not found for user: " + userId + " and shop: " + shopId));

        return cartMapper.toCartResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getUserCart(UUID userId) {
        // Use WITH DETAILS query to eagerly fetch all relationships
        // This prevents LazyInitializationException when CartMapper accesses nested entities
        Cart cart = cartRepository
                .findByUserIdAndStatusWithDetails(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException(
                        "Active cart not found for user: " + userId));

        return cartMapper.toCartResponse(cart);
    }

    @Override
    public void clearCart(UUID userId, UUID cartId) {
        // Use WITH DETAILS query to fetch relationships
        Cart cart = cartRepository.findByIdWithDetails(cartId)
                .orElseThrow(() -> new CartNotFoundException(
                        "Cart not found with id: " + cartId));

        // SECURITY CHECK: Verify the cart belongs to the authenticated user
        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to clear this cart");
        }

        // Release reserved stock for all items
        releaseCartItemsStock(cartId, cart);
    }

    @Override
    @Transactional
    public void consumeCart(UUID cartId) {
        Cart cart = cartRepository.findByIdWithDetails(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new BadRequestException("Cart is not active");
        }

        for (CartItem item : cart.getItems()) {
            ShopInventory inventory = item.getShopInventory();
            int qty = item.getQuantity();

            inventory.setReservedStock(inventory.getReservedStock() - qty);
            inventory.setStock(inventory.getStock() - qty);

            if (inventory.getStock() < 0) {
                throw new IllegalStateException("Stock underflow for inventory " + inventory.getId());
            }

            shopInventoryRepository.save(inventory);
        }

        cartItemRepository.deleteAllByCartId(cartId);
        cart.getItems().clear();
        cart.setStatus(CartStatus.CONVERTED);

        cartRepository.save(cart);
    }

    private void releaseCartItemsStock(UUID cartId, Cart cart) {
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

        cartRepository.save(cart);
    }

    private void validateStockAvailability(ShopInventory inventory, int requestedQuantity, int currentQuantity) {
        // Calculate how much we're trying to add/change
        int diff = requestedQuantity - currentQuantity;

        if (diff > 0) {
            // We're increasing quantity, check if there's enough available stock
            int availableStock = inventory.getStock() - inventory.getReservedStock();
            if (diff > availableStock) {
                throw new BadRequestException("Insufficient stock. Available: " + availableStock);
            }
        }
        // If diff <= 0, we're decreasing quantity, no stock check needed
    }

    @Override
    public CartPricingResponse calculatePricing(UUID userId, UUID cartId) {
        Cart cart = getCartByIdAndUser(cartId, userId);

        int itemsTotal = cart.getItems().stream()
                .mapToInt(i -> (int)i.getShopInventory().getPrice() * i.getQuantity())
                .sum();

        int tax = calculateTax(itemsTotal);
        int deliveryFee = calculateDeliveryFee(cart);
        int payable = itemsTotal + tax + deliveryFee;

        return new CartPricingResponse(deliveryFee, tax, payable);
    }

    @Override
    public CartResponse getCartById(UUID cartId) {
        Cart cart = cartRepository.findByIdWithDetails(cartId)
                .orElseThrow(() -> new CartNotFoundException(
                        "Cart not found with id: " + cartId));

        return cartMapper.toCartResponse(cart);
    }

    private Cart getCartByIdAndUser(UUID cartId, UUID userId) {
        Cart cart = cartRepository.findByIdWithDetails(cartId)
                .orElseThrow(() -> new CartNotFoundException(
                        "Cart not found with id: " + cartId));

        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to access this cart");
        }

        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new BadRequestException("Cart is not active");
        }

        return cart;
    }

    private int calculateTax(int amount) {
        return (int) (amount * 0.05); //produce based on dynamic env attributes
    }

    private int calculateDeliveryFee(Cart cart) {
        return 100; //fetch from delivery service
    }
}