package com.finditnow.shopservice.controller;

import com.finditnow.shopservice.dto.AddInventoryRequest;
import com.finditnow.shopservice.dto.InventoryResponse;
import com.finditnow.shopservice.dto.UpdateInventoryRequest;
import com.finditnow.shopservice.service.ShopInventoryService;
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
public class InventoryController extends BaseController {

    private final ShopInventoryService shopInventoryService;

    public InventoryController(ShopInventoryService shopInventoryService) {
        this.shopInventoryService = shopInventoryService;
    }

    /**
     * Get all inventory items for a specific shop.
     * GET /api/v1/shops/{shopId}/inventory
     * 
     * @param shopId The shop ID
     * @return ResponseEntity with list of inventory items
     */
    @GetMapping("/shops/{shopId}/inventory")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<List<InventoryResponse>> getShopInventory(@PathVariable Long shopId) {
        List<InventoryResponse> inventory = shopInventoryService.findAllByShopId(shopId);
        return ResponseEntity.ok(inventory);
    }

    /**
     * Get a specific inventory item by ID.
     * GET /api/v1/inventory/{id}
     * 
     * @param id The inventory ID
     * @return ResponseEntity with inventory item
     */
    @GetMapping("/inventory/{id}")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable Long id) {
        InventoryResponse inventory = shopInventoryService.findById(id);
        return ResponseEntity.ok(inventory);
    }

    /**
     * Add a product to a shop's inventory.
     * POST /api/v1/shops/{shopId}/inventory
     * 
     * @param shopId        The shop ID
     * @param request       The inventory request with product ID, stock, and price
     * @param authentication Spring Security authentication object
     * @return ResponseEntity with created inventory item
     */
    @PostMapping("/shops/{shopId}/inventory")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<InventoryResponse> addInventory(
            @PathVariable Long shopId,
            @Valid @RequestBody AddInventoryRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        InventoryResponse response = shopInventoryService.addInventory(shopId, request, userId, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an inventory item (stock and/or price).
     * PUT /api/v1/inventory/{id}
     * 
     * @param id            The inventory ID
     * @param request       The update request with new stock and/or price
     * @param authentication Spring Security authentication object
     * @return ResponseEntity with updated inventory item
     */
    @PutMapping("/inventory/{id}")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInventoryRequest request,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        InventoryResponse response = shopInventoryService.updateInventory(id, request, userId, isAdmin);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an inventory item (remove product from shop).
     * DELETE /api/v1/inventory/{id}
     * 
     * @param id            The inventory ID
     * @param authentication Spring Security authentication object
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/inventory/{id}")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<Void> deleteInventory(
            @PathVariable Long id,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        shopInventoryService.deleteInventory(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reserve stock for cart/order.
     * POST /api/v1/inventory/{id}/reserve
     * 
     * @param id      The inventory ID
     * @param quantity The quantity to reserve
     * @return ResponseEntity with updated inventory item
     */
    @PostMapping("/inventory/{id}/reserve")
    public ResponseEntity<InventoryResponse> reserveStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {

        InventoryResponse response = shopInventoryService.reserveStock(id, quantity);
        return ResponseEntity.ok(response);
    }

    /**
     * Release reserved stock (when item removed from cart or order cancelled).
     * POST /api/v1/inventory/{id}/release
     * 
     * @param id      The inventory ID
     * @param quantity The quantity to release
     * @return ResponseEntity with updated inventory item
     */
    @PostMapping("/inventory/{id}/release")
    public ResponseEntity<InventoryResponse> releaseStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {

        InventoryResponse response = shopInventoryService.releaseStock(id, quantity);
        return ResponseEntity.ok(response);
    }
}
