ALTER TABLE auth_credentials
    ADD COLUMN role user_role NOT NULL DEFAULT 'customer';
