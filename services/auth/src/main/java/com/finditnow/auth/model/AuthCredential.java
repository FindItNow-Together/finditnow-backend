package com.finditnow.auth.model;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * CREATE TABLE IF NOT EXISTS auth_credentials (
 * id UUID PRIMARY KEY,
 * user_id UUID NOT NULL,
 * email TEXT UNIQUE,
 * phone TEXT UNIQUE,
 * password_hash TEXT,
 * is_email_verified BOOLEAN DEFAULT FALSE,
 * is_phone_verified BOOLEAN DEFAULT FALSE,
 * created_at TIMESTAMPTZ DEFAULT NOW()
 * );
 */
public class AuthCredential {
    private UUID id;
    private UUID userId;
    private String email;
    private String phone;
    private transient String passwordHash;
    private boolean isEmailVerified;
    private boolean isPhoneVerified;
    private OffsetDateTime createdAt;

    public AuthCredential() {
    }

    public AuthCredential(UUID id, UUID userId, String email, String phone, String passwordHash) {
        this(id, userId, email, phone, passwordHash, false, false, OffsetDateTime.now());
    }

    public AuthCredential(UUID id, UUID userId, String email, String phone, String passwordHash, boolean isEmailVerified, boolean isPhoneVerified, OffsetDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.isEmailVerified = isEmailVerified;
        this.isPhoneVerified = isPhoneVerified;
        this.createdAt = createdAt;
    }

    public UUID getUuid() {
        return id;
    }

    public void setUuid(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public boolean isPhoneVerified() {
        return isPhoneVerified;
    }

    public void setPhoneVerified(boolean phoneVerified) {
        isPhoneVerified = phoneVerified;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
