package com.finditnow.shopservice.controller;

import com.finditnow.shopservice.dto.PagedResponse;
import com.finditnow.shopservice.dto.ProductRequest;
import com.finditnow.shopservice.dto.ProductResponse;
import com.finditnow.shopservice.dto.ShopRequest;
import com.finditnow.shopservice.dto.ShopResponse;
import com.finditnow.shopservice.service.ProductService;
import com.finditnow.shopservice.service.ShopService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/shop")
@Validated
public class ShopController extends BaseController {

    private final ShopService shopService;
    private final ProductService productService;

    public ShopController(ShopService shopService, ProductService productService) {
        this.shopService = shopService;
        this.productService = productService;
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<ShopResponse> registerShop(
            @Valid @RequestBody ShopRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        UUID ownerId = (isAdmin && request.getOwnerId() != null) ? request.getOwnerId() : userId;

        ShopResponse response = shopService.registerShop(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<PagedResponse<ShopResponse>> getMyShops(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UUID userId = extractUserId(authentication);
        PagedResponse<ShopResponse> shops = shopService.getShopsByOwner(userId, page, size);
        return ResponseEntity.ok(shops);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<ShopResponse>> getAllShops(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<ShopResponse> shops = shopService.getAllShops(page, size);
        return ResponseEntity.ok(shops);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<ShopResponse> getShop(@PathVariable Long id) {
        ShopResponse shop = shopService.getShopById(id);
        return ResponseEntity.ok(shop);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<ShopResponse> updateShop(
            @PathVariable Long id,
            @Valid @RequestBody ShopRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        ShopResponse response = shopService.updateShop(id, request, userId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<Void> deleteShop(
            @PathVariable Long id,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        shopService.deleteShop(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<Void> deleteShops(
            @RequestBody @NotEmpty(message = "Shop IDs list cannot be empty")
            List<@NotNull(message = "Shop ID cannot be null") Long> shopIds,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        shopService.deleteShops(shopIds, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    /* ================= PRODUCT APIs ================= */

    @PostMapping("/{shopId}/products")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<ProductResponse> addProduct(
            @PathVariable Long shopId,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        ProductResponse response = productService.addProduct(request, shopId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shopId}/products")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<List<ProductResponse>> getProductsByShop(@PathVariable Long shopId) {
        List<ProductResponse> products = productService.getProductsByShop(shopId);
        return ResponseEntity.ok(products);
    }

    /* ================= SEARCH API ================= */

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ShopResponse>> searchShops(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String deliveryOption,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<ShopResponse> shops =
                shopService.searchShops(name, deliveryOption, lat, lng, page, size);

        return ResponseEntity.ok(shops);
    }
}
