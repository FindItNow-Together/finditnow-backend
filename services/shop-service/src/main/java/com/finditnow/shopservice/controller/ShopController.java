package com.finditnow.shopservice.controller;

import com.finditnow.shopservice.dto.PagedResponse;
import com.finditnow.shopservice.dto.ShopRequest;
import com.finditnow.shopservice.dto.ShopResponse;
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
@RequestMapping("/api/v1/shops")
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
    public ResponseEntity<PagedResponse<ShopResponse>> getMyShops(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = extractUserId(authentication);
        PagedResponse<ShopResponse> shops = shopService.getShopsByOwner(userId, page, size);
        return ResponseEntity.ok(shops);
    }

    /**
     * Endpoint to get ALL shops in the system (ADMIN only).
     * GET /api/v1/shops/all
     * 
     * @param page Page number (0-indexed, default: 0)
     * @param size Page size (default: 10)
     * @return ResponseEntity with paginated list of all shops
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<ShopResponse>> getAllShops(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<ShopResponse> shops = shopService.getAllShops(page, size);
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
     * Endpoint to update a shop's information.
     * PUT /api/v1/shops/{id}
     * 
     * @param id             The shop ID to update from the URL path
     * @param request        The updated shop information
     * @param authentication Spring Security authentication object
     * @return ResponseEntity with updated shop data
     */
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

    /**
     * Search and filter shops.
     * GET /api/v1/shops/search
     * 
     * @param name           Optional shop name filter
     * @param deliveryOption Optional delivery option filter (NO_DELIVERY, IN_HOUSE_DRIVER, THIRD_PARTY_PARTNER)
     * @param lat            Optional latitude for distance-based sorting
     * @param lng            Optional longitude for distance-based sorting
     * @param page           Page number (0-indexed, default: 0)
     * @param size           Page size (default: 10)
     * @return ResponseEntity with paginated search results
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ShopResponse>> searchShops(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String deliveryOption,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PagedResponse<ShopResponse> shops = shopService.searchShops(name, deliveryOption, lat, lng, page, size);
        return ResponseEntity.ok(shops);
    }
}