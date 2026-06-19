DELETE FROM password_reset_token;

ALTER TABLE password_reset_token
    CHANGE COLUMN token token_hash VARCHAR(64) NOT NULL;
