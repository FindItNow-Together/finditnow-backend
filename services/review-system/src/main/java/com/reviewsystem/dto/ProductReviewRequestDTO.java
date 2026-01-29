package com.reviewsystem.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewRequestDTO {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Rating is required")
    @DecimalMin(value = "1.0", message = "Rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Rating must not exceed 5.0")
    private BigDecimal rating;

    @NotNull(message = "Product quality rating is required")
    @Min(value = 1, message = "Product quality rating must be at least 1")
    @Max(value = 5, message = "Product quality rating must not exceed 5")
    private Integer productQualityRating;

    @NotNull(message = "Delivery time rating is required")
    @Min(value = 1, message = "Delivery time rating must be at least 1")
    @Max(value = 5, message = "Delivery time rating must not exceed 5")
    private Integer deliveryTimeRating;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;
}
