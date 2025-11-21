CREATE TABLE IF NOT EXISTS auth_oauth_google (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    google_user_id TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL,
    access_token TEXT,
    refresh_token TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    last_login TIMESTAMPTZ
);
