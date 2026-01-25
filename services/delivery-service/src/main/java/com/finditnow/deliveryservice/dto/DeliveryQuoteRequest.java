package com.finditnow.deliveryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryQuoteRequest {
    private Double shopLatitude;
    private Double shopLongitude;
    private Double userLatitude;
    private Double userLongitude;
}
