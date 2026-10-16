ALTER TABLE accounts
    ADD COLUMN balance_minor BIGINT NOT NULL DEFAULT 0 AFTER currency;

UPDATE accounts a
    SET a.balance_minor = COALESCE((
        SELECT SUM(CASE WHEN e.direction = IF(a.account_type = 'ASSET', 'DEBIT', 'CREDIT')
                        THEN e.amount_minor ELSE -e.amount_minor
                        END)
        FROM ledger_entries e
        WHERE e.fk_account_id = a.account_id),0);

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_balance_non_negative CHECK ( balance_minor >= 0 );

