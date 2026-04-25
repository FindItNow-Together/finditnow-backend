package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.*;
import com.finditnow.shopservice.entity.Category;
import com.finditnow.shopservice.entity.Product;
import com.finditnow.shopservice.entity.Shop;
import com.finditnow.shopservice.entity.ShopInventory;
import com.finditnow.shopservice.repository.ShopInventoryRepository;
import com.finditnow.shopservice.utils.DistanceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final ProductService productService;
    private final ShopService shopService;
    private final ShopInventoryService shopInventoryService;
    private final ShopInventoryRepository shopInventoryRepository;

    public PagedResponse<SearchOpportunityResponse> searchProducts(String query, Double lat, Double lng,
                                                                   String fulfillment, int page, int size, Long shopId) {
        FulfillmentPreference preference = FulfillmentPreference.from(fulfillment);

        Optional<Location> userLocation;
        if (lat != null && lng != null) {
            userLocation = Optional.of(new Location(lat, lng));
        } else {
            userLocation = Optional.empty();
        }

        // Use a single query with JOINs to fetch all data at once
        Pageable pageable = PageRequest.of(page, size);
        Page<ShopInventory> inventoryPage = shopInventoryRepository.searchOpportunities(
                query, shopId, pageable
        );

        List<SearchOpportunityResponse> opportunities = inventoryPage.getContent().stream()
                .map(inventory -> mapToOpportunity(inventory, userLocation, preference))
                .filter(Objects::nonNull) // Filter out null (fulfillment mismatch)
                .sorted(Comparator
                        .comparing(SearchOpportunityResponse::getFulfillmentMode)
                        .thenComparing(
                                SearchOpportunityResponse::getDistanceInKm,
                                Comparator.nullsLast(Double::compareTo)))
                .toList();

        return PagedResponse.<SearchOpportunityResponse>builder().content(opportunities).totalElements(inventoryPage.getTotalElements()).page(page).totalPages(inventoryPage.getTotalPages()).size(size)
                .first(inventoryPage.isFirst()).last(inventoryPage.isLast()).build();
    }

    private SearchOpportunityResponse mapToOpportunity(ShopInventory inventory,
                                                       Optional<Location> userLocation,
                                                       FulfillmentPreference preference) {
        Shop shop = inventory.getShop();
        Product product = inventory.getProduct();

        // Determine fulfillment mode
        FulfillmentMode fulfillmentMode = "NO_DELIVERY".equals(shop.getDeliveryOption())
                ? FulfillmentMode.PICKUP
                : FulfillmentMode.DELIVERY;

        // Filter by preference
        if (!preference.allows(fulfillmentMode)) {
            return null;
        }

        // Calculate distance
        Double distance = userLocation
                .map(loc -> DistanceUtil.km(loc, shop.getLatitude(), shop.getLongitude()))
                .orElse(null);

        // Map to response objects
        ProductResponse productResponse = mapToProductResponse(product);
        ShopResponse shopResponse = mapToShopResponse(shop);
        InventorySearchResponse inventoryResponse = new InventorySearchResponse(
                inventory.getId(),
                inventory.getReservedStock(),
                inventory.getPrice(),
                inventory.getStock()
        );

        SearchOpportunityResponse opportunity = new SearchOpportunityResponse();
        opportunity.setProduct(productResponse);
        opportunity.setShop(shopResponse);
        opportunity.setInventory(inventoryResponse);
        opportunity.setFulfillmentMode(fulfillmentMode);
        opportunity.setDistanceInKm(distance);

        return opportunity;
    }

    private ProductResponse mapToProductResponse(Product product) {
        CategoryResponse categoryResponse = product.getCategory() != null
                ? mapCategoryToCategoryResponse(product.getCategory()) : null;

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                categoryResponse,
                product.getImageUrl()
        );
    }

    private ShopResponse mapToShopResponse(Shop shop) {
        CategoryResponse categoryResponse = shop.getCategory() != null
                ? mapCategoryToCategoryResponse(shop.getCategory())
                : null;

        return new ShopResponse(
                shop.getId(),
                shop.getName(),
                shop.getAddress(),
                shop.getPhone(),
                shop.getOwnerId(),
                shop.getLatitude(),
                shop.getLongitude(),
                shop.getOpenHours(),
                shop.getDeliveryOption(),
                categoryResponse,
                shop.getImageUrl()
        );
    }

    private CategoryResponse mapCategoryToCategoryResponse(Category category) {
        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setId(category.getId());
        categoryResponse.setName(category.getName());
        categoryResponse.setDescription(category.getDescription());
        categoryResponse.setImageUrl(category.getImageUrl());
        categoryResponse.setType(category.getType());
        return categoryResponse;
    }

    public PagedResponse<SearchOpportunityResponse> buildPagedResponseFromOpportunities(
            List<SearchOpportunityResponse> opportunities, int page, int size) {
        int from = page * size;
        int to = Math.min(from + size, opportunities.size());

        List<SearchOpportunityResponse> pageContent = from >= opportunities.size()
                ? List.of()
                : opportunities.subList(from, to);

        PagedResponse<SearchOpportunityResponse> response = new PagedResponse<>();
        response.setContent(pageContent);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(opportunities.size());

        int totalPages = (int) Math.ceil((double) opportunities.size() / size);
        response.setTotalPages(totalPages);

        response.setFirst(page == 0);
        response.setLast(page >= totalPages - 1);

        return response;
    }

    public GlobalSearchResponse globalSearch(String query, Double lat, Double lng, int shopLimit, int productLimit) {
        // Search shops
        List<ShopResponse> allShops = shopService.searchShops(query);
        List<ShopResponse> shops = allShops;
        if (shopLimit > 0 && shops.size() > shopLimit) {
            shops = shops.subList(0, shopLimit);
        }

        // Search products (using existing method)
        PagedResponse<SearchOpportunityResponse> productResults = searchProducts(query, lat, lng, "BOTH", 0, productLimit, null);

        // Build response
        return GlobalSearchResponse.builder()
                .shops(shops)
                .products(productResults.getContent())
                .totalShops((long) allShops.size())
                .totalProducts(productResults.getTotalElements())
                .build();
    }
}
