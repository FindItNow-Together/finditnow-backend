package com.reviewsystem.controller;

import com.reviewsystem.dto.*;
import com.reviewsystem.service.ShopReviewService;
import com.reviewsystem.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews/shops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Configure as needed for your frontend
public class ShopReviewController {

    private final ShopReviewService shopReviewService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Create a new shop review
     * POST /api/reviews/shops
     */
    @PostMapping
    public ResponseEntity<ShopReviewResponseDTO> createReview(
            @Valid @RequestBody ShopReviewRequestDTO requestDTO,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        ShopReviewResponseDTO response = shopReviewService.createReview(userId, requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Update an existing shop review
     * PUT /api/reviews/shops/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ShopReviewResponseDTO> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ShopReviewRequestDTO requestDTO,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        ShopReviewResponseDTO response = shopReviewService.updateReview(userId, id, requestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a shop review
     * DELETE /api/reviews/shops/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        shopReviewService.deleteReview(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all reviews for a specific shop (approved only)
     * GET /api/reviews/shops/{shopId}/list?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping("/{shopId}/list")
    public ResponseEntity<Page<ShopReviewResponseDTO>> getShopReviews(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Pageable pageable = createPageable(page, size, sort);
        Page<ShopReviewResponseDTO> reviews = shopReviewService.getShopReviews(shopId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get review statistics for a shop
     * GET /api/reviews/shops/{shopId}/stats
     */
    @GetMapping("/{shopId}/stats")
    public ResponseEntity<ReviewStatsDTO> getShopStats(@PathVariable Long shopId) {
        ReviewStatsDTO stats = shopReviewService.getShopReviewStats(shopId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get current user's shop reviews
     * GET /api/reviews/shops/my-reviews?page=0&size=10
     */
    @GetMapping("/my-reviews")
    public ResponseEntity<Page<ShopReviewResponseDTO>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        Pageable pageable = createPageable(page, size, sort);
        Page<ShopReviewResponseDTO> reviews = shopReviewService.getUserReviews(userId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Moderate a review (admin only)
     * PUT /api/reviews/shops/{id}/moderate
     */
    @PutMapping("/{id}/moderate")
    // @PreAuthorize("hasRole('ADMIN')") // Uncomment when security is configured
    public ResponseEntity<ShopReviewResponseDTO> moderateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewModerationDTO moderationDTO) {

        ShopReviewResponseDTO response = shopReviewService.moderateReview(id, moderationDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Get pending reviews (admin only)
     * GET /api/reviews/shops/pending?page=0&size=10
     */
    @GetMapping("/pending")
    // @PreAuthorize("hasRole('ADMIN')") // Uncomment when security is configured
    public ResponseEntity<Page<ShopReviewResponseDTO>> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Pageable pageable = createPageable(page, size, sort);
        Page<ShopReviewResponseDTO> reviews = shopReviewService.getPendingReviews(pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Extract user ID from JWT authentication
     */
    private Long extractUserId(Authentication authentication) {
        // For development/demo: if not authenticated, return default user ID 1
        if (authentication == null || authentication.getPrincipal() == null ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return 1L;
        }

        // This assumes the authentication principal contains the user ID
        // Adjust based on your JWT implementation
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return 1L;
        }
    }

    /**
     * Create pageable object with sorting
     */
    private Pageable createPageable(int page, int size, String[] sort) {
        if (sort.length >= 2) {
            String property = sort[0];
            Sort.Direction direction = sort[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            return PageRequest.of(page, size, Sort.by(direction, property));
        }
        return PageRequest.of(page, size);
    }
}
