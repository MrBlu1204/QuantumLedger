package com.quantumledger.app.addmoney;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import com.quantumledger.app.payment.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

public class AddMoneyRecorderTest extends AbstractIntegrationTest {

    @Autowired
    AddMoneyRecorder recorder;
    @Autowired
    AddMoneyRequestRepository repository;

    private AddMoneyRequest open(Account wallet, long amountMinor) {
        return recorder.open(wallet.getUserId(), wallet.getAccountId(), amountMinor, PaymentMethod.UPI);
    }

    @Test
    void openSavesAPendingRequestAndMovesNoMoney() {
        Account wallet = newWallet("Asha");

        AddMoneyRequest request = open(wallet, 50_000);

        assertThat(request.getStatus()).isEqualTo(AddMoneyStatus.PENDING);
        assertThat(request.getCurrency()).isEqualTo("INR");
        assertThat(request.getMethod()).isEqualTo(PaymentMethod.UPI);
        assertThat(balanceOf(wallet)).isZero();
        assertThat(countEntries()).isZero();
    }

    @Test
    void settleSuccessCreditsTheWalletAndMarksSuccess() {
        Account wallet = newWallet("Ben");
        Long id = open(wallet, 50_000).getAddMoneyId();

        recorder.settleSuccess(id, "GW-1");

        AddMoneyRequest found = repository.findById(id).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(AddMoneyStatus.SUCCESS);
        assertThat(found.getGatewayRef()).isEqualTo("GW-1");
        assertThat(found.getLedgerTransactionId()).isNotNull();
        assertThat(balanceOf(wallet)).isEqualTo(50_000);
        assertThat(balanceOf(bank())).isEqualTo(50_000);
        assertThat(unbalancedTransactions()).isEmpty();
        assertThat(balanceMismatches()).isEmpty();
    }

    @Test
    void settlingSuccessTwiceCreditsOnlyOnce() {
        Account wallet = newWallet("Chitra");
        Long id = open(wallet, 50_000).getAddMoneyId();

        recorder.settleSuccess(id, "GW-1");
        recorder.settleSuccess(id, "GW-1");

        assertThat(balanceOf(wallet)).isEqualTo(50_000);
        assertThat(countEntries()).isEqualTo(2);
    }

    @Test
    void concurrentSettlementsCreditOnlyOnce() throws Exception {
        Account wallet = newWallet("Dev");
        Long id = open(wallet, 50_000).getAddMoneyId();

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<AddMoneyRequest>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return recorder.settleSuccess(id, "GW-X");
            }));
        }
        start.countDown();
        for (Future<AddMoneyRequest> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(balanceOf(wallet)).isEqualTo(50_000);
        assertThat(countEntries()).isEqualTo(2);
        assertThat(unbalancedTransactions()).isEmpty();
        assertThat(balanceMismatches()).isEmpty();
    }

    @Test
    void settleFailureMarksFailedAndMovesNoMoney() {
        Account wallet = newWallet("Esha");
        Long id = open(wallet, 50_000).getAddMoneyId();

        recorder.settleFailure(id, "GW-2", "DECLINED_BY_BANK");

        AddMoneyRequest found = repository.findById(id).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(AddMoneyStatus.FAILED);
        assertThat(found.getFailureReason()).isEqualTo("DECLINED_BY_BANK");
        assertThat(found.getLedgerTransactionId()).isNull();
        assertThat(balanceOf(wallet)).isZero();
        assertThat(countEntries()).isZero();
    }

    @Test
    void aLateFailureCannotUndoASuccess() {
        Account wallet = newWallet("Farah");
        Long id = open(wallet, 50_000).getAddMoneyId();
        recorder.settleSuccess(id, "GW-1");

        recorder.settleFailure(id, "GW-1", "LATE_CALLBACK");

        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(AddMoneyStatus.SUCCESS);
        assertThat(balanceOf(wallet)).isEqualTo(50_000);
    }
}
