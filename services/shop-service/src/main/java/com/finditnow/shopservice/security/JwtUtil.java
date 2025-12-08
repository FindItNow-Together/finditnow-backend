package com.finditnow.shopservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            return null;
        }

        // Extract from JWT token in credentials
        if (authentication.getCredentials() instanceof String) {
            String token = (String) authentication.getCredentials();
            try {
                Claims claims = extractClaims(token);
                // userId is stored as Integer in JWT, not Long
                Integer userIdInt = claims.get("userId", Integer.class);
                return userIdInt != null ? userIdInt.longValue() : null;
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    public String extractRole(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        // Extract role from authorities
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }
}
