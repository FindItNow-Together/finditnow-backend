package com.reviewsystem.repository;

import com.reviewsystem.entity.ReviewStatus;
import com.reviewsystem.entity.ShopReview;
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
public interface ShopReviewRepository extends JpaRepository<ShopReview, Long> {

    // Find reviews by shop ID with pagination
    Page<ShopReview> findByShopIdAndStatus(Long shopId, ReviewStatus status, Pageable pageable);

    // Find reviews by user ID
    Page<ShopReview> findByUserId(Long userId, Pageable pageable);

    // Check if user already reviewed a shop for a specific order
    boolean existsByUserIdAndShopIdAndOrderId(Long userId, Long shopId, Long orderId);

    // Find a specific review by user, shop, and order
    Optional<ShopReview> findByUserIdAndShopIdAndOrderId(Long userId, Long shopId, Long orderId);

    // Find reviews by shop ID (all statuses)
    Page<ShopReview> findByShopId(Long shopId, Pageable pageable);

    // Calculate average rating for a shop
    @Query("SELECT AVG(sr.rating) FROM ShopReview sr WHERE sr.shopId = :shopId AND sr.status = 'APPROVED'")
    BigDecimal calculateAverageRating(@Param("shopId") Long shopId);

    // Count reviews by shop ID and status
    long countByShopIdAndStatus(Long shopId, ReviewStatus status);

    // Count total reviews by shop ID
    long countByShopId(Long shopId);

    // Get rating distribution for a shop
    @Query("SELECT sr.rating, COUNT(sr) FROM ShopReview sr WHERE sr.shopId = :shopId AND sr.status = 'APPROVED' GROUP BY sr.rating")
    List<Object[]> getRatingDistribution(@Param("shopId") Long shopId);

    // Calculate average owner interaction rating
    @Query("SELECT AVG(sr.ownerInteractionRating) FROM ShopReview sr WHERE sr.shopId = :shopId AND sr.status = 'APPROVED'")
    Double calculateAverageOwnerInteractionRating(@Param("shopId") Long shopId);

    // Calculate average shop quality rating
    @Query("SELECT AVG(sr.shopQualityRating) FROM ShopReview sr WHERE sr.shopId = :shopId AND sr.status = 'APPROVED'")
    Double calculateAverageShopQualityRating(@Param("shopId") Long shopId);

    // Calculate average delivery rating
    @Query("SELECT AVG(sr.deliveryTimeRating) FROM ShopReview sr WHERE sr.shopId = :shopId AND sr.status = 'APPROVED'")
    Double calculateAverageDeliveryRating(@Param("shopId") Long shopId);

    // Find reviews by status
    Page<ShopReview> findByStatus(ReviewStatus status, Pageable pageable);

    // Find user's review for a specific shop
    Optional<ShopReview> findByUserIdAndId(Long userId, Long id);
}
