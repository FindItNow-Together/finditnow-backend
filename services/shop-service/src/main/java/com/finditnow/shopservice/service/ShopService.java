package com.finditnow.shopservice.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finditnow.shopservice.dto.ShopRequest;
import com.finditnow.shopservice.dto.ShopResponse;
import com.finditnow.shopservice.entity.Shop;
import com.finditnow.shopservice.exception.ForbiddenException;
import com.finditnow.shopservice.exception.NotFoundException;
import com.finditnow.shopservice.repository.ShopRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;

    @Transactional
    public ShopResponse registerShop(ShopRequest request, UUID ownerId) {
        Shop shop = new Shop();
        shop.setName(request.getName());
        shop.setAddress(request.getAddress());
        shop.setPhone(request.getPhone());
        shop.setOwnerId(ownerId);
        shop.setLatitude(request.getLatitude());
        shop.setLongitude(request.getLongitude());
        shop.setOpenHours(request.getOpenHours());
        shop.setDeliveryOption(request.getDeliveryOption());

        Shop savedShop = shopRepository.save(shop);
        return mapToResponse(savedShop);
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> getShopsByOwner(UUID ownerId) {
        List<Shop> shops = shopRepository.findByOwnerId(ownerId);
        return shops.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShopResponse getShopById(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found with id: " + shopId));
        return mapToResponse(shop);
    }

    /**
     * Returns all shops in the system as ShopResponse DTOs.
     * Used by admin endpoints.
     *
     * @return list of ShopResponse
     */
    @Transactional(readOnly = true)
    public List<ShopResponse> getAllShops() {
        List<Shop> shops = shopRepository.findAll();
        return shops.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a user is the owner of a specific shop.
     * This is used for authorization checks before allowing operations on a shop.
     * 
     * @param shopId  The ID of the shop to check
     * @param ownerId The ID of the user to verify ownership
     * @return true if the user owns the shop, false otherwise
     * @throws NotFoundException if the shop doesn't exist
     */
    @Transactional(readOnly = true)
    public boolean isOwner(Long shopId, UUID ownerId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found with id: " + shopId));
        return shop.getOwnerId().equals(ownerId);
    }

    /**
     * Deletes a single shop by its ID.
     * This method also deletes all products associated with the shop (cascade
     * delete).
     * 
     * @param shopId  The ID of the shop to delete
     * @param ownerId The ID of the user attempting to delete (for authorization)
     * @param isAdmin Whether the user is an admin (bypasses ownership check)
     * @throws NotFoundException  if the shop is not found
     * @throws ForbiddenException if user is not the owner and not admin
     */
    @Transactional
    public void deleteShop(Long shopId, UUID ownerId, boolean isAdmin) {
        // Find the shop entity
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found with id: " + shopId));

        // Verify that the user owns it OR is an admin
        if (!isAdmin && !shop.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("You don't have permission to delete this shop");
        }

        // Delete the shop (this will cascade delete all associated products due to
        // orphanRemoval = true)
        shopRepository.delete(shop);
    }

    /**
     * Deletes multiple shops at once.
     * This is useful for bulk operations like removing multiple subscriptions.
     * Validates all shops first, then deletes them in a single transaction.
     * 
     * @param shopIds List of shop IDs to delete
     * @param ownerId The ID of the user attempting to delete (for authorization)
     * @param isAdmin Whether the user is an admin (bypasses ownership check)
     * @throws NotFoundException  if any shop is not found
     * @throws ForbiddenException if user is not the owner of any shop and not admin
     */
    @Transactional
    public void deleteShops(List<Long> shopIds, UUID ownerId, boolean isAdmin) {
        if (shopIds == null || shopIds.isEmpty()) {
            throw new IllegalArgumentException("Shop IDs list cannot be empty");
        }

        // First, validate all shops exist and user owns them
        List<Shop> shopsToDelete = shopRepository.findAllById(shopIds);

        // Check if all shops were found
        if (shopsToDelete.size() != shopIds.size()) {
            throw new NotFoundException("One or more shops not found");
        }

        // Check ownership for all shops if not admin
        if (!isAdmin) {
            for (Shop shop : shopsToDelete) {
                if (!shop.getOwnerId().equals(ownerId)) {
                    throw new ForbiddenException("You don't have permission to delete shop with id: " + shop.getId());
                }
            }
        }

        // If all validations pass, delete all shops
        shopRepository.deleteAll(shopsToDelete);
    }

    /**
     * Converts a Shop entity to a ShopResponse DTO.
     * This method extracts only the necessary information to send to the client.
     * 
     * @param shop The Shop entity to convert
     * @return ShopResponse DTO containing shop information
     */
    private ShopResponse mapToResponse(Shop shop) {
        return new ShopResponse(
                shop.getId(), // Shop's unique identifier
                shop.getName(), // Shop name
                shop.getAddress(), // Shop address
                shop.getPhone(), // Shop phone number
                shop.getOwnerId(), // ID of the shop owner
                shop.getLatitude(), // Latitude coordinate
                shop.getLongitude(), // Longitude coordinate
                shop.getOpenHours(), // Open hours text
                shop.getDeliveryOption()// Delivery option selected
        );
    }
}