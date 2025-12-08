package com.finditnow.shopservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopResponse {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private Long ownerId;
    private Double latitude;
    private Double longitude;
    private String openHours;
    private String deliveryOption;
}

