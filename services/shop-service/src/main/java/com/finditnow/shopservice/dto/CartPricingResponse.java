package com.finditnow.shopservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartPricingResponse {
    private int deliveryFee;
    private int tax;
    private int payable;
}