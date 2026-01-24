package com.reviewsystem.dto;

import com.reviewsystem.entity.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopReviewResponseDTO {

    private Long id;
    private Long userId;
    private String userName; // Optional - can be populated if needed
    private Long shopId;
    private String shopName; // Optional - can be populated if needed
    private Long orderId;
    private BigDecimal rating;
    private Integer ownerInteractionRating;
    private Integer shopQualityRating;
    private Integer deliveryTimeRating;
    private String comment;
    private ReviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
