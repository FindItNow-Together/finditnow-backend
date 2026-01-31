package com.finditnow.shopservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductRequest {
    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    private String categoryId;

    private String category;

    private String imageUrl;

    private Float price;

    private Integer stock;
}

