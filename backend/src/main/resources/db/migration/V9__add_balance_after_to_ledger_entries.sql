-- Running balance of the entry's account, right after this entry was applied

ALTER TABLE ledger_entries
    ADD COLUMN balance_after_minor BIGINT NULL;


UPDATE ledger_entries e
JOIN (
        SELECT le.ledger_entry_id,
                SUM(CASE
                        WHEN (a.account_type IN ('WALLET', 'INCOME') AND le.direction = 'CREDIT')
                          OR (a.account_type = 'ASSET' AND le.direction = 'DEBIT')
                        THEN le.amount_minor
                        ELSE -le.amount_minor
                    END) OVER (PARTITION BY le.fk_account_id ORDER BY le.ledger_entry_id) AS running
        FROM ledger_entries le
        JOIN accounts a ON a.account_id = le.fk_account_id
    ) r ON r.ledger_entry_id = e.ledger_entry_id
SET e.balance_after_minor = r.running;


ALTER TABLE ledger_entries
    MODIFY COLUMN balance_after_minor BIGINT NOT NULL;