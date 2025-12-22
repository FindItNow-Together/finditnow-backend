package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.CategoryRequest;
import com.finditnow.shopservice.dto.CategoryResponse;
import com.finditnow.shopservice.entity.Category;
import com.finditnow.shopservice.entity.CategoryType;
import com.finditnow.shopservice.mapper.CategoryMapper;
import com.finditnow.shopservice.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    public CategoryService(CategoryRepository repository, CategoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public CategoryResponse create(CategoryRequest request) {

        if (repository.existsByNameIgnoreCaseAndType(
                request.getName().trim(), request.getType())) {
            throw new IllegalArgumentException("Category already exists");
        }

        Category saved = repository.save(
                mapper.toEntity(request)
        );

        return mapper.toResponse(saved);
    }

    public List<CategoryResponse> getByType(CategoryType type) {

        List<CategoryType> types =
                type == CategoryType.BOTH
                        ? List.of(CategoryType.BOTH)
                        : List.of(type, CategoryType.BOTH);

        return repository.findByTypeInAndActiveTrue(types)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}

