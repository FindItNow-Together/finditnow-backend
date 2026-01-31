package com.finditnow.shopservice.repository;

import com.finditnow.shopservice.entity.Product;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

        List<Product> findByNameContainingIgnoreCase(String name);

        @Query("""
                            SELECT p
                            FROM Product p
                            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                               OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
                        """)
        Page<Product> findPaginatedByQuery(
                        @Param("query") String query,
                        Pageable pageable);
}
