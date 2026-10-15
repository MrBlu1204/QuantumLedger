CREATE TABLE ledger_transactions (
                                     ledger_transaction_id   BIGINT       NOT NULL AUTO_INCREMENT,
                                     transaction_ref         VARCHAR(36)  NOT NULL,
                                     transaction_type        VARCHAR(30)  NOT NULL,
                                     reverses_transaction_id BIGINT       NULL,
                                     description             VARCHAR(255) NULL,
                                     create_time             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                     PRIMARY KEY (ledger_transaction_id),
                                     CONSTRAINT unique_ledger_txn_ref      UNIQUE (transaction_ref),
                                     CONSTRAINT unique_ledger_txn_reverses UNIQUE (reverses_transaction_id),
                                     CONSTRAINT fk_ledger_txn_reverses
                                         FOREIGN KEY (reverses_transaction_id)
                                             REFERENCES ledger_transactions (ledger_transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ledger_entries (
                                ledger_entry_id          BIGINT      NOT NULL AUTO_INCREMENT,
                                fk_ledger_transaction_id BIGINT      NOT NULL,
                                fk_account_id            BIGINT      NOT NULL,
                                direction                VARCHAR(6)  NOT NULL,
                                amount_minor             BIGINT      NOT NULL,
                                currency                 VARCHAR(3)  NOT NULL,
                                create_time              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                PRIMARY KEY (ledger_entry_id),
                                CONSTRAINT fk_entries_transaction
                                    FOREIGN KEY (fk_ledger_transaction_id)
                                        REFERENCES ledger_transactions (ledger_transaction_id),
                                CONSTRAINT fk_entries_account
                                    FOREIGN KEY (fk_account_id)
                                        REFERENCES accounts (account_id),
                                CONSTRAINT chk_entries_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
                                CONSTRAINT chk_entries_amount_positive CHECK (amount_minor > 0),
                                INDEX idx_entries_account (fk_account_id, ledger_entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;