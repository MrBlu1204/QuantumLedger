package com.quantumledger.app.statement;

import java.time.Instant;

/** One line of a user's statement: one ledger entry on their wallet. */
public record StatementEntry(
        String transactionRef,
        String type,
        StatementDirection direction,
        long amountMinor,
        String currency,
        long balanceAfterMinor,
        String counterpartyFirstName,       // null unless the line is a transfer
        Instant createTime
) {
}
