package com.finditnow.shopservice.repository;

import com.finditnow.shopservice.entity.Cart;
import com.finditnow.shopservice.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
    List<Cart> findByUserIdAndStatus(UUID userId, CartStatus status);

    boolean existsByIdAndUserId(UUID cartId, UUID userId);

    /**
     * Find cart by user, shop, and status WITHOUT fetching related entities
     * Use this for simple operations that don't need product details
     */
    Optional<Cart> findByUserIdAndShopIdAndStatus(UUID userId, Long shopId, CartStatus status);

    /**
     * Find cart by user, shop, and status WITH all related entities eagerly fetched
     * Use this when you need to access cart items, inventory, and product details
     * This prevents LazyInitializationException when accessing nested relationships
     */
    @Query("SELECT c FROM Cart c " +
            "LEFT JOIN FETCH c.items ci " +
            "LEFT JOIN FETCH ci.shopInventory si " +
            "LEFT JOIN FETCH si.product p " +
            "WHERE c.userId = :userId AND c.shopId = :shopId AND c.status = :status")
    Optional<Cart> findByUserIdAndShopIdAndStatusWithDetails(
            @Param("userId") UUID userId,
            @Param("shopId") Long shopId,
            @Param("status") CartStatus status
    );

    /**
     * Find cart by user, shop, and status WITH all related entities eagerly fetched
     * Use this when you need to access cart items, inventory, and product details
     * This prevents LazyInitializationException when accessing nested relationships
     */
    @Query("SELECT c FROM Cart c " +
            "LEFT JOIN FETCH c.items ci " +
            "LEFT JOIN FETCH ci.shopInventory si " +
            "LEFT JOIN FETCH si.product p " +
            "WHERE c.userId = :userId AND c.status = :status")
    Optional<Cart> findByUserIdAndStatusWithDetails(
            @Param("userId") UUID userId,
            @Param("status") CartStatus status
    );

    /**
     * Find cart by ID WITH all related entities eagerly fetched
     * Use this when you need to access cart details for display/checkout
     */
    @Query("SELECT c FROM Cart c " +
            "LEFT JOIN FETCH c.items ci " +
            "LEFT JOIN FETCH ci.shopInventory si " +
            "LEFT JOIN FETCH si.product p " +
            "WHERE c.id = :cartId")
    Optional<Cart> findByIdWithDetails(@Param("cartId") UUID cartId);
}
