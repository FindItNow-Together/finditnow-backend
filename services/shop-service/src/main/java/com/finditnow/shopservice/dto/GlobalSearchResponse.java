package com.finditnow.shopservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchResponse {
    private List<ShopResponse> shops;
    private List<SearchOpportunityResponse> products;
    private Long totalShops;
    private Long totalProducts;
}
