package com.finditnow.shopservice.repository;

import com.finditnow.shopservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByShopId(Long shopId);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.shop.ownerId = :ownerId")
    Optional<Product> findByIdAndShopOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);
}
