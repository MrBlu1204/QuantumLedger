package com.quantumledger.app.statement;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.util.List;

@Repository
public class StatementRepository {

    /** A statement line plus the entry id the cursor is built from (never sent to clients). */
    public record StatementRow(long entryId, StatementEntry entry) { }

    private static final String PAGE = """
            SELECT e.ledger_entry_id, t.transaction_ref, t.transaction_type, e.direction,
                   e.amount_minor, e.currency, e.balance_after_minor, e.create_time,
                   u.first_name AS counterparty_first_name
            FROM ledger_entries e
            JOIN ledger_transactions t ON t.ledger_transaction_id = e.fk_ledger_transaction_id
            LEFT JOIN transfers x ON x.fk_transaction_id = t.ledger_transaction_id
            LEFT JOIN users u ON u.user_id =
                 CASE WHEN x.fk_sender_id = ? THEN x.fk_receiver_id ELSE x.fk_sender_id END
            WHERE e.fk_account_id = ? AND e.ledger_entry_id < ?
            ORDER BY e.ledger_entry_id DESC
            LIMIT ?
            """;

    private static final String OWNS_WALLET = """
            SELECT COUNT(*) FROM accounts
            WHERE account_id = ? AND fk_user_id = ? AND account_type = 'WALLET'
            """;

    private final JdbcTemplate jdbc;

    public StatementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Entries of one wallet account, newest first, strictly older than the cursor entry id. */
    public List<StatementRow> findPage(long userId, long walletAccountId, long before, int fetch){
        return jdbc.query(PAGE, (rs, i) -> new StatementRow(
                rs.getLong("ledger_entry_id"),
                new StatementEntry(
                        rs.getString("transaction_ref"),
                        rs.getString("transaction_type"),
                        "CREDIT".equals(rs.getString("direction"))
                                ? StatementDirection.IN : StatementDirection.OUT,
                        rs.getLong("amount_minor"),
                        rs.getString("currency"),
                        rs.getLong("balance_after_minor"),
                        rs.getString("counterparty_first_name"),
                        rs.getObject("create_time", java.time.LocalDateTime.class)
                                .toInstant(ZoneOffset.UTC)
                )
        ), userId, walletAccountId, before, fetch);
    }

    public boolean ownsWallet(long userId, long accountId){
        Long count = jdbc.queryForObject(OWNS_WALLET, Long.class, accountId, userId);
        return count != null && count > 0;
    }
}
