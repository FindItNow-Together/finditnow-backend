package com.finditnow.shopservice.repository;

import com.finditnow.shopservice.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    List<Shop> findByOwnerId(UUID ownerId);
    Page<Shop> findByOwnerId(UUID ownerId, Pageable pageable);
    boolean existsByIdAndOwnerId(Long id, UUID ownerId);

<<<<<<< HEAD
    @Query("""
        SELECT s FROM Shop s 
        WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(s.address) LIKE LOWER(CONCAT('%', :query, '%'))
        """)
    List<Shop> searchByNameOrAddress(@Param("query") String query);
=======
    @Query("SELECT s FROM Shop s WHERE " +
           "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:deliveryOption IS NULL OR s.deliveryOption = :deliveryOption)")
    Page<Shop> searchShops(@Param("name") String name, 
                          @Param("deliveryOption") String deliveryOption, 
                          Pageable pageable);
>>>>>>> 49c6c06b8ddd591a5e5d2dd8ed7431f333caf104
}

