# Decision log

## 001 – Database: MySQL 8.4 (InnoDB)
- Date: 2026-10-09
- Context: Needs ACID transactions, row locking, and AWS RDS support.
- Options: PostgreSQL, MySQL.
- Decision: MySQL, because I already know it and can focus on
  ledger, idempotency and transaction concepts.
- Trade-offs: No partial indexes and non-transactional DDL, so
  migrations need care. Default isolation is Repeatable Read.