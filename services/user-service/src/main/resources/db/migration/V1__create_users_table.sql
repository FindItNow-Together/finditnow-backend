CREATE TABLE public.users
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ  NOT NULL,
    email       VARCHAR(255) NOT NULL,
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255),
    phone       VARCHAR(255),
    profile_url VARCHAR(255),
    username    VARCHAR(255)
);

-- Unique constraints (explicitly named for clarity)
ALTER TABLE public.users
    ADD CONSTRAINT uq_users_email UNIQUE (email);

ALTER TABLE public.users
    ADD CONSTRAINT uq_users_phone UNIQUE (phone);

ALTER TABLE public.users
    ADD CONSTRAINT uq_users_username UNIQUE (username);