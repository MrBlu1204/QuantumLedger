ALTER TABLE add_money_requests
    ADD COLUMN method VARCHAR(20) NOT NULL DEFAULT 'UPI' AFTER currency;

ALTER TABLE add_money_requests
    ALTER COLUMN method DROP DEFAULT;

ALTER TABLE add_money_requests
    ADD CONSTRAINT chk_add_money_method CHECK ( method IN ('UPI', 'NET_BANKING') );