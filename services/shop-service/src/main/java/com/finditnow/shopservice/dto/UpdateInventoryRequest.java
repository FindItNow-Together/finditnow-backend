package com.finditnow.shopservice.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateInventoryRequest {
    @Min(value = 0, message = "Stock must be non-negative")
    private Integer stock;

    @Min(value = 0, message = "Price must be non-negative")
    private Float price;
}

