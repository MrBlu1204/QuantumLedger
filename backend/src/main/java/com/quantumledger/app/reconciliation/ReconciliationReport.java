package com.quantumledger.app.reconciliation;

import java.util.List;

public record ReconciliationReport(List<UnbalancedTransaction> unbalancedTransactions,
                                   List<BalanceMismatch> balanceMismatches,
                                   List<RunningBalanceMismatch> runningBalanceMismatches) {

    /** DEBITS - CREDITS for one ledger transaction. != 0 is a bug. */
    public record UnbalancedTransaction(long ledgerTransactionId, long imbalanceMinor){ }

    /** An account whose cached balance differs from what its entries add up to. */
    public record BalanceMismatch(long accountId, long storedMinor, long computedMinor){ }

    public record RunningBalanceMismatch(long ledgerEntryId, long accountId,
                                         long storedMinor, long computedMinor){ }

    public boolean isHealthy() {

        return unbalancedTransactions.isEmpty()
                && balanceMismatches.isEmpty()
                && runningBalanceMismatches.isEmpty();
    }
}
