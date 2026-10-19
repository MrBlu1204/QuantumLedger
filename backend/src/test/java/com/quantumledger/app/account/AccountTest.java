package com.quantumledger.app.account;

import com.quantumledger.app.ledger.Direction;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountTest {

    private Account wallet(long balance) {
        Account a = Account.wallet(1L, "INR");
        ReflectionTestUtils.setField(a, "balanceMinor", balance);
        return a;
    }

    @Test
    void creditIncreasesWalletBalance() {
        Account a = wallet(1_000);
        a.applyEntry(Direction.CREDIT, 500);
        assertEquals(1_500, a.getBalanceMinor());
    }

    @Test
    void debitDecreasesWalletBalance() {
        Account a = wallet(1_000);
        a.applyEntry(Direction.DEBIT, 400);
        assertEquals(600, a.getBalanceMinor());
    }

    @Test
    void debitCanTakeBalanceToExactlyZero() {
        Account a = wallet(1_000);
        a.applyEntry(Direction.DEBIT, 1_000);
        assertEquals(0, a.getBalanceMinor());
    }

    @Test
    void overspendIsRejectedAndBalanceIsUnchanged() {
        Account a = wallet(1_000);
        assertThrows(InsufficientFundsException.class, () -> a.applyEntry(Direction.DEBIT, 1_001));
        assertEquals(1_000, a.getBalanceMinor());
    }

    @Test
    void assetAccountIncreasesWithDebits() {
        Account bank = wallet(0);
        ReflectionTestUtils.setField(bank, "accountType", AccountType.ASSET);
        bank.applyEntry(Direction.DEBIT, 700);
        assertEquals(700, bank.getBalanceMinor());
    }


}
