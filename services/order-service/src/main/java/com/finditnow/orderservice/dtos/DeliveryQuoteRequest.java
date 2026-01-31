package com.finditnow.orderservice.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeliveryQuoteRequest {
    private Double shopLatitude;
    private Double shopLongitude;
    private Double userLatitude;
    private Double userLongitude;
}
