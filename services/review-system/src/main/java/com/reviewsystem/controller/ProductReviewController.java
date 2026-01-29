package com.reviewsystem.controller;

import com.reviewsystem.dto.*;
import com.reviewsystem.service.ProductReviewService;
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
@RequestMapping("/api/reviews/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Configure as needed for your frontend
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Create a new product review
     * POST /api/reviews/products
     */
    @PostMapping
    public ResponseEntity<ProductReviewResponseDTO> createReview(
            @Valid @RequestBody ProductReviewRequestDTO requestDTO,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        ProductReviewResponseDTO response = productReviewService.createReview(userId, requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Update an existing product review
     * PUT /api/reviews/products/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductReviewResponseDTO> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ProductReviewRequestDTO requestDTO,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        ProductReviewResponseDTO response = productReviewService.updateReview(userId, id, requestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a product review
     * DELETE /api/reviews/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        productReviewService.deleteReview(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all reviews for a specific product (approved only)
     * GET /api/reviews/products/{productId}/list?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping("/{productId}/list")
    public ResponseEntity<Page<ProductReviewResponseDTO>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Pageable pageable = createPageable(page, size, sort);
        Page<ProductReviewResponseDTO> reviews = productReviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get review statistics for a product
     * GET /api/reviews/products/{productId}/stats
     */
    @GetMapping("/{productId}/stats")
    public ResponseEntity<ReviewStatsDTO> getProductStats(@PathVariable Long productId) {
        ReviewStatsDTO stats = productReviewService.getProductReviewStats(productId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get current user's product reviews
     * GET /api/reviews/products/my-reviews?page=0&size=10
     */
    @GetMapping("/my-reviews")
    public ResponseEntity<Page<ProductReviewResponseDTO>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        Pageable pageable = createPageable(page, size, sort);
        Page<ProductReviewResponseDTO> reviews = productReviewService.getUserReviews(userId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Moderate a review (admin only)
     * PUT /api/reviews/products/{id}/moderate
     */
    @PutMapping("/{id}/moderate")
    // @PreAuthorize("hasRole('ADMIN')") // Uncomment when security is configured
    public ResponseEntity<ProductReviewResponseDTO> moderateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewModerationDTO moderationDTO) {

        ProductReviewResponseDTO response = productReviewService.moderateReview(id, moderationDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Get pending reviews (admin only)
     * GET /api/reviews/products/pending?page=0&size=10
     */
    @GetMapping("/pending")
    // @PreAuthorize("hasRole('ADMIN')") // Uncomment when security is configured
    public ResponseEntity<Page<ProductReviewResponseDTO>> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Pageable pageable = createPageable(page, size, sort);
        Page<ProductReviewResponseDTO> reviews = productReviewService.getPendingReviews(pageable);
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
            // Fallback if name is not a number (shouldn't happen with correct JWT)
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
