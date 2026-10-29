package com.quantumledger.app.transfer;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferStatementQueryTest extends AbstractIntegrationTest {

    private static final long FIRST_PAGE = Long.MAX_VALUE;

    @Autowired
    TransferService transferService;
    @Autowired
    TransferRepository transferRepository;

    private String phoneOf(Account wallet) {
        return jdbc.queryForObject("SELECT phone FROM users WHERE user_id = ?",
                String.class, wallet.getUserId());
    }

    private void send(Account from, Account to, long amount) {
        transferService.transfer(
                new TransferService.TransferCommand(from.getUserId(), "+91", phoneOf(to), amount));
    }

    private List<Long> amounts(List<Transfer> page) {
        return page.stream().map(Transfer::getAmountMinor).toList();
    }

    @Test
    void returnsSentAndReceivedNewestFirstAndNothingElse() {
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");
        Account meera = newWallet("Meera");
        fund(asha, 100_000);
        fund(ravi, 100_000);

        send(asha, ravi, 1_000);   // asha sent
        send(ravi, asha, 2_000);   // asha received
        send(asha, meera, 3_000);  // asha sent
        send(ravi, meera, 500);    // does not involve asha

        List<Transfer> page = transferRepository.findStatementPage(asha.getUserId(), FIRST_PAGE, 21);

        assertEquals(List.of(3_000L, 2_000L, 1_000L), amounts(page));
    }

    @Test
    void mergesBothSidesInOrderWhenThePageCutsThroughThem() {
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");
        fund(asha, 100_000);
        fund(ravi, 100_000);

        send(asha, ravi, 1_000);   // sent
        send(ravi, asha, 2_000);   // received
        send(asha, ravi, 3_000);   // sent

        // Sent side alone would give 3000, 1000. The merge must give 3000, 2000.
        List<Transfer> page = transferRepository.findStatementPage(asha.getUserId(), FIRST_PAGE, 2);

        assertEquals(List.of(3_000L, 2_000L), amounts(page));
    }

    @Test
    void walksThroughPagesWithoutGapsOrRepeats() {
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");
        fund(asha, 100_000);
        for (long amount = 100; amount <= 500; amount += 100) {
            send(asha, ravi, amount);
        }

        List<Transfer> first = transferRepository.findStatementPage(asha.getUserId(), FIRST_PAGE, 3);
        assertEquals(List.of(500L, 400L, 300L), amounts(first));

        long cursor = first.get(first.size() - 1).getTransferId();
        List<Transfer> second = transferRepository.findStatementPage(asha.getUserId(), cursor, 3);
        assertEquals(List.of(200L, 100L), amounts(second));
    }

    @Test
    void aUserWithNoTransfersGetsAnEmptyPage() {
        Account asha = newWallet("Asha");

        assertTrue(transferRepository.findStatementPage(asha.getUserId(), FIRST_PAGE, 21).isEmpty());
    }
}
