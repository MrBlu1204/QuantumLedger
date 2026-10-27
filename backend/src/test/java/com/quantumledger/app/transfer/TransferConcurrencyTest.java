package com.quantumledger.app.transfer;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import com.quantumledger.app.account.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferConcurrencyTest extends AbstractIntegrationTest {

    @Autowired TransferFacade transferFacade;

    @Test
    void parallelTransfersFromOneWalletNeverOverspend() throws Exception {
        Account asha = newWallet("asha");
        Account ravi = newWallet("ravi");
        fund(asha, 10_000);

        String raviPhone = jdbc.queryForObject(
                "SELECT phone FROM users WHERE user_id = ?", String.class, ravi.getUserId()
        );

        ExecutorService pool = Executors.newFixedThreadPool(30);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger insufficient =  new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        try{
            for(int i = 0; i < 30; i++){
                futures.add(pool.submit(() -> {
                    try {
                        startGate.await();
                        transferFacade.transfer(new TransferService.TransferCommand(
                                asha.getUserId(), "+91", raviPhone, 500
                        ));
                        succeeded.incrementAndGet();
                    } catch (InsufficientFundsException e) {
                        insufficient.incrementAndGet();
                    } catch (Throwable t){
                        unexpected.add(t);
                    }
                }));
            }
            startGate.countDown();
            for(Future<?> f : futures){
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdown();
        }

        assertTrue(unexpected.isEmpty(), "unexpected failures: " + unexpected);
        assertEquals(20, succeeded.get());
        assertEquals(10, insufficient.get());
        assertEquals(0, balanceOf(asha));
        assertEquals(10_000, balanceOf(ravi));
        assertEquals(20L, jdbc.queryForObject("SELECT COUNT(*) FROM transfers", Long.class));
        assertTrue(unbalancedTransactions().isEmpty());
        assertTrue(balanceMismatches().isEmpty());

    }
}
