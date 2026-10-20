package com.quantumledger.app.posting;

import com.quantumledger.app.AbstractIntegrationTest;
import com.quantumledger.app.account.Account;
import com.quantumledger.app.account.AccountFrozenException;
import com.quantumledger.app.account.InsufficientFundsException;
import com.quantumledger.app.ledger.LedgerTransactionType;
import com.quantumledger.app.ledger.PostingLine;
import com.quantumledger.app.ledger.PostingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PostingIntegrationTest extends AbstractIntegrationTest {

    private PostingRequest transfer(Account from, Account to, long amount) {
        return new PostingRequest(LedgerTransactionType.TRANSFER, "test transfer", "INR",
                List.of(PostingLine.debit(from.getAccountId(), amount),
                        PostingLine.credit(to.getAccountId(), amount)));
    }

    @Test
    void systemAccountsExistAfterMigrations(){
        assertEquals(0, balanceOf(bank()));
        assertEquals(0, balanceOf(feeIncome()));
    }

    @Test
    void topUpCreditsWalletAndDebitsBank(){
        Account asha = newWallet("Asha");

        fund(asha, 100_000);

        assertEquals(100_000, balanceOf(asha));
        assertEquals(100_000, balanceOf(bank()));
        assertEquals(2, countEntries());
        assertTrue(unbalancedTransactions().isEmpty());
        assertTrue(balanceMismatches().isEmpty());
    }

    @Test
    void transferWithFeeMatchesTheWorkedExample(){
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");

        fund(asha, 100_000);

        postingService.post(new PostingRequest(LedgerTransactionType.TRANSFER, "transfer with fee", "INR",
                List.of(PostingLine.debit(asha.getAccountId(), 25_500),
                        PostingLine.credit(ravi.getAccountId(), 25_000),
                        PostingLine.credit(feeIncome().getAccountId(), 500))));

        assertEquals(74_500, balanceOf(asha));
        assertEquals(25_000, balanceOf(ravi));
        assertEquals(500, balanceOf(feeIncome()));
        assertEquals(100_000, balanceOf(bank()));
        assertTrue(unbalancedTransactions().isEmpty());
        assertTrue(balanceMismatches().isEmpty());
    }

    @Test
    void overspendRollsEverythingBack(){

        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");

        fund(asha, 1_000);
        long entriesBefore = countEntries();

        assertThrows(InsufficientFundsException.class, () -> postingService.post(transfer(asha, ravi, 5_000)));

        assertEquals(1_000, balanceOf(asha));
        assertEquals(0, balanceOf(ravi));
        assertEquals(entriesBefore, countEntries());
        assertTrue(balanceMismatches().isEmpty());

    }


    @Test
    void frozenAccountIsRejectedAndNothingChanges(){
        Account asha = newWallet("Asha");
        Account ravi = newWallet("Ravi");

        fund(asha, 10_000);

        jdbc.update("UPDATE accounts SET status = 'FROZEN' where account_id = ?", asha.getAccountId());

        long entriesBefore = countEntries();

        assertThrows(AccountFrozenException.class, () -> postingService.post(transfer(asha, ravi, 1_000)));

        assertEquals(10_000, balanceOf(asha));
        assertEquals(entriesBefore, countEntries());
    }


    @Test
    void databaseRefusesNegativeBalanceEvenIfCodeIsBypassed(){
        Account asha = newWallet("Asha");

        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE accounts SET balance_minor = -1 WHERE account_id = ?", asha.getAccountId()));
    }
}
