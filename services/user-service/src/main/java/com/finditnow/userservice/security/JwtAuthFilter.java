package com.finditnow.userservice.security;

import com.finditnow.jwt.JwtService;
import com.finditnow.jwt.exceptions.JwtExpiredException;
import com.finditnow.redis.RedisStore;
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

    // ✅ CRITICAL: Skip Swagger & public APIs
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/auth");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null || redis.isAccessTokenBlacklisted(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Map<String, String> userInfo = jwt.parseTokenToUser(token);

            request.setAttribute("userId",
                    UUID.fromString(userInfo.get("userId")));
            request.setAttribute("profile",
                    userInfo.get("profile"));

            filterChain.doFilter(request, response);

        } catch (JwtExpiredException e) {
            sendUnauthorized(response, "token_expired");
        }
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }

    private void sendUnauthorized(
            HttpServletResponse response,
            String message
    ) throws IOException {

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
