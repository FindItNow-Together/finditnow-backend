package com.finditnow.shopservice.controller;

import com.finditnow.shopservice.dto.PagedResponse;
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

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<ProductResponse> addProduct(
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        ProductResponse response = productService.addProduct(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all products with pagination support.
     * GET /api/v1/products
     * 
     * @param page Page number (0-indexed, default: 0)
     * @param size Page size (default: 10)
     * @return ResponseEntity with paginated list of products
     */
    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<PagedResponse<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<ProductResponse> products = productService.getAll(page, size);
        return ResponseEntity.ok(products);
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        ProductResponse response = productService.updateProduct(id, request, userId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        productService.deleteProduct(id, userId, isAdmin);
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
