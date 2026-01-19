package com.finditnow.shopservice.repository;

import com.finditnow.shopservice.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    List<Shop> findByOwnerId(UUID ownerId);
    boolean existsByIdAndOwnerId(Long id, UUID ownerId);

    @Query("""
        SELECT s FROM Shop s 
        WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(s.address) LIKE LOWER(CONCAT('%', :query, '%'))
        """)
    List<Shop> searchByNameOrAddress(@Param("query") String query);
}

