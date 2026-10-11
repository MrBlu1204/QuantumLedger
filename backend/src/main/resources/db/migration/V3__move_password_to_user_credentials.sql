CREATE TABLE user_credentials (
            user_id             BIGINT       NOT NULL,
            password_hash       VARCHAR(100) NOT NULL,
            password_changed_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
            PRIMARY KEY (user_id),
            CONSTRAINT fk_credentials_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO user_credentials (user_id, password_hash)
SELECT user_id, password_hash FROM users;

ALTER TABLE users DROP COLUMN password_hash;