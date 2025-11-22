package com.finditnow.auth.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * CREATE TABLE IF NOT EXISTS auth_oauth_google (
 * id UUID PRIMARY KEY,
 * user_id UUID NOT NULL,
 * google_user_id TEXT NOT NULL UNIQUE,
 * email TEXT NOT NULL,
 * access_token TEXT,
 * refresh_token TEXT,
 * created_at TIMESTAMPTZ DEFAULT NOW(),
 * last_login TIMESTAMPTZ
 * );
 */

public class AuthOauthGoogle {

    private UUID id;
    private UUID userId;
    private String googleUserId;
    private String email;
    private String accessToken;
    private String refreshToken;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastLogin;

    public AuthOauthGoogle() {
    }

    public AuthOauthGoogle(UUID id, UUID userId, String googleUserId,
                           String email, String accessToken, String refreshToken,
                           OffsetDateTime createdAt, OffsetDateTime lastLogin) {
        this.id = id;
        this.userId = userId;
        this.googleUserId = googleUserId;
        this.email = email;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getGoogleUserId() {
        return googleUserId;
    }

    public void setGoogleUserId(String googleUserId) {
        this.googleUserId = googleUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(OffsetDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}

