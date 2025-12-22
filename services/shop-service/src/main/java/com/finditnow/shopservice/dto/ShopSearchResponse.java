package com.finditnow.shopservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopSearchResponse {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private UUID ownerId;
    private Double latitude;
    private Double longitude;
    private String openHours;
    private String deliveryOption;

    double distanceToUserInKm;
}
