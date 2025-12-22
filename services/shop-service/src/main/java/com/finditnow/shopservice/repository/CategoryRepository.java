package com.finditnow.shopservice.repository;

import com.finditnow.shopservice.entity.Category;
import com.finditnow.shopservice.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCaseAndType(String name, CategoryType type);

    List<Category> findByTypeInAndActiveTrue(List<CategoryType> types);

    Optional<Category> findByNameIgnoreCase(String name);
}
