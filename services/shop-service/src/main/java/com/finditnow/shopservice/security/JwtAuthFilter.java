package com.finditnow.shopservice.security;

import com.finditnow.jwt.JwtService;
import com.finditnow.redis.RedisStore;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;

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
            ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            Map<String, String> userInfo = jwt.parseTokenToUser(token);
            String userId = userInfo.get("userId");
            String profile = userInfo.get("profile");

            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + profile.toUpperCase()));

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities);

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            request.setAttribute("userId", userId);
            request.setAttribute("profile", userInfo.get("profile"));

        } catch (Exception e) {
            ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            return null;
        return header.substring(7);
    }
}