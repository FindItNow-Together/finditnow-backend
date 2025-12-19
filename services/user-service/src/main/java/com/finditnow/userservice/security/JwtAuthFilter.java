package com.finditnow.userservice.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.finditnow.jwt.JwtService;
import com.finditnow.redis.RedisStore;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtAuthFilter implements Filter {

    private final JwtService jwt;
    private final RedisStore redis;

    public JwtAuthFilter(JwtService jwt, RedisStore redis) {
        this.jwt = jwt;
        this.redis = redis;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String token = extractToken(req);

        if (token == null || redis.isAccessTokenBlacklisted(token)) {
            System.out.print("AUTHENTICATION INVALID TOKEN");
            if (token != null) {
                System.out.println(" >>>> " + token);
            }
            doFilter(request, response, chain);
            return;
        }

        Map<String, String> userInfo = jwt.parseTokenToUser(token);

        // Attach identity to request context
        request.setAttribute("userId", userInfo.get("userId"));
        request.setAttribute("profile", userInfo.get("profile"));

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer "))
            return null;
        return header.substring(7);
    }
}