package com.finditnow.shopservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finditnow.shopservice.dto.ShopRequest;
import com.finditnow.shopservice.dto.ShopResponse;
import com.finditnow.shopservice.service.ShopService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
@Validated
public class ShopController extends BaseController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<ShopResponse> registerShop(
            @Valid @RequestBody ShopRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // If admin and ownerId is provided, use it. Otherwise use the authenticated
        // user's ID.
        UUID ownerId = (isAdmin && request.getOwnerId() != null) ? request.getOwnerId() : userId;

        ShopResponse response = shopService.registerShop(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<List<ShopResponse>> getMyShops(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        List<ShopResponse> shops = shopService.getShopsByOwner(userId);
        return ResponseEntity.ok(shops);
    }

    /**
     * Endpoint to get ALL shops in the system (ADMIN only).
     * GET /api/v1/shops
     * 
     * @return ResponseEntity with list of all shops
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ShopResponse>> getAllShops() {
        List<ShopResponse> shops = shopService.getAllShops();
        return ResponseEntity.ok(shops);
    }

    /**
     * Endpoint to get a specific shop by its ID.
     * GET /api/v1/shops/{id}
     * 
     * @param id The shop ID from the URL path
     * @return ResponseEntity with shop data
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<ShopResponse> getShop(@PathVariable Long id) {
        ShopResponse shop = shopService.getShopById(id);
        return ResponseEntity.ok(shop);
    }

    /**
     * Endpoint to delete a single shop.
     * DELETE /api/v1/shops/{id}
     * 
     * @param id             The shop ID to delete from the URL path
     * @param authentication Spring Security authentication object
     * @return ResponseEntity with no content (HTTP 204) on success
     */
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

    /**
     * Endpoint to delete multiple shops at once (bulk delete).
     * DELETE /api/v1/shops/bulk
     * 
     * @param shopIds        List of shop IDs to delete from the request body
     * @param authentication Spring Security authentication object
     * @return ResponseEntity with no content (HTTP 204) on success
     */
    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<Void> deleteShops(
            @RequestBody @NotEmpty(message = "Shop IDs list cannot be empty") List<@NotNull(message = "Shop ID cannot be null") Long> shopIds,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        shopService.deleteShops(shopIds, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }
}