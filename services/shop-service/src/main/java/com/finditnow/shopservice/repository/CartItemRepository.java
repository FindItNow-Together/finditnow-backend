package com.finditnow.shopservice.repository;

import com.finditnow.shopservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByCartIdAndShopInventoryId(UUID cartId, Long shopInventoryId);

    void deleteAllByCartId(UUID cartId);
}
