# QuantumLedger
A payment wallet app that can do a lot more than just send money

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


