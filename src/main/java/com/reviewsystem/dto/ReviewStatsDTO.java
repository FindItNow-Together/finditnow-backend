package com.reviewsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsDTO {

    private BigDecimal averageRating;
    private Long totalReviews;
    private Long approvedReviews;
    private Map<BigDecimal, Long> ratingDistribution; // e.g., {5.0: 10, 4.0: 5, 3.0: 2}

    // Product-specific stats
    private Double averageQualityRating;
    private Double averageDeliveryRating;

    // Shop-specific stats
    private Double averageOwnerInteractionRating;
    private Double averageShopQualityRating;
}
