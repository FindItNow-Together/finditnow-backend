package com.finditnow.userservice.security;

import com.finditnow.jwt.JwtService;
import com.finditnow.jwt.exceptions.JwtExpiredException;
import com.finditnow.redis.RedisStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final RedisStore redis;

    public JwtAuthFilter(JwtService jwt, RedisStore redis) {
        this.jwt = jwt;
        this.redis = redis;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (redis.isAccessTokenBlacklisted(token)) {
            System.out.println("AUTHENTICATION INVALID TOKEN (blacklisted) >>>> " + token);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Jws<Claims> claims = jwt.parseClaims(token);
            String tokenType = claims.getPayload().get("typ", String.class);

            if ("service".equals(tokenType)) {
                // Service-to-service token
                String serviceName = claims.getPayload().getSubject();
                request.setAttribute("tokenType", "service");
                request.setAttribute("serviceName", serviceName);
                request.setAttribute("role", "SERVICE");

                System.out.println("Service token authenticated: " + serviceName);
            } else {
                // User token
                String userId = claims.getPayload().get("userId", String.class);
                String profile = claims.getPayload().get("profile", String.class);

                request.setAttribute("tokenType", "user");
                request.setAttribute("userId", UUID.fromString(userId));
                request.setAttribute("profile", profile);
                request.setAttribute("role", profile.toUpperCase());

                System.out.println("User token authenticated: " + userId + " (" + profile + ")");
            }

            filterChain.doFilter(request, response);

        } catch (JwtException e) {
            // Covers JwtExpiredException and other JWT validation errors
            System.out.println("JWT validation failed: " + e.getMessage());
            sendUnauthorized(response, "token_expired");
        } catch (Exception e) {
            System.out.println("Authentication error: " + e.getMessage());
            sendUnauthorized(response, "unauthorized");
        }
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) return null;
        return header.substring(7);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        response.getWriter().write("""
                {
                  "success": false,
                  "error": "%s"
                }
                """.formatted(message));
    }
}