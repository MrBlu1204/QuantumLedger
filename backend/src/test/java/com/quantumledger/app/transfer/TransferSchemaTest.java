package com.quantumledger.app.transfer;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferSchemaTest extends AbstractIntegrationTest {

    private int insertTransfer(Account sender, Account receiver, long amount, long ledgerTxnId){
        return jdbc.update("""
                INSERT INTO transfers (transfer_ref, fk_sender_id, fk_receiver_id,
                                       fk_sender_account_id, fk_receiver_account_id,
                                       amount_minor, currency, status, fk_transaction_id)
            VALUES (UUID(), ?, ?, ?, ?, ?, 'INR', 'COMPLETED' , ?)
            """,
        sender.getUserId(), receiver.getUserId(),
                sender.getAccountId(), receiver.getAccountId(), amount, ledgerTxnId);
    }

    private long someLedgerTransactionId(Account wallet){
        fund(wallet, 1_000);
        return jdbc.queryForObject("SELECT MAX(ledger_transaction_id) FROM ledger_transactions", Long.class);
    }

    @Test
    void acceptsAValidTransfer() {
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");
        long txn = someLedgerTransactionId(asha);

        assertEquals(1, insertTransfer(asha, ravi, 500, txn));
    }

    @Test
    void refusesAZeroAmount() {
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");
        long txn = someLedgerTransactionId(asha);

        assertThrows(DataAccessException.class, () -> insertTransfer(asha, ravi, 0, txn));
    }

    @Test
    void refusesATransferToTheSameUser() {
        Account asha = newWallet("Asha");
        long txn = someLedgerTransactionId(asha);

        assertThrows(DataAccessException.class, () -> insertTransfer(asha, asha, 500, txn));
    }

    @Test
    void refusesTwoTransfersForOneLedgerTransaction() {
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");
        long txn = someLedgerTransactionId(asha);
        insertTransfer(asha, ravi, 500, txn);

        assertThrows(DataAccessException.class, () -> insertTransfer(asha, asha, 500, txn));
    }
}
