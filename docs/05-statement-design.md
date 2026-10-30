## As built

### API
GET /api/v1/accounts/{accountId}/statement?limit=20&cursor=<opaque>   (Bearer token required)
limit: optional, 1-200, default 20. cursor: optional; omit it for the newest page.
Response 200:
{ "items": [ { "transactionRef", "type": "TRANSFER", "direction": "OUT" | "IN", "amountMinor",
"currency", "balanceAfterMinor", "counterpartyFirstName", "createTime" } ],
"nextCursor": "<opaque>" | null, "hasMore": true | false }
One statement per wallet. The user always comes from the token; only the wallet id is in the URL,
and it is checked against the token's user.

### Data
- A statement line is one ledger entry on the wallet account.
- ledger_entries.balance_after_minor (migration V9): the account's balance right after that entry.
  Written by PostingService while it holds the account lock; V9 backfilled existing rows.
- counterpartyFirstName is filled for transfer lines only (null for add-money, fees, refunds).
- Direction: CREDIT on a wallet is IN, DEBIT is OUT. The API vocabulary is separate from the ledger's.

### Flow (StatementController -> StatementService -> StatementRepository)
1. Service (read-only transaction): validate limit -> decode cursor -> check the wallet belongs
   to the caller -> read limit + 1 rows.
2. Repository: one index range read on idx_entries_account (fk_account_id, ledger_entry_id),
   newest first, entries older than the cursor, LEFT JOINs to transfers and users for the counterparty.
3. The extra row only tells us hasMore (no COUNT(*)). nextCursor is built from the last item shown.
4. Cursor = Base64(ledger_entry_id). Opaque to clients so the internals can change.

### Error semantics
| Situation                                   | HTTP | Code               |
|---------------------------------------------|------|--------------------|
| No or invalid token                         | 401  | (security layer)   |
| limit outside 1-200, or limit/accountId not a number | 400  | VALIDATION_FAILED  |
| Corrupt cursor                              | 400  | INVALID_CURSOR     |
| Wallet is not yours, or does not exist      | 404  | ACCOUNT_NOT_FOUND  |

### Limits
Default page 20, max 200 (constants in StatementService).

### Reconciliation
Beside the unbalanced-transaction and cached-balance checks, the job now recomputes every entry's
running balance with a window function and reports any entry whose stored balance_after_minor differs.

### Known limitations (deliberate, with the phase that fixes each)
- GET /api/v1/accounts/me assumes one wallet. -> becomes a list endpoint when a second wallet is added.
- No date filter, search or export. -> later statement features.
- The running-balance reconciliation scans every entry. -> Phase 14, add an id or time window.
- Statement reads are allowed for BLOCKED users (read-only). -> Phase 9, revisit with KYC.
- No rate limiting on the endpoint. -> Phase 5.
- No total count and no jump-to-page-N (keyset paging trade-off, decision 020).