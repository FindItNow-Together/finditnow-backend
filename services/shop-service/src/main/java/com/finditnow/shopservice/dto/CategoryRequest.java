package com.finditnow.shopservice.dto;

import com.finditnow.shopservice.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequest {
    @NotBlank
    private String name;

    private String description;

    private String imageUrl;

    @NotNull
    private CategoryType type;
}
