# Add Money: design (as built)

## Purpose
A user adds money to their own wallet through a payment method (UPI or net banking). The money is credited to the wallet through PostingService only after the payment gateway approves.

## API
`POST /api/v1/accounts/{accountId}/add-money` (Bearer token). The wallet must belong to the token's user.

Request: `{"amountMinor": 50000, "method": "UPI" | "NET_BANKING", "payerRef": "asha@okbank"}`
`payerRef` is a UPI handle or a bank code. It is never stored and never logged.

Receipt (same body for all three outcomes): `{requestRef, status, amountMinor, currency, failureReason}`

| Outcome | HTTP | status |
|---|---|---|
| Approved and credited | 201 | SUCCESS |
| Gateway unavailable, outcome unknown | 202 | PENDING |
| Declined by the gateway | 422 | FAILED |

## Flow
1. Check the amount limits, the active user and wallet ownership. Look up the gateway for the method. Nothing is saved yet.
2. Transaction 1: save the request as PENDING and commit.
3. Call the gateway outside any transaction, exactly once. Never retried.
4. Transaction 2 (own transaction, retried on lock conflicts only): lock the request row, re-check PENDING, then
    - approved: post TOP_UP (debit BANK_SETTLEMENT, credit the wallet) and mark SUCCESS with the ledger transaction id;
    - declined: mark FAILED with the reason.
5. Gateway unavailable, or settlement retries exhausted: the request stays PENDING and a PENDING receipt is returned.

## Components (package addmoney, payment, common)
| Class | Role |
|---|---|
| AddMoneyController | HTTP mapping, JWT user, status code from the receipt |
| AddMoneyService | Orchestrator. Not transactional, refuses to run inside a transaction |
| AddMoneyRecorder | The short transactions: open, settleSuccess, settleFailure |
| AddMoneyRequest | Entity and state machine PENDING to SUCCESS or FAILED, no setters |
| PaymentGateway | Strategy interface |
| FakeUpiGateway, FakeNetBankingGateway | Dev adapters |
| PaymentGatewayFactory | Picks the gateway by PaymentMethod, fails fast on duplicates |
| LockConflictRetry, Sleeper | Shared retry with jittered backoff (also used by TransferFacade) |

Fake gateway outcomes: UPI handle ending `@fail` is declined and `@timeout` is unavailable. Net-banking bank code `DECLINEBANK` is declined and `DOWNBANK` is unavailable. Anything else is approved.

## Data
Table `add_money_requests` (V10, V11): add_money_id, request_ref (unique), fk_user_id, fk_account_id, amount_minor, currency, method, status, gateway_ref, failure_reason, fk_transaction_id (unique, nullable), create_time, update_time.
Database checks: amount greater than 0; status in PENDING, SUCCESS, FAILED; method in UPI, NET_BANKING; fk_transaction_id is set if and only if status is SUCCESS.

## Errors
| Situation | HTTP | Code |
|---|---|---|
| Invalid or missing field, unknown method, malformed JSON, non-numeric accountId | 400 | VALIDATION_FAILED |
| No or invalid token | 401 | (security layer) |
| User not active | 403 | ACCOUNT_NOT_ACTIVE |
| Wallet not yours or unknown | 404 | ACCOUNT_NOT_FOUND |
| Amount outside the limits | 422 | AMOUNT_OUT_OF_RANGE |
| No gateway for the method | 422 | UNSUPPORTED_PAYMENT_METHOD |
| Wallet frozen at settlement (see limitations) | 422 | ACCOUNT_FROZEN |

## Config
`app.add-money.min-amount-minor` 100, `max-amount-minor` 10000000, `retry.max-attempts` 5, `retry.base-delay-ms` 50.

## Safety properties
- Money is credited only after a confirmed approval.
- Row lock plus the PENDING re-check mean concurrent or repeated settlement credits exactly once.
- Lock order is request row first, then accounts in ascending id (PostingService). Nothing locks in the opposite order.
- Only database-only steps are retried. The gateway call is never repeated.

## Tests (49 new, suite 165)
AddMoneyRequestTest 5, AddMoneyRecorderTest 6 (including 8-thread settlement), AddMoneyServiceTest 7, AddMoneyServiceRetryTest 4, AddMoneyFaultInjectionTest 2, AddMoneyHttpTest 9, FakeUpiGatewayTest 4, FakeNetBankingGatewayTest 3, PaymentGatewayFactoryTest 3, PaymentGatewayWiringTest 1, LockConflictRetryTest 5.

## Known limitations (with fixing phase)
| Limitation | Fix |
|---|---|
| A client retry after a lost response credits twice (no Idempotency-Key) | Phase 5 |
| PENDING requests are never resolved automatically (no status-check job or webhook) | Later, with events and ops tooling |
| Money collected but wallet cannot be credited (for example frozen) leaves the request PENDING or returns ACCOUNT_FROZEN: needs refund or manual review | Phase 10 (fraud/support) |
| If settlement gives up, the provider's reference is only in the log; recovery uses requestRef | With the status-check job |
| Every top-up locks the single BANK_SETTLEMENT row, so top-ups serialize | Phase 14 load tests |
| payerRef format is not validated per method; users can enter any handle (collect spam); no saved, verified instruments | Phase 5 rate limiting, Phase 9 |
| Only fake gateways, no real provider; no per-user daily limits | Later, Phase 9 KYC tiers |
| Plain HTTP locally; TLS is required in deployment | Phase 13 |
| Wallet-ownership check duplicated (StatementRepository and AccountRepository) | Small chore |