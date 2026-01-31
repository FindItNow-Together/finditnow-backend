package com.finditnow.jwt;

import com.finditnow.config.Config;
import com.finditnow.jwt.exceptions.JwtExpiredException;
import com.finditnow.jwt.exceptions.JwtInvalidException;
import com.finditnow.redis.RedisStore;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtService {
    private final SecretKey key;
    private final long accessTokenMillis = 60 * 60 * 1000L; // 15 min
    private final long serviceTokenMillis = 60 * 1000L; //60 seconds

    public JwtService() {
        String secret = Config.get("JWT_SECRET", "VERY_LONG_unimaginable_SECRET111");
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String generateAccessToken(String sessionId, String credId, String userId, String authProfile) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(sessionId).claim("credId", credId)
                .claim("userId", userId)
                .claim("profile", authProfile)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenMillis))
                .signWith(key)
                .compact();
    }

    public String generateServiceToken(String serviceName, String audience) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .issuer("auth-service")
                .subject("service:" + serviceName)
                .claim("aud", audience)
                .claim("typ", "service")
                .issuedAt(new Date(now))
                .expiration(new Date(now + serviceTokenMillis)) // 30–120s
                .signWith(key)
                .compact();
    }


    /**
     * Validate a token with blacklist checking. This method can be used by other microservices.
     *
     * @param token The JWT token to validate
     * @param redis RedisStore instance for blacklist checking (can be null to skip blacklist check)
     * @return Jws<Claims> if valid, null if invalid or blacklisted
     */
    public Jws<Claims> validateTokenWithBlacklist(String token, RedisStore redis) {
        if (token == null) {
            return null;
        }

        // Check blacklist if Redis is provided
        if (redis != null && redis.isAccessTokenBlacklisted(token)) {
            return null;
        }

        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        } catch (JwtException e) {
            return null;
        }
    }

    // validate externally in code when needed
    public Jws<Claims> parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

        } catch (ExpiredJwtException e) {
            throw new JwtExpiredException(e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtInvalidException(e);
        }
    }

    // get user info from the token
    public Map<String, String> parseTokenToUser(String token) {
        Jws<Claims> claims = parseClaims(token);
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("sessionId", claims.getPayload().getSubject());
        userInfo.put("credId", claims.getPayload().get("credId", String.class));
        userInfo.put("userId", claims.getPayload().get("userId", String.class));
        userInfo.put("profile", claims.getPayload().get("profile", String.class));
        userInfo.put("isSessionExpired", String.valueOf(claims.getPayload().getExpiration().before(new Date())));

        return userInfo;
    }

    public boolean isTokenExpired(String token) {
        Jws<Claims> claims = parseClaims(token);

        return claims.getPayload().getExpiration().before(new Date());
    }

    public boolean isServiceToken(String token) {
        Jws<Claims> claims = parseClaims(token);
        return "service".equals(claims.getPayload().get("typ", String.class));
    }

    /**
     * Get the expiration time remaining for a token in seconds
     *
     * @param token The JWT token
     * @return Remaining TTL in seconds, or 0 if token is expired or invalid
     */
    public long getTokenRemainingTtlSeconds(String token) {
        try {
            Jws<Claims> jws = parseClaims(token);
            Date expiration = jws.getPayload().getExpiration();
            if (expiration == null) {
                return accessTokenMillis / 1000L; // Default to access token TTL
            }
            long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000L;
            return Math.max(0, remaining);
        } catch (JwtException e) {
            return 0;
        }
    }

    public long getAccessTokenMillis() {
        return accessTokenMillis;
    }
}

