package com.finditnow.shopservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddInventoryRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be non-negative")
    private Integer stock;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be non-negative")
    private Float price;
}

