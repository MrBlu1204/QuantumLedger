package com.quantumledger.app.statement;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import com.quantumledger.app.transfer.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatementRepositoryTest extends AbstractIntegrationTest {

    private static final long FIRST_PAGE = Long.MAX_VALUE;

    @Autowired
    StatementRepository statementRepository;
    @Autowired
    TransferService transferService;

    private String phoneOf(Account wallet) {
        return jdbc.queryForObject("SELECT phone FROM users WHERE user_id = ?",
                String.class, wallet.getUserId());
    }

    private void send(Account from, Account to, long amount) {
        transferService.transfer(
                new TransferService.TransferCommand(from.getUserId(), "+91", phoneOf(to), amount));
    }

    private List<StatementRepository.StatementRow> page(Account owner, long before, int fetch) {
        return statementRepository.findPage(owner.getUserId(), owner.getAccountId(), before, fetch);
    }

    @Test
    void showsDirectionCounterpartyAndRunningBalancePerLine() {
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");
        fund(asha, 100_000);
        send(asha, ravi, 25_000);
        send(ravi, asha, 5_000);

        List<StatementRepository.StatementRow> rows = page(asha, FIRST_PAGE, 21);

        assertEquals(3, rows.size());

        StatementEntry received = rows.get(0).entry();
        assertEquals(StatementDirection.IN, received.direction());
        assertEquals(5_000, received.amountMinor());
        assertEquals(80_000, received.balanceAfterMinor());
        assertEquals("Ravi", received.counterpartyFirstName());
        assertEquals("TRANSFER", received.type());

        StatementEntry sent = rows.get(1).entry();
        assertEquals(StatementDirection.OUT, sent.direction());
        assertEquals(25_000, sent.amountMinor());
        assertEquals(75_000, sent.balanceAfterMinor());
        assertEquals("Ravi", sent.counterpartyFirstName());

        StatementEntry funding = rows.get(2).entry();
        assertEquals(StatementDirection.IN, funding.direction());
        assertEquals(100_000, funding.balanceAfterMinor());
        assertNull(funding.counterpartyFirstName());
    }

    @Test
    void pagesWithTheCursorWithoutGapsOrRepeats() {
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");
        fund(asha, 100_000);
        for (long amount = 100; amount <= 500; amount += 100) {
            send(asha, ravi, amount);
        }

        List<StatementRepository.StatementRow> first = page(asha, FIRST_PAGE, 3);
        assertEquals(List.of(500L, 400L, 300L),
                first.stream().map(r -> r.entry().amountMinor()).toList());

        long cursor = first.get(first.size() - 1).entryId();
        List<StatementRepository.StatementRow> second = page(asha, cursor, 3);
        assertEquals(List.of(200L, 100L, 100_000L),
                second.stream().map(r -> r.entry().amountMinor()).toList());
    }

    @Test
    void aWalletWithNoEntriesGetsAnEmptyPage() {
        Account asha = newWallet("Asha");

        assertTrue(page(asha, FIRST_PAGE, 21).isEmpty());
    }

    @Test
    void ownsWalletIsTrueOnlyForTheWalletsOwner(){
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");

        assertTrue(statementRepository.ownsWallet(asha.getUserId(), asha.getAccountId()));
        assertFalse(statementRepository.ownsWallet(asha.getUserId(), ravi.getAccountId()));
        assertFalse(statementRepository.ownsWallet(asha.getUserId(), 999_999L));
    }


}
