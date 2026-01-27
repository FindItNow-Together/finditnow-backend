package com.finditnow.shopservice.repository;

import com.finditnow.shopservice.entity.Cart;
import com.finditnow.shopservice.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserIdAndShopIdAndStatus(UUID userId, Long shopId, CartStatus status);

    List<Cart> findByUserIdAndStatus(UUID userId, CartStatus status);

    boolean existsByIdAndUserId(UUID cartId, UUID userId);
}
