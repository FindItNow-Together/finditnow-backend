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
    private OffsetDateTime createdAt;

    public AuthOauthGoogle() {
    }

    public AuthOauthGoogle(UUID id, UUID userId, String googleUserId,
                           String email, String accessToken, String refreshToken,
                           OffsetDateTime createdAt, OffsetDateTime lastLogin) {
        this.id = id;
        this.userId = userId;
        this.googleUserId = googleUserId;
        this.email = email;
        this.createdAt = createdAt;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

}

