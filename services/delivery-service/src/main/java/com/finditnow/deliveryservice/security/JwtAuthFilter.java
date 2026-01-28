package com.finditnow.deliveryservice.security;

import com.finditnow.jwt.JwtService;
import com.finditnow.jwt.exceptions.JwtExpiredException;
import com.finditnow.jwt.exceptions.JwtValidationException;
import com.finditnow.redis.RedisStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter { // Ensure 'extends'

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
            sendUnauthorized(response, "token_revoked");
            return;
        }

        try {
            Map<String, String> userInfo = jwt.parseTokenToUser(token);
            String userId = userInfo.get("userId");
            String profile = userInfo.get("profile");

            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + profile.toUpperCase()));

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // 2. Set additional request details
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Optional: Continue using attributes if needed for controllers
            request.setAttribute("userId", userId);
            request.setAttribute("profile", profile);
            filterChain.doFilter(request, response);

        } catch (JwtExpiredException e) {
            SecurityContextHolder.clearContext();
            sendUnauthorized(response, "token_expired");
        } catch (JwtValidationException e) {
            SecurityContextHolder.clearContext();
            sendUnauthorized(response, "token_invalid");
        }
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
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