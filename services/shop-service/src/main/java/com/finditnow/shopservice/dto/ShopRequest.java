package com.finditnow.shopservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ShopRequest {

    @NotBlank(message = "Shop name is required")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotBlank(message = "Open hours are required")
    private String openHours;

    @NotBlank(message = "Delivery option is required")
    private String deliveryOption;

    /**
     * Optional: ID of the owner to assign this shop to.
     * Only used if the requester is an ADMIN.
     */
    private UUID ownerId;
}
