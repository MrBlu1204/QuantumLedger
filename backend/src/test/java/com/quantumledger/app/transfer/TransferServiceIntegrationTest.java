package com.quantumledger.app.transfer;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import com.quantumledger.app.account.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TransferService transferService;
    @Autowired
    TransferRepository transferRepository;

    private String phoneOf(Account wallet) {
        return jdbc.queryForObject("SELECT phone FROM users WHERE user_id = ?",
                String.class, wallet.getUserId());
    }

    private TransferService.TransferCommand command(Account from, Account to, long amount) {
        return new TransferService.TransferCommand(from.getUserId(), "+91", phoneOf(to), amount);
    }

    @Test
    void movesMoneyAndRecordsTheTransfer() {
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");
        fund(asha, 100_000);

        TransferService.TransferReceipt receipt =
                transferService.transfer(command(asha, ravi, 25_000));

        assertEquals(75_000, balanceOf(asha));
        assertEquals(25_000, balanceOf(ravi));
        assertEquals("Ravi", receipt.receiverFirstName());
        assertEquals(1, transferRepository.count());

        long linked = jdbc.queryForObject("""
                SELECT COUNT(*) FROM transfers x
                JOIN ledger_transactions t ON t.ledger_transaction_id = x.fk_transaction_id
                WHERE t.transaction_type = 'TRANSFER'
                """, Long.class);
        assertEquals(1, linked);

        assertTrue(unbalancedTransactions().isEmpty());
        assertTrue(balanceMismatches().isEmpty());
    }

    @Test
    void insufficientFundsLeavesNoTransferRecordAndNoMovement() {
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");
        fund(asha, 1_000);
        long entriesBefore = countEntries();

        assertThrows(InsufficientFundsException.class,
                () -> transferService.transfer(command(asha, ravi, 5_000)));

        assertEquals(0, transferRepository.count());
        assertEquals(1_000, balanceOf(asha));
        assertEquals(0, balanceOf(ravi));
        assertEquals(entriesBefore, countEntries());
    }

    @Test
    void anUnknownReceiverIsRejected() {
        Account asha = newWallet("Asha");
        fund(asha, 1_000);

        TransferService.TransferCommand toNobody =
                new TransferService.TransferCommand(asha.getUserId(), "+91", "8000000000", 500);

        assertThrows(ReceiverNotFoundException.class, () -> transferService.transfer(toNobody));
        assertEquals(1_000, balanceOf(asha));
    }

    @Test
    void aBlockedReceiverIsRejectedAndNothingMoves() {
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");
        fund(asha, 1_000);
        jdbc.update("UPDATE users SET status = 'BLOCKED' WHERE user_id = ?", ravi.getUserId());

        TransferRejectedException e = assertThrows(TransferRejectedException.class,
                () -> transferService.transfer(command(asha, ravi, 500)));

        assertEquals(TransferRejection.RECEIVER_UNAVAILABLE, e.getRejection());
        assertEquals(1_000, balanceOf(asha));
        assertEquals(0, balanceOf(ravi));
        assertEquals(0, transferRepository.count());
    }

}
