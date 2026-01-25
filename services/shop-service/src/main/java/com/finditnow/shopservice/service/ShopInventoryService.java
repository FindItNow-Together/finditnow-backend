package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.AddInventoryRequest;
import com.finditnow.shopservice.dto.InventoryResponse;
import com.finditnow.shopservice.dto.UpdateInventoryRequest;
import com.finditnow.shopservice.entity.Product;
import com.finditnow.shopservice.entity.Shop;
import com.finditnow.shopservice.entity.ShopInventory;
import com.finditnow.shopservice.exception.ForbiddenException;
import com.finditnow.shopservice.exception.NotFoundException;
import com.finditnow.shopservice.mapper.ShopInventoryMapper;
import com.finditnow.shopservice.repository.ProductRepository;
import com.finditnow.shopservice.repository.ShopInventoryRepository;
import com.finditnow.shopservice.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopInventoryService {
    private final ShopInventoryRepository shopInventoryRepository;
    private final ShopInventoryMapper inventoryMapper;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final ShopService shopService;

    @Transactional(readOnly = true)
    public InventoryResponse findById(Long id) {
        ShopInventory inventory = shopInventoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inventory not found with id: " + id));
        return inventoryMapper.toDto(inventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> findAllByShopId(Long shopId) {
        return inventoryMapper.toDtoList(shopInventoryRepository.findByShopId(shopId));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> searchByProductName(String prodName) {
        return inventoryMapper.toDtoList(shopInventoryRepository.searchByProductName(prodName));
    }

    /**
     * Adds a product to a shop's inventory.
     * Validates shop ownership and prevents duplicate products in the same shop.
     * 
     * @param shopId  The ID of the shop
     * @param request The inventory request with product ID, stock, and price
     * @param ownerId The ID of the user attempting to add inventory (for authorization)
     * @param isAdmin Whether the user is an admin (bypasses ownership check)
     * @return InventoryResponse with the created inventory
     * @throws NotFoundException  if shop or product is not found
     * @throws ForbiddenException if user is not the owner and not admin
     * @throws IllegalArgumentException if product already exists in shop inventory
     */
    @Transactional
    public InventoryResponse addInventory(Long shopId, AddInventoryRequest request, UUID ownerId, boolean isAdmin) {
        // Validate shop ownership
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found with id: " + shopId));

        if (!isAdmin && !shop.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("You don't have permission to add inventory to this shop");
        }

        // Validate product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.getProductId()));

        // Check if product already exists in shop inventory
        List<ShopInventory> existingInventory = shopInventoryRepository.findByShopId(shopId);
        boolean productExists = existingInventory.stream()
                .anyMatch(inv -> inv.getProduct().getId().equals(request.getProductId()));

        if (productExists) {
            throw new IllegalArgumentException("Product already exists in shop inventory");
        }

        // Validate stock >= reservedStock (should be 0 for new inventory)
        if (request.getStock() < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }

        // Create new inventory entry
        ShopInventory inventory = new ShopInventory();
        inventory.setShop(shop);
        inventory.setProduct(product);
        inventory.setStock(request.getStock());
        inventory.setPrice(request.getPrice());
        inventory.setReservedStock(0); // New inventory has no reserved stock

        ShopInventory savedInventory = shopInventoryRepository.save(inventory);
        return inventoryMapper.toDto(savedInventory);
    }

    /**
     * Updates an existing inventory entry (stock and/or price).
     * 
     * @param inventoryId The ID of the inventory to update
     * @param request     The update request with new stock and/or price
     * @param ownerId     The ID of the user attempting to update (for authorization)
     * @param isAdmin     Whether the user is an admin (bypasses ownership check)
     * @return InventoryResponse with updated inventory
     * @throws NotFoundException  if inventory is not found
     * @throws ForbiddenException if user is not the owner and not admin
     * @throws IllegalArgumentException if stock < reservedStock
     */
    @Transactional
    public InventoryResponse updateInventory(Long inventoryId, UpdateInventoryRequest request, UUID ownerId, boolean isAdmin) {
        ShopInventory inventory = shopInventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventory not found with id: " + inventoryId));

        // Validate shop ownership
        if (!isAdmin && !inventory.getShop().getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("You don't have permission to update this inventory");
        }

        // Update stock if provided
        if (request.getStock() != null) {
            if (request.getStock() < inventory.getReservedStock()) {
                throw new IllegalArgumentException("Stock cannot be less than reserved stock: " + inventory.getReservedStock());
            }
            inventory.setStock(request.getStock());
        }

        // Update price if provided
        if (request.getPrice() != null) {
            if (request.getPrice() < 0) {
                throw new IllegalArgumentException("Price cannot be negative");
            }
            inventory.setPrice(request.getPrice());
        }

        ShopInventory updatedInventory = shopInventoryRepository.save(inventory);
        return inventoryMapper.toDto(updatedInventory);
    }

    /**
     * Deletes an inventory entry (removes product from shop).
     * 
     * @param inventoryId The ID of the inventory to delete
     * @param ownerId     The ID of the user attempting to delete (for authorization)
     * @param isAdmin     Whether the user is an admin (bypasses ownership check)
     * @throws NotFoundException  if inventory is not found
     * @throws ForbiddenException if user is not the owner and not admin
     */
    @Transactional
    public void deleteInventory(Long inventoryId, UUID ownerId, boolean isAdmin) {
        ShopInventory inventory = shopInventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventory not found with id: " + inventoryId));

        // Validate shop ownership
        if (!isAdmin && !inventory.getShop().getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("You don't have permission to delete this inventory");
        }

        shopInventoryRepository.delete(inventory);
    }

    /**
     * Reserves stock for cart/order.
     * 
     * @param inventoryId The ID of the inventory
     * @param quantity    The quantity to reserve
     * @return InventoryResponse with updated reserved stock
     * @throws NotFoundException  if inventory is not found
     * @throws IllegalArgumentException if insufficient available stock
     */
    @Transactional
    public InventoryResponse reserveStock(Long inventoryId, Integer quantity) {
        ShopInventory inventory = shopInventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventory not found with id: " + inventoryId));

        int availableStock = inventory.getStock() - inventory.getReservedStock();
        if (quantity > availableStock) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + availableStock + ", Requested: " + quantity);
        }

        inventory.setReservedStock(inventory.getReservedStock() + quantity);
        ShopInventory updatedInventory = shopInventoryRepository.save(inventory);
        return inventoryMapper.toDto(updatedInventory);
    }

    /**
     * Releases reserved stock (when item removed from cart or order cancelled).
     * 
     * @param inventoryId The ID of the inventory
     * @param quantity    The quantity to release
     * @return InventoryResponse with updated reserved stock
     * @throws NotFoundException  if inventory is not found
     * @throws IllegalArgumentException if trying to release more than reserved
     */
    @Transactional
    public InventoryResponse releaseStock(Long inventoryId, Integer quantity) {
        ShopInventory inventory = shopInventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventory not found with id: " + inventoryId));

        if (quantity > inventory.getReservedStock()) {
            throw new IllegalArgumentException("Cannot release more stock than reserved. Reserved: " + inventory.getReservedStock() + ", Requested: " + quantity);
        }

        inventory.setReservedStock(inventory.getReservedStock() - quantity);
        ShopInventory updatedInventory = shopInventoryRepository.save(inventory);
        return inventoryMapper.toDto(updatedInventory);
    }
}
