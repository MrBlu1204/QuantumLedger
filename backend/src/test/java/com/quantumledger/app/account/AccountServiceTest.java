package com.quantumledger.app.account;

import com.quantumledger.app.ledger.LedgerService;
import com.quantumledger.app.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    LedgerService ledgerService;

    @Mock
    UserService userService;

    @InjectMocks
    AccountService accountService;

    @Test
    void createsWallectWhenNoneExists() {
        when(accountRepository.findByUserIdAndCurrency(7L, "INR")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account wallet = accountService.createWalletFor(7L);

        assertEquals(7L, wallet.getUserId());
        assertEquals(AccountType.WALLET, wallet.getAccountType());
        verify(accountRepository).save(any(Account.class));

    }

    @Test
    void returnsExistingWalletWithoutCreatingAnother(){

        Account existing = Account.wallet(7L, "INR");
        when(accountRepository.findByUserIdAndCurrency(7L, "INR")).thenReturn(Optional.of(existing));

        Account result = accountService.createWalletFor(7L);

        assertSame(existing, result);
        verify(accountRepository, never()).save(any(Account.class));
    }
}
