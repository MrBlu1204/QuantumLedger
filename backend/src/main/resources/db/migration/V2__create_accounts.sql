CREATE TABLE accounts (
    account_id         BIGINT      NOT NULL AUTO_INCREMENT,
    fk_user_id         BIGINT      NOT NULL,
    currency           CHAR(3)     NOT NULL DEFAULT 'INR',
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    create_time        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                   ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (account_id),
    CONSTRAINT unique_accounts_user_currency UNIQUE (fk_user_id, currency),
    CONSTRAINT fk_accounts_user FOREIGN KEY (fk_user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;