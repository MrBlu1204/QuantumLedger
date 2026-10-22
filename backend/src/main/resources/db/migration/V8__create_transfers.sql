CREATE TABLE transfers (
    transfer_id                 BIGINT          NOT NULL AUTO_INCREMENT,
    transfer_ref                VARCHAR(36)     NOT NULL,
    fk_sender_id                BIGINT          NOT NULL,
    fk_receiver_id              BIGINT          NOT NULL,
    fk_sender_account_id        BIGINT          NOT NULL,
    fk_receiver_account_id      BIGINT          NOT NULL,
    amount_minor                BIGINT          NOT NULL,
    currency                    VARCHAR(3)      NOT NULL,
    status                      VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED',
    fk_transaction_id           BIGINT          NOT NULL,
    create_time                 DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (transfer_id),
    CONSTRAINT unique_transfers_ref             UNIQUE (transfer_ref),
    CONSTRAINT unique_transfers_txn             UNIQUE (fk_transaction_id),

    CONSTRAINT fk_transfers_sender              FOREIGN KEY (fk_sender_id)          REFERENCES users (user_id),
    CONSTRAINT fk_transfer_receiver             FOREIGN KEY (fk_receiver_id)        REFERENCES users (user_id),
    CONSTRAINT fk_transfers_sender_account      FOREIGN KEY (fk_sender_account_id)  REFERENCES accounts (account_id),
    CONSTRAINT fk_transfers_receiver_account    FOREIGN KEY (fk_receiver_account_id) REFERENCES accounts (account_id),
    CONSTRAINT fk_transfers_ledger_txn          FOREIGN KEY (fk_transaction_id)     REFERENCES ledger_transactions (ledger_transaction_id),

    CONSTRAINT chk_transfers_amt_positive       CHECK ( amount_minor > 0 ),
    CONSTRAINT chk_transfers_not_to_self        CHECK ( fk_sender_id <> fk_receiver_id),
    CONSTRAINT chk_transfers_status             CHECK ( status IN ('COMPLETED', 'REVERSED')),

    INDEX idx_transfers_sender                  (fk_sender_id, transfer_id),
    INDEX idx_transfers_receiver                (fk_receiver_id, transfer_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;