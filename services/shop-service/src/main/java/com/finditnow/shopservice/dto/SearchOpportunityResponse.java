package com.finditnow.shopservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchOpportunityResponse {
    ProductResponse product;
    ShopResponse shop;
    InventorySearchResponse inventory;
    FulfillmentMode fulfillmentMode;
    Double distanceInKm;
}
