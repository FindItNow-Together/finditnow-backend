package com.finditnow.shopservice.controller;

import com.finditnow.shopservice.dto.ApiResponse;
import com.finditnow.shopservice.dto.CategoryRequest;
import com.finditnow.shopservice.dto.CategoryResponse;
import com.finditnow.shopservice.entity.CategoryType;
import com.finditnow.shopservice.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController extends BaseController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP')")
    public ApiResponse<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request) {

        return ApiResponse.<CategoryResponse>builder()
                .data(service.create(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getByType(
            @RequestParam CategoryType type) {

        return ApiResponse.<List<CategoryResponse>>builder()
                .data(service.getByType(type))
                .build();
    }
}

