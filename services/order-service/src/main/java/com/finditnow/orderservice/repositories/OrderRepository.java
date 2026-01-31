package com.finditnow.orderservice.repositories;

import com.finditnow.orderservice.entities.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Order> findByShopIdOrderByCreatedAtDesc(Long shopId);

    Page<Order> findByShopId(Long shopId, Pageable pageable);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.shopId = :shopId AND o.status <> 'CANCELLED'")
    Double calculateTotalEarningsByShopId(@Param("shopId") Long shopId);

    @Query("SELECT DISTINCT i.productName, o.createdAt FROM Order o JOIN o.orderItems i WHERE o.shopId = :shopId ORDER BY o.createdAt DESC")
    List<String> findRecentProductNamesByShopId(@Param("shopId") Long shopId, Pageable pageable);
}
