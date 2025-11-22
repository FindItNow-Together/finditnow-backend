CREATE TABLE public.user_addresses
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    city         VARCHAR(255),
    country      VARCHAR(255),
    full_address TEXT,
    line1        VARCHAR(255),
    line2        VARCHAR(255),
    postal_code  VARCHAR(255),
    state        VARCHAR(255),
    user_id      UUID,

    CONSTRAINT fk_user_addresses_user
        FOREIGN KEY (user_id)
            REFERENCES public.users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_user_addresses_user_id
    ON public.user_addresses (user_id);