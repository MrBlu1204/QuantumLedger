package com.quantumledger.app.posting;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import com.quantumledger.app.account.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostingConcurrencyTest extends AbstractIntegrationTest {

    private static final int TIMEOUT_SECONDS = 60;

    private record Outcome(int succeeded, int insuffiecient, List<Throwable> unexpected) { }

    /** Runs every request on a new thread, all released at the same instant. */
    private Outcome runConcurrently(int threads, List<PostingRequest> requests) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger insuffiecient = new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        try{
            for (PostingRequest request : requests) {
                futures.add(pool.submit(() -> {
                    try {
                        startGate.await();
                        postingService.post(request);
                        succeeded.incrementAndGet();
                    } catch (InsufficientFundsException e){
                        insuffiecient.incrementAndGet();
                    }catch (Throwable t){
                        unexpected.add(t);
                    }
                }));
            }
            startGate.countDown();
            for(Future<?> future : futures){
                future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        return new Outcome(succeeded.get(), insuffiecient.get(), unexpected);
    }


    @Test
    void fiftyThreadsSpendingFromOneWalletNeverOverspend() throws Exception {
        Account asha = newWallet("asha");
        Account ravi = newWallet("ravi");

        fund(asha, 10_000);

        List<PostingRequest> requests = new ArrayList<>();

        for(int i = 0; i < 50; i++){
            requests.add(transfer(asha, ravi, 500));
        }

        Outcome outcome = runConcurrently(50, requests);

        assertTrue(outcome.unexpected().isEmpty(), "unexpected failures: " + outcome.unexpected());
        assertEquals(20, outcome.succeeded());
        assertEquals(30, outcome.insuffiecient());
        assertEquals(0, balanceOf(asha));
        assertEquals(10_000, balanceOf(ravi));
        assertEquals(2 + 20 * 2, countEntries());
        assertTrue(unbalancedTransactions().isEmpty());
        assertTrue(balanceMismatches().isEmpty());
    }


    @Test
    void oppositeTransfersDoNotDeadlock() throws Exception {
        Account asha = newWallet("asha");
        Account ravi = newWallet("ravi");

        fund(asha, 100_000);
        fund(ravi, 100_000);

        List<PostingRequest> requests = new ArrayList<>();
        for(int i = 0; i < 25; i++){
            requests.add(transfer(asha, ravi, 100));
            requests.add(transfer(ravi, asha, 100));
        }

        Outcome outcome = runConcurrently(50, requests);

        assertTrue(outcome.unexpected().isEmpty(), "unexpected failures: " + outcome.unexpected());
        assertEquals(50, outcome.succeeded());
        assertEquals(0, outcome.insuffiecient());
        assertEquals(100_000, balanceOf(asha));
        assertEquals(100_000, balanceOf(ravi));
        assertTrue(unbalancedTransactions().isEmpty());
        assertTrue(balanceMismatches().isEmpty());
    }

    @Test
    void randomTransfersNeverCreateOrDestroyMoney() throws Exception {

        List<Account> wallets = new ArrayList<>();

        for(String name : List.of("Anil", "Bina", "Chitra", "Dev", "Esha")){
            Account wallet = newWallet(name);
            fund(wallet, 5_000);
            wallets.add(wallet);
        }

        List<PostingRequest> requests = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for(int i = 0; i < 200; i++){
            int from = random.nextInt(wallets.size());
            int to = (from + 1 + random.nextInt(wallets.size()-1)) % wallets.size();
            requests.add(transfer(wallets.get(from), wallets.get(to), 1 + random.nextInt(800)));
        }

        Outcome outcome = runConcurrently(20, requests);

        assertTrue(outcome.unexpected().isEmpty(), "unexpected failures: " + outcome.unexpected());
        assertEquals(200, outcome.succeeded() + outcome.insuffiecient());

        long totalInWallets = wallets.stream().mapToLong(this :: balanceOf).sum();

        assertEquals(25_000, totalInWallets);
        assertEquals(25_000,   balanceOf(bank()));
        assertTrue(unbalancedTransactions().isEmpty());
        assertTrue(balanceMismatches().isEmpty());
    }

}
