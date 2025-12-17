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
@RequestMapping("/api/v1")
public class ProductController extends BaseController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/shops/{shopId}/products")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<ProductResponse> addProduct(
            @PathVariable Long shopId,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        ProductResponse response = productService.addProduct(shopId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/shops/{shopId}/products")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<List<ProductResponse>> getProductsByShop(@PathVariable Long shopId) {
        List<ProductResponse> products = productService.getProductsByShop(shopId);
        return ResponseEntity.ok(products);
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        ProductResponse response = productService.updateProduct(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        productService.deleteProduct(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/products/bulk")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<Void> deleteProducts(
            @RequestBody List<Long> productIds,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        productService.deleteProducts(productIds, userId);
        return ResponseEntity.noContent().build();
    }
}
