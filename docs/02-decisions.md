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
- Decision: Deploy to AWS right after the MVP to find deployment problems early.