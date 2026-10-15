ALTER TABLE accounts
    MODIFY COLUMN fk_user_id    BIGINT NULL,
    MODIFY COLUMN currency      VARCHAR(3) NOT NULL DEFAULT 'INR',
    ADD    COLUMN account_type  VARCHAR(20) NOT NULL DEFAULT 'WALLET' AFTER fk_user_id,
    ADD    COLUMN account_code  VARCHAR(50) NULL AFTER account_type,
    ADD    CONSTRAINT unique_accounts_code UNIQUE (account_code),
    ADD    CONSTRAINT chk_accounts_type
                CHECK ( account_type IN ('WALLET', 'ASSET', 'INCOME') ),
    ADD    CONSTRAINT chk_accounts_owner
                CHECK (
                    (account_type = 'WALLET' AND fk_user_id IS NOT NULL AND account_code IS NULL)
                    OR (account_type <> 'WALLET' AND fk_user_id IS NULL AND account_code IS NOT NULL)
                    );

INSERT INTO accounts (fk_user_id, account_type, account_code, currency) VALUES
                    (NULL, 'ASSET', 'BANK_SETTLEMENT', 'INR'),
                    (NULL, 'INCOME', 'FEE_INCOME', 'INR');

INSERT INTO accounts (fk_user_id,  account_type, currency)
SELECT u.user_id, 'WALLET', 'INR'
       FROM users u
       WHERE NOT EXISTS(
           SELECT 1 FROM accounts a
                    WHERE a.fk_user_id = u.user_id AND a.currency = 'INR'
       );
