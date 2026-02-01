package com.finditnow.shopservice.service;

import com.finditnow.shopservice.dto.*;
import com.finditnow.shopservice.utils.DistanceUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SearchService {
    private final ProductService productService;
    private final ShopService shopService;
    private final ShopInventoryService shopInventoryService;

    public SearchService(ProductService productService, ShopService shopService,
            ShopInventoryService shopInventoryService) {
        this.productService = productService;
        this.shopService = shopService;
        this.shopInventoryService = shopInventoryService;
    }

    public PagedResponse<SearchOpportunityResponse> searchProducts(String query, Double lat, Double lng,
            String fulfillment, int page, int size, Long shopId) {
        FulfillmentPreference preference = FulfillmentPreference.from(fulfillment);

        Optional<Location> userLocation = Optional.empty();

        if (lat != null && lng != null) {
            userLocation = Optional.of(new Location(lat, lng));
        }

        List<InventoryResponse> inventories;
        if (shopId != null) {
            inventories = shopInventoryService.searchByProductName(query, shopId);
        } else {
            inventories = shopInventoryService.searchByProductName(query);
        }

        List<SearchOpportunityResponse> opportunities = new ArrayList<>();

        for (InventoryResponse inventory : inventories) {
            ProductResponse prod = productService.getById(inventory.getProduct().getId());
            ShopResponse shop = shopService.getShopById(inventory.getShop().getId());

            FulfillmentMode fulfillmentMode = "NO_DELIVERY".equals(shop.getDeliveryOption()) ? FulfillmentMode.PICKUP
                    : FulfillmentMode.DELIVERY;

            if (!preference.allows(fulfillmentMode))
                continue;

            InventorySearchResponse inventorySearchResponse = new InventorySearchResponse(inventory.getId(),
                    inventory.getReservedStock(), inventory.getPrice(), inventory.getStock());

            Double distance = null;
            if (userLocation.isPresent()) {
                distance = DistanceUtil.km(userLocation.get(), shop.getLatitude(), shop.getLongitude());
            }

            SearchOpportunityResponse opportunity = new SearchOpportunityResponse();

            opportunity.setProduct(prod);
            opportunity.setShop(shop);
            opportunity.setInventory(inventorySearchResponse);
            opportunity.setFulfillmentMode(fulfillmentMode);
            opportunity.setDistanceInKm(distance);

            opportunities.add(opportunity);
        }

        opportunities.sort(Comparator
                .comparing(SearchOpportunityResponse::getFulfillmentMode)
                .thenComparing(
                        SearchOpportunityResponse::getDistanceInKm,
                        Comparator.nullsLast(Double::compareTo)));

        return buildPagedResponseFromOpportunities(opportunities, page, size);
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
