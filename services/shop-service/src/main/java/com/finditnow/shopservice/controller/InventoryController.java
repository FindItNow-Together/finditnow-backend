package com.finditnow.shopservice.controller;

import com.finditnow.shopservice.dto.InventoryRequest;
import com.finditnow.shopservice.dto.InventoryResponse;
import com.finditnow.shopservice.dto.ProductRequest;
import com.finditnow.shopservice.service.ShopInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final ShopInventoryService inventoryService;

    @GetMapping("/{shopId}/inventory")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ResponseEntity<List<InventoryResponse>> getInventory(@PathVariable Long shopId) {
        return ResponseEntity.ok(inventoryService.getInventory(shopId));
    }

    @PostMapping("/{shopId}/inventory/existing")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<InventoryResponse> addExistingProduct(
            @PathVariable Long shopId,
            @RequestBody InventoryRequest request) {
        return ResponseEntity.ok(inventoryService.addExistingProduct(shopId, request));
    }

    @PostMapping("/{shopId}/inventory/new")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<InventoryResponse> addNewProduct(
            @PathVariable Long shopId,
            @Valid @RequestBody ProductRequest productRequest,
            @RequestParam int stock,
            @RequestParam float price) {
        return ResponseEntity.ok(inventoryService.addNewProduct(shopId, productRequest, stock, price));
    }
}
