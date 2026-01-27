package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.CategoryResponse;
import com.finditnow.shopservice.dto.PagedResponse;
import com.finditnow.shopservice.dto.ShopRequest;
import com.finditnow.shopservice.dto.ShopResponse;
import com.finditnow.shopservice.entity.Category;
import com.finditnow.shopservice.entity.Shop;
import com.finditnow.shopservice.exception.ForbiddenException;
import com.finditnow.shopservice.exception.NotFoundException;
import com.finditnow.shopservice.repository.CategoryRepository;
import com.finditnow.shopservice.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;

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

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found with id: " + request.getCategoryId()));
            shop.setCategory(category);
        }

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

    /**
     * Returns shops owned by a user with pagination support.
     *
     * @param ownerId The ID of the shop owner
     * @param page    The page number (0-indexed)
     * @param size    The page size
     * @return PagedResponse with shops
     */
    @Transactional(readOnly = true)
    public PagedResponse<ShopResponse> getShopsByOwner(UUID ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Shop> shopPage = shopRepository.findByOwnerId(ownerId, pageable);

        List<ShopResponse> content = shopPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                shopPage.getNumber(),
                shopPage.getSize(),
                shopPage.getTotalElements(),
                shopPage.getTotalPages(),
                shopPage.isFirst(),
                shopPage.isLast()
        );
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
     * Returns all shops in the system with pagination support.
     * Used by admin endpoints.
     *
     * @param page The page number (0-indexed)
     * @param size The page size
     * @return PagedResponse with shops
     */
    @Transactional(readOnly = true)
    public PagedResponse<ShopResponse> getAllShops(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Shop> shopPage = shopRepository.findAll(pageable);

        List<ShopResponse> content = shopPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                shopPage.getNumber(),
                shopPage.getSize(),
                shopPage.getTotalElements(),
                shopPage.getTotalPages(),
                shopPage.isFirst(),
                shopPage.isLast()
        );
    }

    /**
     * Updates an existing shop's information.
     * Only the shop owner or an admin can update a shop.
     *
     * @param shopId  The ID of the shop to update
     * @param request The updated shop information
     * @param ownerId The ID of the user attempting to update (for authorization)
     * @param isAdmin Whether the user is an admin (bypasses ownership check)
     * @return ShopResponse with updated shop information
     * @throws NotFoundException  if the shop is not found
     * @throws ForbiddenException if user is not the owner and not admin
     */
    @Transactional
    public ShopResponse updateShop(Long shopId, ShopRequest request, UUID ownerId, boolean isAdmin) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found with id: " + shopId));

        // Verify that the user owns it OR is an admin
        if (!isAdmin && !shop.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("You don't have permission to update this shop");
        }

        // Update shop fields
        if (request.getName() != null) {
            shop.setName(request.getName());
        }
        if (request.getAddress() != null) {
            shop.setAddress(request.getAddress());
        }
        if (request.getPhone() != null) {
            shop.setPhone(request.getPhone());
        }
        if (request.getLatitude() != null) {
            shop.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            shop.setLongitude(request.getLongitude());
        }
        if (request.getOpenHours() != null) {
            shop.setOpenHours(request.getOpenHours());
        }
        if (request.getDeliveryOption() != null) {
            shop.setDeliveryOption(request.getDeliveryOption());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found with id: " + request.getCategoryId()));
            shop.setCategory(category);
        }

        Shop updatedShop = shopRepository.save(shop);
        return mapToResponse(updatedShop);
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
     * Searches shops by name or address.
     * 
     * @param query The search query string
     * @return List of ShopResponse DTOs matching the query
     */
    @Transactional(readOnly = true)
    public List<ShopResponse> searchShops(String query) {
        List<Shop> shops = shopRepository.searchByNameOrAddress(query);
        return shops.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Converts a Shop entity to a ShopResponse DTO.
     * This method extracts only the necessary information to send to the client.
     * 
     * @param shop The Shop entity to convert
     * @return ShopResponse DTO containing shop information
     */
    private ShopResponse mapToResponse(Shop shop) {
        CategoryResponse categoryResponse = null;
        if (shop.getCategory() != null) {
            categoryResponse = new CategoryResponse(
                    shop.getCategory().getId(),
                    shop.getCategory().getName(),
                    shop.getCategory().getDescription(),
                    shop.getCategory().getImageUrl(),
                    shop.getCategory().getType());
        }

        return new ShopResponse(
                shop.getId(), // Shop's unique identifier
                shop.getName(), // Shop name
                shop.getAddress(), // Shop address
                shop.getPhone(), // Shop phone number
                shop.getOwnerId(), // ID of the shop owner
                shop.getLatitude(), // Latitude coordinate
                shop.getLongitude(), // Longitude coordinate
                shop.getOpenHours(), // Open hours text
                shop.getDeliveryOption(), // Delivery option selected
                categoryResponse);
    }
}