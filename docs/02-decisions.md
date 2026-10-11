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