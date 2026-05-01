CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(160) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255),
    failed_login_attempts INT NOT NULL,
    locked_until DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE url_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT,
    original_url VARCHAR(255),
    short_url VARCHAR(32) NOT NULL,
    click_count INT NOT NULL,
    created_at DATETIME(6),
    last_accessed DATETIME(6),
    expires_at DATETIME(6),
    user_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_url_mapping_short_url UNIQUE (short_url),
    CONSTRAINT fk_url_mapping_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_url_mapping_user_created ON url_mapping (user_id, created_at);
CREATE INDEX idx_url_mapping_user_clicks ON url_mapping (user_id, click_count);
CREATE INDEX idx_url_mapping_user_last_accessed ON url_mapping (user_id, last_accessed);
CREATE INDEX idx_url_mapping_user_expires ON url_mapping (user_id, expires_at);
CREATE INDEX idx_url_mapping_short_url ON url_mapping (short_url);
CREATE INDEX idx_url_mapping_original_url ON url_mapping (original_url);

CREATE TABLE click_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    click_time DATETIME(6),
    url_mapping_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_click_event_event_id UNIQUE (event_id),
    CONSTRAINT fk_click_event_url_mapping FOREIGN KEY (url_mapping_id) REFERENCES url_mapping (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_click_event_event_id ON click_event (event_id);
CREATE INDEX idx_click_event_url_mapping_time ON click_event (url_mapping_id, click_time);

CREATE TABLE refresh_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(128) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked BIT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_token UNIQUE (token),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token (expires_at);
CREATE INDEX idx_refresh_token_user_revoked ON refresh_token (user_id, revoked);
CREATE INDEX idx_refresh_token_revoked ON refresh_token (revoked);

CREATE TABLE password_reset_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(128) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used BIT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_token_token UNIQUE (token),
    CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_password_reset_token_user ON password_reset_token (user_id);
CREATE INDEX idx_password_reset_token_expires_at ON password_reset_token (expires_at);
CREATE INDEX idx_password_reset_token_user_used ON password_reset_token (user_id, used);
