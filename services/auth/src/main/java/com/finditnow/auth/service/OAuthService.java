package com.finditnow.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finditnow.auth.dto.AuthResponse;
import com.finditnow.auth.model.AuthCredential;
import com.finditnow.auth.model.AuthSession;
import com.finditnow.config.Config;
import com.finditnow.jwt.JwtService;
import com.finditnow.redis.RedisStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class OAuthService {
    private static final Logger logger = LoggerFactory.getLogger(OAuthService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthService authService;
    private final JwtService jwt;
    private final RedisStore redis;

    public final String OAUTH_CLIENT_ID = Config.get("OAUTH_CLIENT_ID");
    public final String OAUTH_CLIENT_SECRET = Config.get("OAUTH_CLIENT_SECRET");
    public final String OAUTH_REDIRECT_URI = Config.get("OAUTH_REDIRECT_URI");

    public OAuthService(AuthService authService, RedisStore redis, JwtService jwt) {
        this.authService = authService;
        this.jwt = jwt;
        this.redis = redis;
    }

    public void saveOauthState(String state) {
        redis.setKey("oauth_state" + state, "1", 15 * 60);
    }

    public boolean existsOauthState(String state) {
        return "1".equals(redis.getKeyValue("oauth_state" + state));
    }

    /**
     * Handles Google OAuth authentication flow
     *
     * @param idToken The Google ID token from the client
     * @return AuthResponse containing access_token and refresh_token
     */
    public AuthResponse handleGoogleAuth(String idToken) {
        Map<String, String> data = new HashMap<>();

        // Decode and validate the Google ID token
        Map<String, Object> payload = decodeJwtPayload(idToken);
        if (payload == null || payload.get("email") == null) {
            data.put("error", "invalid_id_token");
            return new AuthResponse(401, data);
        }

        Boolean emailVerified = (Boolean) payload.get("email_verified");
        if (emailVerified == null || !emailVerified) {
            data.put("error", "email_not_verified");
            return new AuthResponse(401, data);
        }

        String sub = (String) payload.get("sub");

        if (sub == null || sub.isEmpty()) {
            data.put("error", "invalid_id_token");
            return new AuthResponse(401, data);
        }

        String email = (String) payload.get("email");
        String name = (String) payload.getOrDefault("name", "");

        try {
            // Find or create user account
            AuthCredential cred = authService.findCredentialByAuthProvider(sub);

            if (cred == null) {
                cred = authService.findCredentialByEmail(email);

                if (cred == null) {
                    cred = authService.findOrCreateCredWithOauth(sub, email);
                } else {
                    authService.createOauthByExistingCred(sub, email, cred.getUserId());
                }
            }
            // Use provided profile or fall back to existing user role
            String profile =
                    cred.getRole() != null
                            ? cred.getRole().toString()
                            : "UNASSIGNED";

            // Create authentication session
            AuthSession authSession = authService.createSessionFromCred(cred, "oauth");

            // Generate tokens
            String accessToken = jwt.generateAccessToken(
                    authSession.getId().toString(),
                    cred.getId().toString(),
                    cred.getUserId().toString(),
                    profile
            );

            // Store session in Redis
            authService.addSessionToRedis(
                    authSession,
                    cred.getUserId().toString(),
                    profile
            );

            // Build response
            data.put("access_token", accessToken);
            data.put("refresh_token", authSession.getSessionToken());
            data.put("profile", profile);
            data.put("email", email);

            if (name != null && !name.isEmpty()) {
                data.put("name", name);
            }

            return new AuthResponse(200, data);

        } catch (Exception e) {
            logger.error("Google OAuth authentication failed for email: " + email, e);
            data.put("error", "authentication_failed");
            return new AuthResponse(500, data);
        }
    }

    /**
     * Handles Microsoft OAuth authentication flow
     * Similar to Google but with Microsoft-specific token validation
     */
    public AuthResponse handleMicrosoftAuth(String accessToken, String authProfile) {
        // Implementation for Microsoft OAuth
        Map<String, String> data = new HashMap<>();
        data.put("error", "not_implemented");
        return new AuthResponse(501, data);
    }

    /**
     * Handles GitHub OAuth authentication flow
     */
    public AuthResponse handleGitHubAuth(String code, String authProfile) {
        // Implementation for GitHub OAuth
        Map<String, String> data = new HashMap<>();
        data.put("error", "not_implemented");
        return new AuthResponse(501, data);
    }

    /**
     * Decodes JWT payload without verification
     * Note: This is used for OAuth provider tokens that are already validated by the provider
     *
     * @param jwt The JWT token to decode
     * @return Map containing the decoded payload, or null if invalid
     */
    private Map<String, Object> decodeJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) {
                logger.warn("Invalid JWT format: expected 3 parts");
                return null;
            }

            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8
            );

            Map<String, Object> payload = mapper.readValue(payloadJson, Map.class);

            if (!"https://accounts.google.com".equals(payload.get("iss"))) {
                throw new SecurityException("Invalid issuer");
            }

            if (!OAUTH_CLIENT_ID.equals(payload.get("aud"))) {
                throw new SecurityException("Invalid audience");
            }

            long exp = ((Number) payload.get("exp")).longValue();
            long now = Instant.now().getEpochSecond();

            if (exp < now) {
                throw new SecurityException("Google Auth Token expired");
            }

            return payload;
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid Base64 encoding in JWT", e);
            return null;
        } catch (Exception e) {
            logger.error("Failed to decode JWT payload", e);
            return null;
        }
    }

    /**
     * Validates OAuth token with the provider's verification endpoint
     * This should be used for additional security when needed
     */
    private boolean verifyGoogleToken(String idToken) {
        // TODO: Implement actual Google token verification
        // https://developers.google.com/identity/sign-in/web/backend-auth
        return true;
    }
}
