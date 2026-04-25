package com.finditnow.deliveryservice.security;

import com.finditnow.jwt.JwtService;
import com.finditnow.jwt.exceptions.JwtValidationException;
import com.finditnow.redis.RedisStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

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
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UsernamePasswordAuthenticationToken authentication;
            Jws<Claims> claims = jwt.parseClaims(token);
            if("service".equals(claims.getPayload().get("typ", String.class))) {

                List<GrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_SERVICE"));

                authentication = new UsernamePasswordAuthenticationToken(
                        claims.getPayload().getSubject(),
                        null,
                        authorities
                );
            }else{
                String userId = claims.getPayload().get("userId", String.class);
                String profile = claims.getPayload().get("profile", String.class);

                List<GrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + profile.toUpperCase()));

                authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities
                );
                request.setAttribute("userId", userId);
                request.setAttribute("profile", profile);
            }

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (JwtValidationException e) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        }
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring(7);
    }
}