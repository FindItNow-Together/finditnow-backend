package com.finditnow.shopservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartPricingResponse {
    private double distanceKm;
    private double deliveryFee;
    private double tax;
    private double payable;
}