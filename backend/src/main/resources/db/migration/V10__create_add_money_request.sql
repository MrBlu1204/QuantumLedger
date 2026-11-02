CREATE TABLE add_money_requests (
        add_money_id        BIGINT          NOT NULL AUTO_INCREMENT,
        request_ref         VARCHAR(36)     NOT NULL,
        fk_user_id          BIGINT          NOT NULL,
        fk_account_id       BIGINT          NOT NULL,
        amount_minor        BIGINT          NOT NULL,
        currency            VARCHAR(3)      NOT NULL,
        status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
        gateway_ref         VARCHAR(64)     NULL,
        failure_reason      VARCHAR(200)    NULL,
        fk_transaction_id   BIGINT          NULL,
        create_time         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
        update_time         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

        PRIMARY KEY (add_money_id),
        CONSTRAINT unique_add_money_ref         UNIQUE (request_ref),
        CONSTRAINT unique_add_money_txn         UNIQUE (fk_transaction_id),

        CONSTRAINT fk_add_money_user            FOREIGN KEY (fk_user_id)        REFERENCES users (user_id),
        CONSTRAINT fk_add_money_account         FOREIGN KEY (fk_account_id)     REFERENCES accounts (account_id),
        CONSTRAINT fk_add_money_ledger_txn      FOREIGN KEY (fk_transaction_id) REFERENCES ledger_transactions (ledger_transaction_id),

        CONSTRAINT chk_add_money_amt_positive   CHECK ( amount_minor > 0 ),
        CONSTRAINT chk_add_money_status         CHECK ( status IN ('PENDING', 'SUCCESS', 'FAILED') ),
        CONSTRAINT chk_add_money_txn_on_success CHECK (
            (status = 'SUCCESS' AND fk_transaction_id IS NOT NULL)
                OR (status <> 'SUCCESS' AND fk_transaction_id IS NULL) ),

        INDEX idx_add_money_user                (fk_user_id, add_money_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;