package com.finditnow.shopservice.controller;

import java.util.UUID;

import org.springframework.security.core.Authentication;

import com.finditnow.shopservice.exception.UnauthorizedException;

/**
 * Base controller class providing common functionality for all controllers.
 * This reduces code duplication across multiple controller classes.
 */
public abstract class BaseController {
    /**
     * Helper method to extract user ID from authentication token.
     * Throws UnauthorizedException if user ID cannot be extracted.
     * 
     * @param authentication Spring Security authentication object
     * @return User ID from JWT token
     * @throws UnauthorizedException if user ID extraction fails
     */
    protected UUID extractUserId(Authentication authentication) {
        UUID userId = UUID.fromString((String) authentication.getPrincipal());
        if (userId == null) {
            throw new UnauthorizedException("Unable to extract user ID from authentication token");
        }
        return userId;
    }
}
