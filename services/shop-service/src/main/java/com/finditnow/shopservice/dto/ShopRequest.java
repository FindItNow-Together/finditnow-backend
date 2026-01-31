package com.finditnow.shopservice.dto;

import com.finditnow.shopservice.validation.DeliveryOption;
import com.finditnow.shopservice.validation.Latitude;
import com.finditnow.shopservice.validation.Longitude;
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
    @Latitude
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @Longitude
    private Double longitude;

    @NotBlank(message = "Open hours are required")
    private String openHours;

    @NotBlank(message = "Delivery option is required")
    @DeliveryOption
    private String deliveryOption;

    /**
     * Optional: ID of the owner to assign this shop to.
     * Only used if the requester is an ADMIN.
     */
    private UUID ownerId;

    /**
     * Optional: Category ID to assign to the shop.
     * Category type should be SHOP or BOTH.
     */
    private Long categoryId;
}
