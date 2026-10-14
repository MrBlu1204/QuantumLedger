CREATE TABLE refresh_tokens (
    refresh_token_id            BIGINT          NOT NULL AUTO_INCREMENT,
    fk_user_id                  BIGINT          NOT NULL ,
    token_hash                  VARCHAR(64)     NOT NULL ,
    family_id                   VARCHAR(36)     NOT NULL ,
    status                      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    expires_at                  DATETIME(6)     NOT NULL ,
    used_at                     DATETIME(6),
    create_time                 DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (refresh_token_id),
    CONSTRAINT unique_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_user  FOREIGN KEY (fk_user_id) REFERENCES users (user_id),
    INDEX idx_refresh_family (family_id),
    INDEX idx_refresh_user (fk_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;
