package com.finditnow.orderservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryQuoteResponse {
    private Double amount;
    private Double distanceKm;
}
