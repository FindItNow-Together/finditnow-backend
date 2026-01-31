CREATE TABLE IF NOT EXISTS auth_sessions
(
    id
    UUID
    PRIMARY
    KEY,
    cred_id
    UUID
    NOT
    NULL,
    session_token
    TEXT
    UNIQUE
    NOT
    NULL,
    session_method
    VARCHAR
(
    30
),
    ip_address INET,
    user_agent TEXT,
    expires_at TIMESTAMPTZ,
    is_valid BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW
(
),
    revoked_at TIMESTAMPTZ
    );