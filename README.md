# QuantumLedger

A UPI-style digital wallet backend: signup, login, balances, transfers and statements.
Built with Spring Boot 4, MySQL, and (later) Redis and Kafka.

## Feature groups and dependencies

### Group A: Core money (everything depends on it)
Signup/login, accounts, balance check, add money, transfer, statement.

### Group B: Making money features useful
Categories, notes and tags on payments, auto pay (scheduled payments).

### Group C: Trust and safety
KYC with tiers, cards, block/report users, fraud rules, support tickets.

### Group D: Social
Messaging between users.

## Folder Structure
```
   QuantumLedger/
   ├── docs/        <- scope, design notes, diagrams
   ├── backend/     <- Spring Boot app (from Day 1)
   ├── infra/       <- docker-compose files (from Day 1)
   ├── frontend/    <- React app (much later)
   └── README.md
```



## Run locally

Prerequisites: JDK 17, Docker Desktop.

1. Start the database:
```
   cd infra
   copy .env.example .env      (then edit the values)
   docker compose up -d
```
2. Generate a signing key and set it as the `JWT_SECRET` environment variable:
```
   $b = New-Object byte[] 32
   [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
   [Convert]::ToBase64String($b)
```
3. Run `QuantumLedgerApplication` (or `./mvnw spring-boot:run` in `backend`).

## Docs

See `docs/` for the scope, the decision log, and the roadmap.


