package com.finditnow.shopservice.repository;

import com.finditnow.shopservice.entity.ShopInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShopInventoryRepository extends JpaRepository<ShopInventory, Long> {
    List<ShopInventory> findByShopId(long shopId);

    @Query("SELECT inv FROM ShopInventory inv WHERE inv.product.name LIKE :prodName")
    List<ShopInventory> searchByProductName(@Param("prodName") String prodName);
}
