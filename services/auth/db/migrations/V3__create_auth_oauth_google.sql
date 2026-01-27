CREATE TABLE IF NOT EXISTS auth_oauth_google (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,
    google_user_id TEXT NOT NULL UNIQUE,
    email TEXT,

    created_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT fk_oauth_user
        FOREIGN KEY (user_id)
        REFERENCES auth_credentials (user_id)
        ON DELETE CASCADE
);
