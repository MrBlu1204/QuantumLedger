package com.quantumledger.app.ledger;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    LedgerService ledgerService;

    @Test
    void walletBalanceIsCreditsMinusDebits(){
        when(ledgerEntryRepository.netCredits(1L)).thenReturn(74_500L);
        assertEquals(74_500, ledgerService.balanceOf(1L, Direction.CREDIT));
    }

    @Test
    void  walletBalanceIsDebitsMinusCredits(){
        when(ledgerEntryRepository.netCredits(2L)).thenReturn(-100_000L);
        assertEquals(100_000, ledgerService.balanceOf(2L, Direction.DEBIT));
    }

    @Test
    void emptyAccountHasZeroBalance(){
        when(ledgerEntryRepository.netCredits(3L)).thenReturn(0L);
        assertEquals(0, ledgerService.balanceOf(3L, Direction.CREDIT));
    }
}
