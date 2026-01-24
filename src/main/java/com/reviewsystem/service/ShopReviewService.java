package com.reviewsystem.service;

import com.reviewsystem.dto.*;
import com.reviewsystem.entity.ReviewStatus;
import com.reviewsystem.entity.ShopReview;
import com.reviewsystem.exception.DuplicateReviewException;
import com.reviewsystem.exception.ReviewNotFoundException;
import com.reviewsystem.exception.UnauthorizedReviewException;
import com.reviewsystem.repository.ShopReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShopReviewService {

    private final ShopReviewRepository shopReviewRepository;
    private final com.reviewsystem.repository.ShopRepository shopRepository;

    /**
     * Create a new shop review
     */
    @Transactional
    public ShopReviewResponseDTO createReview(Long userId, ShopReviewRequestDTO requestDTO) {
        // Check if user already reviewed this shop for this order
        if (shopReviewRepository.existsByUserIdAndShopIdAndOrderId(
                userId, requestDTO.getShopId(), requestDTO.getOrderId())) {
            throw new DuplicateReviewException(
                    "You have already reviewed this shop for this order");
        }

        ShopReview review = new ShopReview();
        review.setUserId(userId);
        review.setUserName("User " + userId); // Placeholder username
        review.setShopId(requestDTO.getShopId());
        // Fetch and set shop name
        shopRepository.findById(requestDTO.getShopId())
                .ifPresent(s -> review.setShopName(s.getName()));

        review.setOrderId(requestDTO.getOrderId());
        review.setRating(requestDTO.getRating());
        review.setOwnerInteractionRating(requestDTO.getOwnerInteractionRating());
        review.setShopQualityRating(requestDTO.getShopQualityRating());
        review.setDeliveryTimeRating(requestDTO.getDeliveryTimeRating());
        review.setComment(requestDTO.getComment());
        review.setStatus(ReviewStatus.APPROVED); // Auto-approve for demo

        // Manually set dates as fallback for JPA Auditing
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        review.setCreatedAt(now);
        review.setUpdatedAt(now);

        ShopReview savedReview = shopReviewRepository.save(review);
        return mapToResponseDTO(savedReview);
    }

    /**
     * Update an existing shop review
     */
    @Transactional
    public ShopReviewResponseDTO updateReview(Long userId, Long reviewId,
            ShopReviewRequestDTO requestDTO) {
        ShopReview review = shopReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        // Check if the user owns this review
        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedReviewException("You are not authorized to update this review");
        }

        // Update review fields
        review.setRating(requestDTO.getRating());
        review.setOwnerInteractionRating(requestDTO.getOwnerInteractionRating());
        review.setShopQualityRating(requestDTO.getShopQualityRating());
        review.setDeliveryTimeRating(requestDTO.getDeliveryTimeRating());
        review.setComment(requestDTO.getComment());
        // Reset status to PENDING after update
        review.setStatus(ReviewStatus.PENDING);

        review.setUpdatedAt(java.time.LocalDateTime.now());

        ShopReview updatedReview = shopReviewRepository.save(review);
        return mapToResponseDTO(updatedReview);
    }

    /**
     * Delete a shop review
     */
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        ShopReview review = shopReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        // Check if the user owns this review
        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedReviewException("You are not authorized to delete this review");
        }

        shopReviewRepository.delete(review);
    }

    /**
     * Get all reviews for a shop (approved only for public view)
     */
    @Transactional(readOnly = true)
    public Page<ShopReviewResponseDTO> getShopReviews(Long shopId, Pageable pageable) {
        Page<ShopReview> reviews = shopReviewRepository
                .findByShopIdAndStatus(shopId, ReviewStatus.APPROVED, pageable);
        return reviews.map(this::mapToResponseDTO);
    }

    /**
     * Get all reviews by a user
     */
    @Transactional(readOnly = true)
    public Page<ShopReviewResponseDTO> getUserReviews(Long userId, Pageable pageable) {
        Page<ShopReview> reviews = shopReviewRepository.findByUserId(userId, pageable);
        return reviews.map(this::mapToResponseDTO);
    }

    /**
     * Get review statistics for a shop
     */
    @Transactional(readOnly = true)
    public ReviewStatsDTO getShopReviewStats(Long shopId) {
        ReviewStatsDTO stats = new ReviewStatsDTO();

        // Calculate average rating
        BigDecimal avgRating = shopReviewRepository.calculateAverageRating(shopId);
        stats.setAverageRating(avgRating != null ? avgRating : BigDecimal.ZERO);

        // Get total and approved review counts
        stats.setTotalReviews(shopReviewRepository.countByShopId(shopId));
        stats.setApprovedReviews(
                shopReviewRepository.countByShopIdAndStatus(shopId, ReviewStatus.APPROVED));

        // Get rating distribution
        List<Object[]> distribution = shopReviewRepository.getRatingDistribution(shopId);
        Map<BigDecimal, Long> ratingMap = new HashMap<>();
        for (Object[] row : distribution) {
            ratingMap.put((BigDecimal) row[0], (Long) row[1]);
        }
        stats.setRatingDistribution(ratingMap);

        // Calculate shop-specific average ratings
        stats.setAverageOwnerInteractionRating(
                shopReviewRepository.calculateAverageOwnerInteractionRating(shopId));
        stats.setAverageShopQualityRating(
                shopReviewRepository.calculateAverageShopQualityRating(shopId));
        stats.setAverageDeliveryRating(
                shopReviewRepository.calculateAverageDeliveryRating(shopId));

        return stats;
    }

    /**
     * Moderate a review (admin function)
     */
    @Transactional
    public ShopReviewResponseDTO moderateReview(Long reviewId, ReviewModerationDTO moderationDTO) {
        ShopReview review = shopReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        review.setStatus(moderationDTO.getStatus());
        ShopReview moderatedReview = shopReviewRepository.save(review);
        return mapToResponseDTO(moderatedReview);
    }

    /**
     * Get pending reviews (admin function)
     */
    @Transactional(readOnly = true)
    public Page<ShopReviewResponseDTO> getPendingReviews(Pageable pageable) {
        Page<ShopReview> reviews = shopReviewRepository
                .findByStatus(ReviewStatus.PENDING, pageable);
        return reviews.map(this::mapToResponseDTO);
    }

    /**
     * Map entity to response DTO
     */
    private ShopReviewResponseDTO mapToResponseDTO(ShopReview review) {
        ShopReviewResponseDTO dto = new ShopReviewResponseDTO();
        dto.setId(review.getId());
        dto.setUserId(review.getUserId());
        dto.setUserName(review.getUserName());
        dto.setShopId(review.getShopId());
        dto.setShopName(review.getShopName());
        dto.setOrderId(review.getOrderId());
        dto.setRating(review.getRating());
        dto.setOwnerInteractionRating(review.getOwnerInteractionRating());
        dto.setShopQualityRating(review.getShopQualityRating());
        dto.setDeliveryTimeRating(review.getDeliveryTimeRating());
        dto.setComment(review.getComment());
        dto.setStatus(review.getStatus());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        return dto;
    }
}
