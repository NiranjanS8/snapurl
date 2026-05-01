CREATE INDEX idx_refresh_token_token_revoked_expires
    ON refresh_token (token, revoked, expires_at);
