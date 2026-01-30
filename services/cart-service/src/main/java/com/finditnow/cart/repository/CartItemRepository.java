package com.finditnow.cart.repository;

import com.finditnow.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByCartIdAndInventoryId(UUID cartId, Long inventoryId);

    void deleteAllByCartId(UUID cartId);
}

