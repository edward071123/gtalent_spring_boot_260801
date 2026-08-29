CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_tokens_token_hash (token_hash),
    KEY idx_password_reset_tokens_member (member_id),
    KEY idx_password_reset_tokens_expires_at (expires_at)
);