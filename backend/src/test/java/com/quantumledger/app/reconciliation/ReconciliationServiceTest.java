package com.quantumledger.app.reconciliation;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReconciliationServiceTest extends AbstractIntegrationTest {

    @Test
    void emptySystemIsHealthy(){
        assertTrue(reconciliationService.check().isHealthy());
    }

    @Test
    void systemIsHealthyAfterNormalActivity(){
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");

        fund(asha, 100_000);
        postingService.post(transfer(asha,ravi, 25_000));

        assertTrue(reconciliationService.check().isHealthy());
    }

    @Test
    void detectsCachedBalanceThatDisagreesWithEntries(){
        Account asha = newWallet("Asha");
        fund(asha, 1_000);

        jdbc.update("UPDATE accounts SET balance_minor = 5 WHERE account_id = ?", asha.getAccountId());

        ReconciliationReport report = reconciliationService.check();

        assertFalse(report.isHealthy());
        assertEquals(1, report.balanceMismatches().size());

        ReconciliationReport.BalanceMismatch mismatch = report.balanceMismatches().get(0);
        assertEquals(asha.getAccountId(), mismatch.accountId());
        assertEquals(5, mismatch.storedMinor());
        assertEquals(1_000, mismatch.computedMinor());
        assertTrue(report.unbalancedTransactions().isEmpty());
    }

    @Test
    void detectsUnbalancedLedgerTransactions(){
        // Bypass the posting engine on purpose and write a lopsided transaction by hand.
        jdbc.update("INSERT INTO ledger_transactions (transaction_ref, transaction_type) VALUES (UUID(), 'TEST')");
        Long txnId = jdbc.queryForObject("SELECT MAX(ledger_transaction_id) FROM ledger_transactions", Long.class);
        jdbc.update("INSERT INTO ledger_entries (fk_ledger_transaction_id, fk_account_id, direction, amount_minor, currency) "
                + "VALUES (?, ?, 'DEBIT', 777, 'INR')", txnId, bank().getAccountId());

        ReconciliationReport report = reconciliationService.check();

        assertFalse(report.isHealthy());
        assertEquals(1, report.unbalancedTransactions().size());
        assertEquals(txnId, report.unbalancedTransactions().get(0).ledgerTransactionId());
        assertEquals(777, report.unbalancedTransactions().get(0).imbalanceMinor());

    }
}
