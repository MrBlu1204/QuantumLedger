package com.quantumledger.app;

import com.quantumledger.app.account.Account;
import com.quantumledger.app.account.AccountRepository;
import com.quantumledger.app.ledger.LedgerTransactionType;
import com.quantumledger.app.ledger.PostingLine;
import com.quantumledger.app.ledger.PostingRequest;
import com.quantumledger.app.posting.PostingResult;
import com.quantumledger.app.posting.PostingService;
import com.quantumledger.app.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("quantumledger_test")
            .withUsername("test")
            .withPassword("test")
            .withCommand("--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci",
                    "--default-time-zone=+00:00");

    static {
        MYSQL.start();     //One container for all the test run
    }

    @DynamicPropertySource
    static void connectionProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        // A throwaway, test-only signing key. It protects nothing.
        registry.add("app.security.jwt.secret", () -> Base64.getEncoder().encodeToString(
                "test-only-signing-key-0123456789!!".getBytes(StandardCharsets.UTF_8)));
    }

    private static final AtomicLong COUNTER = new AtomicLong(1);

    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected AccountRepository accountRepository;
    @Autowired protected PostingService postingService;
    @Autowired protected UserService userService;

    @BeforeEach
    void resetDatabase() {
        jdbc.update("DELETE FROM ledger_entries");
        jdbc.update("DELETE FROM ledger_transactions ORDER BY ledger_transaction_id DESC");
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM accounts where fk_user_id IS NOT NULL");
        jdbc.update("DELETE FROM user_credentials");
        jdbc.update("DELETE FROM users");
        jdbc.update("UPDATE accounts SET balance_minor = 0, status = 'ACTIVE' WHERE fk_user_id IS NULL");
    }

    /** Signs up a real user through the real service, so the wallet event fires too. */
    protected Account newWallet(String firstName){
        long n =  COUNTER.getAndIncrement();

        var user = userService.register("Mr", firstName, "Test",
                firstName.toLowerCase() + n + "@example.com", "+91",
                String.format("9%09d",n), "StrongPass123");

        return accountRepository.findByUserIdAndCurrency(user.getUserId(), "INR").orElseThrow();
    }

    protected Account bank(){
        return accountRepository.findByAccountCode("BANK_SETTLEMENT").orElseThrow();
    }

    protected Account feeIncome(){
        return accountRepository.findByAccountCode("FEE_INCOME").orElseThrow();
    }

    protected void fund(Account wallet, long amountMinor){
        postingService.post(new PostingRequest(LedgerTransactionType.TOP_UP, "test top-up", "INR",
                List.of(PostingLine.debit(bank().getAccountId(),amountMinor),
                        PostingLine.credit(wallet.getAccountId(),amountMinor))));
    }

    /** Reads straight from the DB, never from a possibly stale entity. */
    protected long balanceOf(Account account){
        return jdbc.queryForObject("SELECT balance_minor FROM accounts WHERE account_id = ?",
                Long.class, account.getAccountId());
    }

    protected long countEntries(){
        return jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entries", Long.class);
    }

    /** should always be empty: every ledger transaction balances. */
    protected List<Map<String, Object>> unbalancedTransactions() {
        return jdbc.queryForList("""
                SELECT fk_ledger_transaction_id,
                SUM(CASE direction WHEN 'DEBIT' THEN amount_minor ELSE -amount_minor END) AS imbalance
                FROM ledger_entries
                GROUP BY fk_ledger_transaction_id
                HAVING imbalance <> 0
                """);
    }

    /** should always be empty: every cached balance equals the sum of its entries. */
    protected List<Map<String, Object>> balanceMismatches() {
        return jdbc.queryForList("""
                SELECT a.account_id, a.balance_minor,
                COALESCE(SUM(CASE WHEN e.direction = IF(a.account_type = 'ASSET' , 'DEBIT', 'CREDIT')
                THEN e.amount_minor ELSE -amount_minor END), 0) AS computed
                FROM accounts a
                LEFT JOIN ledger_entries e ON e.fk_account_id = a.account_id
                GROUP BY a.account_id, a.account_type, a.balance_minor
                HAVING a.balance_minor <> computed
                """);
    }
}
