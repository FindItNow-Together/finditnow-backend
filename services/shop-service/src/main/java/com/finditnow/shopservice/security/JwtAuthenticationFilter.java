package com.finditnow.shopservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        logger.info("=== JWT Authentication Filter ===");
        logger.info("Request URI: " + request.getRequestURI());
        logger.info("Auth Header present: " + (authHeader != null));

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logger.info("Token extracted: " + token.substring(0, Math.min(20, token.length())) + "...");

            try {
                var claims = jwtUtil.extractClaims(token);
                // userId is stored as Integer/Long in the JWT, not String
                Integer userIdInt = claims.get("userId", Integer.class);
                String role = claims.get("role", String.class);

                logger.info("Claims extracted - userId: " + userIdInt + ", role: " + role);

                if (userIdInt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String userId = String.valueOf(userIdInt);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId,
                            token,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info(
                            "Authentication set successfully! Principal: " + userId + ", Authorities: ROLE_" + role);
                } else {
                    logger.warn("Authentication not set. UserIdInt: " + userIdInt + ", Existing auth: "
                            + SecurityContextHolder.getContext().getAuthentication());
                }
            } catch (Exception e) {
                // Invalid token, continue without authentication
                logger.warn("JWT validation failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            logger.info("No Bearer token found in request");
        }

        filterChain.doFilter(request, response);
    }
}