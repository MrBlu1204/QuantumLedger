package com.quantumledger.app.transfer;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferRetryIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void shortLockTimeout(DynamicPropertyRegistry registry) {
        // Each pooled connection gives up waiting for a row lock after 2 seconds.
        registry.add("spring.datasource.hikari.connection-init-sql",
                () -> "SET SESSION innodb_lock_wait_timeout = 2");
    }

    @Autowired
    TransferFacade transferFacade;

    @Autowired
    PlatformTransactionManager transactionManager;

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void retriesAfterARealLockTimeoutAndSucceedsOnceTheLockIsReleased() throws Exception {
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");
        fund(asha, 100_000);
        String raviPhone = jdbc.queryForObject(
                "SELECT phone FROM users WHERE user_id = ?", String.class, ravi.getUserId());

        CountDownLatch lockHeld = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> holder = pool.submit(() ->
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    jdbc.queryForObject(
                            "SELECT balance_minor FROM accounts WHERE account_id = ? FOR UPDATE",
                            Long.class, asha.getAccountId());
                    lockHeld.countDown();
                    sleepQuietly(3_000);                      // hold the lock for 3 seconds
                }));

        assertTrue(lockHeld.await(10, TimeUnit.SECONDS), "helper never took the lock");

        long started = System.nanoTime();
        TransferService.TransferReceipt receipt = transferFacade.transfer(
                new TransferService.TransferCommand(asha.getUserId(), "+91", raviPhone, 10_000));
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;

        holder.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals("Ravi", receipt.receiverFirstName());
        assertEquals(90_000, balanceOf(asha));
        assertEquals(10_000, balanceOf(ravi));
        // Without a retry the call would have failed after ~2s. Succeeding means it waited, retried, and won.
        assertTrue(elapsedMs >= 1_800, "expected to wait for the lock timeout first, but took " + elapsedMs + " ms");
        assertTrue(unbalancedTransactions().isEmpty());
        assertTrue(balanceMismatches().isEmpty());
    }
}
