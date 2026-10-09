# QuantumLedger

### What can a user do (version 1)?
```
Sign up and log in
Get a wallet account
Add money to the wallet
Transfer money to another user
View a statement (history of money in and out)
```
### What is explicitly NOT in version 1?
```
Real bank connections, KYC, refunds, multiple currencies, a mobile app. 
```

### What must never go wrong? 
```
Money is never created or destroyed by a bug (total money in the system stays constant).
A balance never goes negative.
If a user’s phone retries a request, they are not charged twice.
```


## Feature groups and dependencies

### Group A: Core money (everything depends on it)
Signup/login, accounts, balance check, add money, transfer, statement.

### Group B: Making money features useful
Categories, notes and tags on payments, auto-pay (scheduled payments).

### Group C: Trust and safety
KYC with tiers, cards, block/report users, fraud rules, support tickets.

### Group D: Social
Messaging between users.


## Tools & Technologies
* Java 17 (JDK)
* IntelliJ IDEA
* Maven
* Git + GitHub
* Docker Desktop
* Postman
* DBeaver
* SpringBoot
* PostgreSQL
* Redis
* Kafka
* Flyway
* React
* AWS (EC2/ECS + RDS)
* Prometheus + Grafana
* k6 or JMeter