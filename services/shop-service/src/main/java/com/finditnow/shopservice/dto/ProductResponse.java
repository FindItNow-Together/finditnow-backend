package com.finditnow.shopservice.dto;

import com.finditnow.shopservice.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private CategoryResponse category;
    private String imageUrl;
}

