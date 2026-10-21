package com.quantumledger.app.reconciliation;

import com.quantumledger.app.reconciliation.ReconciliationReport.UnbalancedTransaction;
import com.quantumledger.app.reconciliation.ReconciliationReport.BalanceMismatch;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReconciliationService {

    private static final String UNBALANCED_TRANSACTIONS = """
            SELECT fk_ledger_transaction_id AS ledger_transaction_id,
                   SUM(CASE direction WHEN 'DEBIT' THEN amount_minor ELSE -amount_minor END) AS imbalance
            FROM ledger_entries
            GROUP BY fk_ledger_transaction_id
            HAVING imbalance <> 0
            ORDER BY fk_ledger_transaction_id
            """;

    private static final String BALANCE_MISMATCHES = """
                 SELECT a.account_id, a.balance_minor AS stored_balance,
                        COALESCE(SUM(CASE WHEN e.direction = IF(a.account_type = 'ASSET', 'DEBIT', 'CREDIT')
                                          THEN e.amount_minor ELSE -e.amount_minor END), 0) AS computed_balance
                 FROM accounts a
                 LEFT JOIN ledger_entries e ON e.fk_account_id = a.account_id
                 GROUP BY a.account_id, a.account_type, a.balance_minor
                 HAVING stored_balance <> computed_balance
                 ORDER BY a.account_id
           """;

    private final JdbcTemplate jdbc;

    public ReconciliationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** both queries run in a single transactions, so they see the same consistent snapshot. */
    @Transactional(readOnly = true)
    public ReconciliationReport check(){
        List<UnbalancedTransaction> unbalanced = jdbc.query(UNBALANCED_TRANSACTIONS,
                (rs, i) -> new UnbalancedTransaction(
                        rs.getLong("ledger_transaction_id"), rs.getLong("imbalance")
                ));

        List<BalanceMismatch> mismatches = jdbc.query(BALANCE_MISMATCHES,
                (rs, i) -> new BalanceMismatch(
                        rs.getLong("account_id"),
                        rs.getLong("stored_balance"),
                        rs.getLong("computed_balance")
                ));

        return new ReconciliationReport(unbalanced, mismatches);
    }
}
