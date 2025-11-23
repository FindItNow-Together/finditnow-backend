package com.finditnow.auth.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AuthSession {

    private UUID id;
    private UUID credId;
    private String sessionToken;
    private String sessionMethod;
    private String ipAddress;
    private String userAgent;
    private OffsetDateTime expiresAt;
    private boolean isValid;
    private OffsetDateTime createdAt;
    private OffsetDateTime revokedAt;

    public AuthSession() {
    }

    public AuthSession(UUID id, UUID credId, String sessionToken, String sessionMethod, OffsetDateTime expiresAt) {
        this(id, credId, sessionToken, sessionMethod, null, null, expiresAt, true, OffsetDateTime.now(), null);
    }

    public AuthSession(UUID id, UUID credId, String sessionToken, String sessionMethod, String ipAddress, String userAgent, OffsetDateTime expiresAt, boolean isValid, OffsetDateTime createdAt, OffsetDateTime revokedAt) {
        this.id = id;
        this.credId = credId;
        this.sessionToken = sessionToken;
        this.sessionMethod = sessionMethod;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.expiresAt = expiresAt;
        this.isValid = isValid;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCredId() {
        return credId;
    }

    public void setCredId(UUID credId) {
        this.credId = credId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getSessionMethod() {
        return sessionMethod;
    }

    public void setSessionMethod(String sessionMethod) {
        this.sessionMethod = sessionMethod;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
