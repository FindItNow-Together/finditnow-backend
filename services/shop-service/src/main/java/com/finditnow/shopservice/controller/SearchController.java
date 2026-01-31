package com.finditnow.shopservice.controller;

import com.finditnow.shopservice.dto.ApiResponse;
import com.finditnow.shopservice.dto.GlobalSearchResponse;
import com.finditnow.shopservice.dto.PagedResponse;
import com.finditnow.shopservice.dto.SearchOpportunityResponse;
import com.finditnow.shopservice.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/products")
    public ApiResponse<PagedResponse<SearchOpportunityResponse>> searchProducts(@RequestParam String q,
            @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "BOTH") String fulfillment, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) Long shopId) {
        return ApiResponse.<PagedResponse<SearchOpportunityResponse>>builder()
                .data(searchService.searchProducts(q, lat, lng, fulfillment, page, size, shopId)).success(true).build();
    }

    @GetMapping("/global")
    public ApiResponse<GlobalSearchResponse> globalSearch(
            @RequestParam String q,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "5") int shopLimit,
            @RequestParam(defaultValue = "5") int productLimit) {
        return ApiResponse.<GlobalSearchResponse>builder()
                .data(searchService.globalSearch(q, lat, lng, shopLimit, productLimit))
                .success(true)
                .build();
    }
}
