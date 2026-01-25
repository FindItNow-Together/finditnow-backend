package com.finditnow.shopservice.controller;

import com.finditnow.shopservice.dto.ProductRequest;
import com.finditnow.shopservice.dto.ProductResponse;
import com.finditnow.shopservice.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ProductController extends BaseController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<ProductResponse> addProduct(
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        ProductResponse response = productService.addProduct(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String query) {
        List<ProductResponse> products;
        if (query != null && !query.trim().isEmpty()) {
            products = productService.searchProducts(query);
        } else {
            products = productService.getAll();
        }
        return ResponseEntity.ok(products);
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/products/bulk")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<Void> deleteProducts(
            @RequestBody List<Long> productIds) {
        productService.deleteProducts(productIds);
        return ResponseEntity.noContent().build();
    }
}
