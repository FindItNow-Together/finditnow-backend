package com.reviewsystem.service;

import com.reviewsystem.dto.*;
import com.reviewsystem.entity.ProductReview;
import com.reviewsystem.entity.ReviewStatus;
import com.reviewsystem.exception.DuplicateReviewException;
import com.reviewsystem.exception.ReviewNotFoundException;
import com.reviewsystem.exception.UnauthorizedReviewException;
import com.reviewsystem.repository.ProductReviewRepository;
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
public class ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final com.reviewsystem.repository.ProductRepository productRepository;

    /**
     * Create a new product review
     */
    @Transactional
    public ProductReviewResponseDTO createReview(Long userId, ProductReviewRequestDTO requestDTO) {
        // Check if user already reviewed this product for this order
        if (productReviewRepository.existsByUserIdAndProductIdAndOrderId(
                userId, requestDTO.getProductId(), requestDTO.getOrderId())) {
            throw new DuplicateReviewException(
                    "You have already reviewed this product for this order");
        }

        ProductReview review = new ProductReview();
        review.setUserId(userId);
        review.setUserName("User " + userId); // Placeholder username
        review.setProductId(requestDTO.getProductId());
        // Fetch and set product name
        productRepository.findById(requestDTO.getProductId())
                .ifPresent(p -> review.setProductName(p.getName()));

        review.setOrderId(requestDTO.getOrderId());
        review.setRating(requestDTO.getRating());
        review.setProductQualityRating(requestDTO.getProductQualityRating());
        review.setDeliveryTimeRating(requestDTO.getDeliveryTimeRating());
        review.setComment(requestDTO.getComment());
        review.setStatus(ReviewStatus.APPROVED); // Auto-approve for demo

        // Manually set dates as fallback for JPA Auditing
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        review.setCreatedAt(now);
        review.setUpdatedAt(now);

        ProductReview savedReview = productReviewRepository.save(review);
        return mapToResponseDTO(savedReview);
    }

    /**
     * Update an existing product review
     */
    @Transactional
    public ProductReviewResponseDTO updateReview(Long userId, Long reviewId,
            ProductReviewRequestDTO requestDTO) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        // Check if the user owns this review
        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedReviewException("You are not authorized to update this review");
        }

        // Update review fields
        review.setRating(requestDTO.getRating());
        review.setProductQualityRating(requestDTO.getProductQualityRating());
        review.setDeliveryTimeRating(requestDTO.getDeliveryTimeRating());
        review.setComment(requestDTO.getComment());
        // Reset status to PENDING after update
        review.setStatus(ReviewStatus.PENDING);

        review.setUpdatedAt(java.time.LocalDateTime.now());

        ProductReview updatedReview = productReviewRepository.save(review);
        return mapToResponseDTO(updatedReview);
    }

    /**
     * Delete a product review
     */
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        // Check if the user owns this review
        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedReviewException("You are not authorized to delete this review");
        }

        productReviewRepository.delete(review);
    }

    /**
     * Get all reviews for a product (approved only for public view)
     */
    @Transactional(readOnly = true)
    public Page<ProductReviewResponseDTO> getProductReviews(Long productId, Pageable pageable) {
        Page<ProductReview> reviews = productReviewRepository
                .findByProductIdAndStatus(productId, ReviewStatus.APPROVED, pageable);
        return reviews.map(this::mapToResponseDTO);
    }

    /**
     * Get all reviews by a user
     */
    @Transactional(readOnly = true)
    public Page<ProductReviewResponseDTO> getUserReviews(Long userId, Pageable pageable) {
        Page<ProductReview> reviews = productReviewRepository.findByUserId(userId, pageable);
        return reviews.map(this::mapToResponseDTO);
    }

    /**
     * Get review statistics for a product
     */
    @Transactional(readOnly = true)
    public ReviewStatsDTO getProductReviewStats(Long productId) {
        ReviewStatsDTO stats = new ReviewStatsDTO();

        // Calculate average rating
        BigDecimal avgRating = productReviewRepository.calculateAverageRating(productId);
        stats.setAverageRating(avgRating != null ? avgRating : BigDecimal.ZERO);

        // Get total and approved review counts
        stats.setTotalReviews(productReviewRepository.countByProductId(productId));
        stats.setApprovedReviews(
                productReviewRepository.countByProductIdAndStatus(productId, ReviewStatus.APPROVED));

        // Get rating distribution
        List<Object[]> distribution = productReviewRepository.getRatingDistribution(productId);
        Map<BigDecimal, Long> ratingMap = new HashMap<>();
        for (Object[] row : distribution) {
            ratingMap.put((BigDecimal) row[0], (Long) row[1]);
        }
        stats.setRatingDistribution(ratingMap);

        // Calculate average quality and delivery ratings
        stats.setAverageQualityRating(
                productReviewRepository.calculateAverageQualityRating(productId));
        stats.setAverageDeliveryRating(
                productReviewRepository.calculateAverageDeliveryRating(productId));

        return stats;
    }

    /**
     * Moderate a review (admin function)
     */
    @Transactional
    public ProductReviewResponseDTO moderateReview(Long reviewId, ReviewModerationDTO moderationDTO) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        review.setStatus(moderationDTO.getStatus());
        ProductReview moderatedReview = productReviewRepository.save(review);
        return mapToResponseDTO(moderatedReview);
    }

    /**
     * Get pending reviews (admin function)
     */
    @Transactional(readOnly = true)
    public Page<ProductReviewResponseDTO> getPendingReviews(Pageable pageable) {
        Page<ProductReview> reviews = productReviewRepository
                .findByStatus(ReviewStatus.PENDING, pageable);
        return reviews.map(this::mapToResponseDTO);
    }

    /**
     * Map entity to response DTO
     */
    private ProductReviewResponseDTO mapToResponseDTO(ProductReview review) {
        ProductReviewResponseDTO dto = new ProductReviewResponseDTO();
        dto.setId(review.getId());
        dto.setUserId(review.getUserId());
        dto.setUserName(review.getUserName());
        dto.setProductId(review.getProductId());
        dto.setProductName(review.getProductName());
        dto.setOrderId(review.getOrderId());
        dto.setRating(review.getRating());
        dto.setProductQualityRating(review.getProductQualityRating());
        dto.setDeliveryTimeRating(review.getDeliveryTimeRating());
        dto.setComment(review.getComment());
        dto.setStatus(review.getStatus());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        return dto;
    }
}
