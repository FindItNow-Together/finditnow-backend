package com.finditnow.shopservice.controller;

import com.finditnow.shopservice.exception.UnauthorizedException;
import com.finditnow.shopservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

/**
 * Base controller class providing common functionality for all controllers.
 * This reduces code duplication across multiple controller classes.
 */
@RequiredArgsConstructor
public abstract class BaseController {

    protected final JwtUtil jwtUtil;

    /**
     * Helper method to extract user ID from authentication token.
     * Throws UnauthorizedException if user ID cannot be extracted.
     * 
     * @param authentication Spring Security authentication object
     * @return User ID from JWT token
     * @throws UnauthorizedException if user ID extraction fails
     */
    protected Long extractUserId(Authentication authentication) {
        Long userId = jwtUtil.extractUserId(authentication);
        if (userId == null) {
            throw new UnauthorizedException("Unable to extract user ID from authentication token");
        }
        return userId;
    }
}
