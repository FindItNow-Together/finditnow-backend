package com.finditnow.shopservice.repository;

import com.finditnow.shopservice.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    List<Shop> findByOwnerId(Long ownerId);
    boolean existsByIdAndOwnerId(Long id, Long ownerId);
}

