## As built

### API
POST /api/v1/transfers   (Bearer token required)
Request:  { "receiverPhone": "9123456780", "receiverCountryCode": "+91", "amountMinor": 25000 }
Response 201: { "transferRef", "status": "COMPLETED", "amountMinor", "currency",
"receiver": { "firstName" }, "createTime" }
The sender is always taken from the token. There is no senderId in the request.

### Flow (TransferController -> TransferFacade -> TransferService -> PostingService)
1. Facade: refuses to run inside a transaction; retries lock conflicts (max 3, backoff + jitter).
2. Service (one transaction): validate amount range -> load active sender -> find receiver by phone
   -> reject self/blocked/deleted receiver -> load both wallets -> post to the ledger
   -> save the transfers row.
3. PostingService: lock accounts in ascending id order, apply balances, insert ledger rows.

### Error semantics
| Situation                         | HTTP | Code                          |
|-----------------------------------|------|-------------------------------|
| No or invalid token               | 401  | (security layer)              |
| Malformed body or field           | 400  | VALIDATION_FAILED             |
| Amount below min or above max     | 422  | AMOUNT_OUT_OF_RANGE           |
| Receiver not registered           | 404  | RECEIVER_NOT_FOUND            |
| Receiver blocked or deleted       | 422  | RECEIVER_UNAVAILABLE          |
| Sending to yourself               | 422  | CANNOT_TRANSFER_TO_SELF       |
| Sender not active                 | 403  | ACCOUNT_NOT_ACTIVE            |
| Not enough money                  | 422  | INSUFFICIENT_FUNDS            |
| Lock conflict persists            | 503  | TEMPORARILY_UNAVAILABLE (Retry-After: 1) |

### Limits (app.transfer.*)
min 100 paise, max 10,000,000 paise, retry 3 attempts, base delay 50 ms.

### Known limitations (deliberate, with the phase that fixes each)
- Not idempotent: a client retry after a lost response pays twice. -> Phase 5, Idempotency-Key.
- Failed attempts are not stored. -> Phase 10, audit and fraud tables.
- Receiver's user status is checked before locking; account status is checked under the lock.
  Blocking a user will also freeze their accounts in one transaction. -> Phase 9.
- RECEIVER_NOT_FOUND reveals whether a phone number is registered. -> Phase 5, rate limiting.
- No per-user daily limits. -> Phase 9, KYC tiers.
- No fee on transfers yet (FEE_INCOME exists and is ready).