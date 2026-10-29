# Decision log

## 001 – Database: MySQL 8.4 (InnoDB)
- Date: 2026-10-09
- Context: Needs ACID transactions, row locking, and AWS RDS support.
- Options: PostgreSQL, MySQL.
- Decision: MySQL, because I already know it and can focus on
  ledger, idempotency and transaction concepts.
- Trade-offs: No partial indexes and non-transactional DDL, so
  migrations need care. Default isolation is Repeatable Read.

## 002 – Schema changes via Flyway migrations
- Date: 2026-10-09
- Decision: All schema changes are versioned SQL files. Applied migrations are never edited.
  Hibernate ddl-auto is "validate" only.

## 003 – Deploy a small MVP version at the end of phase 5
- Date: 2026-10-09
- Decision: Deploy to AWS right after the MVP to find deployment problems early.

## 004 – Sensitive data isolation
- Date: 2026-10-10
- Decision: Separate schemas and per-module DB users now; secrets in a vault, not a database;
  field-level encryption for KYC data later. Truly separate database only for KYC documents.
- Reason: A second database for passwords would break atomic signup and add little protection
  if the app can read both. Least privilege and a vault give most of the benefit.

## 005 – User lifecycle: status vs is_deleted
- Date: 2026-10-10
- status = lifecycle: ACTIVE, CLOSED (user can self-recover), BLOCKED (support only).
- is_deleted = soft delete for audit; only the bank can restore, with the user's consent.
- Users are never hard-deleted. Email and phone stay reserved after deletion.
- full_name = first +  last, built in exactly one place (the User entity).

## 006 – Credentials in a separate table
- Date: 2026-10-11
- Decision: Password hashes live in user_credentials (one row per user), created by migration V3.
  Signup writes users and user_credentials in one transaction.
- Plan: move user_credentials to its own schema with a separate DB user in phase 5.5.
  Secrets go in a vault, not a database.

## 007 – Authentication and sessions
- Access token: JWT, HS256, 15 minutes, built with Spring Security's Nimbus support.
  Claims: iss, sub (user id), iat, exp, jti. No personal data in the token.
- Refresh token: opaque 256-bit random value, 7 days. Only its SHA-256 hash is stored.
  Rotated on every use. Each login starts a token family; replaying a used token revokes
  the whole family.
- The signing secret comes from the JWT_SECRET environment variable and has no default.
- Endpoints that matter re-check the user's status in the database, because a signed
  token can't be cancelled before it expires.
- Public paths are listed explicitly; everything else requires a token (deny by default).
- Known limitations, deliberately deferred: no instant access-token revocation (Redis
  deny-list on jti, phase 5), no failed-login limiting (Redis rate limiter, phase 5),
  symmetric signing key (move to asymmetric keys and a vault, phase 5.5), no grace window
  for refresh retries (phase 4).

## 008 – Money as integer minor units
- All amounts are BIGINT paise. APIs return paise (balanceMinor); clients format.
- Reason: floating point can't represent most decimals, and ledger checks need exact equality.

## 009 – Immutable double-entry ledger
- Tables: ledger_transactions, ledger_entries. Entries are positive amounts plus a direction.
- Accounts table holds user wallets and platform accounts (BANK_SETTLEMENT, FEE_INCOME),
  seeded by migration V5. A CHECK ties account type to ownership.
- A user's external bank is transaction metadata, not a ledger account.
- Balance is derived from entries; a cached balance arrives with the posting engine.
- Reversals are new transactions that reference the original (at most one, by UNIQUE constraint).

## 010 – Wallet creation via a synchronous application event
- UserService publishes UserRegisteredEvent; the account module listens and creates the wallet
  inside the signup transaction.
- Reason: one-way dependency (account depends on user), easy to extend, and no wallet-less users.
- Cost: the flow is less visible when reading UserService. Post-commit reactions (emails)
  will use a different listener type later.

## 011 – One posting engine, in its own package
- PostingService (package posting) is the only writer of ledger entries. posting depends on
  account and ledger; they do not depend on it, so there are no circular dependencies.

## 012 – Cached balance, pessimistic locks
- accounts.balance_minor is a cache with CHECK >= 0, updated in the same transaction as the entries.
- Pessimistic locking (SELECT ... FOR UPDATE), not optimistic: wallets are the most
  correctness-critical rows and contention on them is expected.
- Drift is detected by a reconciliation job; it is never auto-repaired.

## 013 – Tests run against a real MySQL
- Testcontainers (mysql:8.4), one container per test run, tests are not @Transactional.
- Concurrency tests use a start gate and exact expected outcomes. Experiments that break the
  lock and the lock order were run to confirm the tests can fail.

## 014 – A transfer is a business record plus a ledger posting, in one transaction
- transfers table (who paid whom, status) is separate from the immutable ledger.
- One UNIQUE link per ledger transaction. If either part fails, neither is saved.

## 015 – Retries only for failures that guarantee a rollback
- TransferFacade retries deadlocks and lock timeouts (allow-list), outside the transaction,
  with exponential backoff and full jitter. Business rejections and unknown outcomes are never retried.
- The facade refuses to run inside a transaction.

## 016 – HTTP error semantics
- 400 malformed, 422 business rule, 404 receiver unknown, 403 inactive sender, 503 busy (+ Retry-After).
- Error bodies carry a stable machine-readable code.

## 017 – Column naming conventions
- fk_<role>_id references users (fk_sender_id); fk_<role>_account_id references accounts;
  fk_transaction_id (transfers) references ledger_transactions.
- Migrations V1 to V8 are never edited; older ledger tables keep fk_ledger_transaction_id.

## 018 – Never load a row into the persistence context before locking it
- A pessimistic lock query returns the already-loaded (possibly stale) entity instead of refreshing it.
- TransferService therefore resolves wallets to ids only; PostingService is the only code that loads
  and locks Account entities.
- Regression tests: TransferConcurrencyTest (service level) and the parallel tests in TransferHttpTest.

## 019 — Statements use keyset pagination on transfer_id
- Decision: GET /api/v1/transfers returns newest-first pages using a cursor (last transfer_id, Base64), not page numbers.
- Why: OFFSET gets slower with depth and shifts when new rows arrive; a cursor stays fast and stable. No COUNT(*) needed: fetch limit+1 rows to compute hasMore.
- Query: two index-ordered queries (sender side, receiver side) combined with UNION ALL, top limit+1. Safe from duplicates because of chk_transfers_not_to_self. Uses existing idx_transfers_sender/receiver (fk_*_id, transfer_id); no V9 migration needed.
- Contract: limit 1–200 (default 20), invalid -> 400; bad cursor -> 400 INVALID_CURSOR; only the caller's own transfers; counterparty exposed as first name only.
- Trade-off: no total count and no jumping to page N. Acceptable for a statement feed.

## 020 — Statements are built from ledger entries with a stored running balance
- Decision: a statement line = one ledger entry on the user's wallet account; ledger_entries.balance_after_minor is written by PostingService under the account lock.
- Why: shows balance per line; covers add-money, fees and refunds automatically (not only transfers); paging is a single-account index read. Computing running sums at read time gets slower with history.
- Supersedes the transfers-based query in 019 (cursor, limit rules and response shape still apply; cursor becomes ledger_entry_id).
- Trade-off: one more column to keep correct; reconciliation also verifies it.