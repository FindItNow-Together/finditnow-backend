package com.finditnow.orderservice.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopResponse {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private UUID ownerId;
    private Double latitude;
    private Double longitude;
    private String openHours;
    private String deliveryOption;
    private String imageUrl;
}
