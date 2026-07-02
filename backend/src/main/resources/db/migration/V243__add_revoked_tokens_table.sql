CREATE TABLE revoked_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    jti         VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_revoked_tokens_jti        ON revoked_tokens(jti);
CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens(expires_at);
