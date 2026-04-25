package com.finditnow.shopservice.repository;

import com.finditnow.shopservice.entity.ShopInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShopInventoryRepository extends JpaRepository<ShopInventory, Long> {
    List<ShopInventory> findByShopId(long shopId);

    @Query("SELECT inv FROM ShopInventory inv WHERE lower(inv.product.name) LIKE lower(concat('%', :prodName, '%'))")
    List<ShopInventory> searchByProductName(@Param("prodName") String prodName);

    @Query("SELECT inv FROM ShopInventory inv WHERE inv.shop.id = :shopId AND lower(inv.product.name) LIKE lower(concat('%', :prodName, '%'))")
    List<ShopInventory> searchByProductNameAndShopId(@Param("prodName") String prodName, @Param("shopId") Long shopId);

    @Query("SELECT inv FROM ShopInventory inv WHERE inv.product.id = :productId")
    List<ShopInventory> findByProductId(@Param("productId") Long productId);

    @Query("SELECT inv FROM ShopInventory inv WHERE inv.product.id = :productId AND inv.shop.ownerId = :ownerId")
    List<ShopInventory> findByProductIdAndOwnerId(@Param("productId") Long productId, @Param("ownerId") java.util.UUID ownerId);

    @Query("""
    SELECT inv FROM ShopInventory inv
    JOIN FETCH inv.shop s
    JOIN FETCH inv.product p
    LEFT JOIN FETCH p.category
    LEFT JOIN FETCH s.category
    WHERE (COALESCE(:query, '') = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')))
      AND (:shopId IS NULL OR s.id = :shopId)
    """)
    Page<ShopInventory> searchOpportunities(
            @Param("query") String query,
            @Param("shopId") Long shopId,
            Pageable pageable
    );
}
