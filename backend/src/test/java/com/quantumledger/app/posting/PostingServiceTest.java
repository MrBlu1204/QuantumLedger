package com.quantumledger.app.posting;

import com.quantumledger.app.account.*;
import com.quantumledger.app.ledger.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostingServiceTest {

    private static final long ASHA = 30L;
    private static final long RAVI = 40L;

    @Mock
    AccountRepository accountRepository;

    @Mock
    LedgerTransactionRespository ledgerTransactionRespository;

    @Mock
    LedgerEntryRepository ledgerEntryRepository;

    @Captor
    ArgumentCaptor<List<LedgerEntry>> entriesCaptor;

    @InjectMocks
    PostingService postingService;

    private Account wallet(long id, long balance) {
        Account a = Account.wallet(100 + id, "INR");
        ReflectionTestUtils.setField(a, "accountId", id);
        ReflectionTestUtils.setField(a, "balanceMinor", balance);
        return a;
    }

    private PostingRequest transfer(long amount) {
        return new PostingRequest(LedgerTransactionType.TRANSFER, "test", "INR",
                List.of(PostingLine.credit(RAVI, amount), PostingLine.debit(ASHA, amount)));
    }

    private void givenWallets(Account asha, Account ravi){
        when(accountRepository.findByIdForUpdate(ASHA)).thenReturn(Optional.of(asha));
        when(accountRepository.findByIdForUpdate(RAVI)).thenReturn(Optional.of(ravi));
    }

    @Test
    void movesMoneyBetweenWallets() {
        Account asha = wallet(ASHA, 100_000);
        Account ravi = wallet(RAVI, 0);
        givenWallets(asha, ravi);
        when(ledgerTransactionRespository.save(any(LedgerTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PostingResult result = postingService.post(transfer(25_000));

        assertEquals(75_000, asha.getBalanceMinor());
        assertEquals(25_000, ravi.getBalanceMinor());
        assertEquals(36, result.transactionRef().length());  //a UUID
    }

    @Test
    void locksAccountsInAscendingOrderRegardlessOfLineOrder() {
        givenWallets(wallet(ASHA, 100_000), wallet(RAVI, 0));
        when(ledgerTransactionRespository.save(any(LedgerTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        postingService.post(transfer(1_000));  // the request lists RAVI before ASHA

        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository).findByIdForUpdate(ASHA);
        order.verify(accountRepository).findByIdForUpdate(RAVI);
    }

    @Test
    void writesOneEntryPerLine() {
        givenWallets(wallet(ASHA, 100_000), wallet(RAVI, 0));
        when(ledgerTransactionRespository.save(any(LedgerTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        postingService.post(transfer(1_000));

        verify(ledgerEntryRepository).saveAll(entriesCaptor.capture());
        List<LedgerEntry> saved = entriesCaptor.getValue();
        assertEquals(2, saved.size());
        assertEquals(1, saved.stream().filter(e -> e.getDirection() == Direction.DEBIT).count());
        assertEquals(1, saved.stream().filter(e -> e.getDirection() == Direction.CREDIT).count());
    }


    @Test
    void overspendWritesNothing(){
        givenWallets(wallet(ASHA, 1_000), wallet(RAVI, 0));

        assertThrows(InsufficientFundsException.class, () -> postingService.post(transfer(5_000)));

        verify(ledgerTransactionRespository, never()).save(any());
        verify(ledgerEntryRepository, never()).saveAll(any());
    }

    @Test
    void frozenAccountIsRejected(){
        Account asha = wallet(ASHA, 100_000);
        ReflectionTestUtils.setField(asha, "status", "FROZEN");
        givenWallets(asha, wallet(RAVI, 0));

        assertThrows(AccountFrozenException.class, () -> postingService.post(transfer(1_000)));

        verify(ledgerTransactionRespository, never()).save(any());
    }

    @Test
    void unkownAccountIsRejected(){
        when(accountRepository.findByIdForUpdate(ASHA)).thenReturn(Optional.empty());

        assertThrows(InvalidPostingException.class, () -> postingService.post(transfer(1_000)));

        verify(ledgerTransactionRespository, never()).save(any());
    }


}
