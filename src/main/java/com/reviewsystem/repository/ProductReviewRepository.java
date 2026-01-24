package com.reviewsystem.repository;

import com.reviewsystem.entity.ProductReview;
import com.reviewsystem.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    // Find reviews by product ID with pagination
    Page<ProductReview> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    // Find reviews by user ID
    Page<ProductReview> findByUserId(Long userId, Pageable pageable);

    // Check if user already reviewed a product for a specific order
    boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);

    // Find a specific review by user, product, and order
    Optional<ProductReview> findByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);

    // Find reviews by product ID (all statuses)
    Page<ProductReview> findByProductId(Long productId, Pageable pageable);

    // Calculate average rating for a product
    @Query("SELECT AVG(pr.rating) FROM ProductReview pr WHERE pr.productId = :productId AND pr.status = 'APPROVED'")
    BigDecimal calculateAverageRating(@Param("productId") Long productId);

    // Count reviews by product ID and status
    long countByProductIdAndStatus(Long productId, ReviewStatus status);

    // Count total reviews by product ID
    long countByProductId(Long productId);

    // Get rating distribution for a product
    @Query("SELECT pr.rating, COUNT(pr) FROM ProductReview pr WHERE pr.productId = :productId AND pr.status = 'APPROVED' GROUP BY pr.rating")
    List<Object[]> getRatingDistribution(@Param("productId") Long productId);

    // Calculate average quality rating
    @Query("SELECT AVG(pr.productQualityRating) FROM ProductReview pr WHERE pr.productId = :productId AND pr.status = 'APPROVED'")
    Double calculateAverageQualityRating(@Param("productId") Long productId);

    // Calculate average delivery rating
    @Query("SELECT AVG(pr.deliveryTimeRating) FROM ProductReview pr WHERE pr.productId = :productId AND pr.status = 'APPROVED'")
    Double calculateAverageDeliveryRating(@Param("productId") Long productId);

    // Find reviews by status
    Page<ProductReview> findByStatus(ReviewStatus status, Pageable pageable);

    // Find user's review for a specific product
    Optional<ProductReview> findByUserIdAndId(Long userId, Long id);
}
