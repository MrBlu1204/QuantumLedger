package com.quantumledger.app.transfer;

import com.quantumledger.app.account.Account;
import com.quantumledger.app.account.AccountService;
import com.quantumledger.app.account.InsufficientFundsException;
import com.quantumledger.app.auth.AccountNotActiveException;
import com.quantumledger.app.ledger.Direction;
import com.quantumledger.app.ledger.LedgerTransactionType;
import com.quantumledger.app.posting.PostingLine;
import com.quantumledger.app.posting.PostingRequest;
import com.quantumledger.app.posting.PostingResult;
import com.quantumledger.app.posting.PostingService;
import com.quantumledger.app.user.User;
import com.quantumledger.app.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final String COUNTRY = "+91";
    private static final String PHONE = "9876543210";

    @Mock
    UserService userService;
    @Mock
    AccountService accountService;
    @Mock
    PostingService postingService;
    @Mock TransferRepository transferRepository;

    private TransferService service;

    @BeforeEach
    void setUp() {
        service = new TransferService(userService, accountService, postingService,
                transferRepository, new TransferProperties(100, 10_000_000));
    }

    private User user(long id, String firstName, String status, boolean deleted) {
        User u = mock(User.class);
        lenient().when(u.getUserId()).thenReturn(id);
        lenient().when(u.getFirstName()).thenReturn(firstName);
        lenient().when(u.getStatus()).thenReturn(status);
        lenient().when(u.isDeleted()).thenReturn(deleted);
        return u;
    }

    private Account wallet(long accountId, long userId) {
        Account a = Account.wallet(userId, "INR");
        ReflectionTestUtils.setField(a, "accountId", accountId);
        return a;
    }

    private TransferService.TransferCommand command(long amount) {
        return new TransferService.TransferCommand(1L, COUNTRY, PHONE, amount);
    }

    private void givenSenderAndReceiver(User receiver) {
        User sender = user(1L, "Asha", "ACTIVE", false);
        Account senderWallet = wallet(10L, 1L);
        Account receiverWallet = wallet(20L, 2L);

        lenient().when(userService.getActiveUser(1L)).thenReturn(sender);
        lenient().when(userService.findByPhone(COUNTRY, PHONE)).thenReturn(Optional.of(receiver));
        lenient().when(accountService.getWallet(1L)).thenReturn(senderWallet);
        lenient().when(accountService.getWallet(2L)).thenReturn(receiverWallet);
    }

    @Test
    void movesMoneyAndRecordsTheTransfer() {
        givenSenderAndReceiver(user(2L, "Ravi", "ACTIVE", false));
        when(postingService.post(any(PostingRequest.class))).thenReturn(new PostingResult(99L, "ref"));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferService.TransferReceipt receipt = service.transfer(command(25_000));

        ArgumentCaptor<PostingRequest> captor = ArgumentCaptor.forClass(PostingRequest.class);
        verify(postingService).post(captor.capture());
        PostingRequest request = captor.getValue();
        assertEquals(LedgerTransactionType.TRANSFER, request.type());
        assertEquals("INR", request.currency());
        assertEquals(2, request.lines().size());

        PostingLine debit = request.lines().stream()
                .filter(l -> l.direction() == Direction.DEBIT).findFirst().orElseThrow();
        PostingLine credit = request.lines().stream()
                .filter(l -> l.direction() == Direction.CREDIT).findFirst().orElseThrow();
        assertEquals(10L, debit.accountId());
        assertEquals(20L, credit.accountId());
        assertEquals(25_000, debit.amountMinor());
        assertEquals(25_000, credit.amountMinor());

        Transfer t = receipt.transfer();
        assertEquals(1L, t.getSenderId());
        assertEquals(2L, t.getReceiverId());
        assertEquals(10L, t.getSenderAccountId());
        assertEquals(20L, t.getReceiverAccountId());
        assertEquals(25_000, t.getAmountMinor());
        assertEquals(99L, t.getLedgerTransactionId());
        assertEquals(TransferStatus.COMPLETED, t.getStatus());
        assertEquals("Ravi", receipt.receiverFirstName());
    }

    @Test
    void rejectsAnAmountBelowTheMinimumWithoutTouchingAnything() {
        TransferRejectedException e = assertThrows(TransferRejectedException.class,
                () -> service.transfer(command(99)));

        assertEquals(TransferRejection.AMOUNT_OUT_OF_RANGE, e.getRejection());
        verifyNoInteractions(userService, accountService, postingService, transferRepository);
    }

    @Test
    void rejectsAnAmountAboveTheMaximumWithoutTouchingAnything() {
        TransferRejectedException e = assertThrows(TransferRejectedException.class,
                () -> service.transfer(command(10_000_001)));

        assertEquals(TransferRejection.AMOUNT_OUT_OF_RANGE, e.getRejection());
        verifyNoInteractions(userService, accountService, postingService, transferRepository);
    }

    @Test
    void rejectsAnUnknownReceiver() {
        User sender = user(1L, "Asha", "ACTIVE", false);
        lenient().when(userService.getActiveUser(1L)).thenReturn(sender);
        when(userService.findByPhone(COUNTRY, PHONE)).thenReturn(Optional.empty());

        assertThrows(ReceiverNotFoundException.class, () -> service.transfer(command(1_000)));

        verifyNoInteractions(postingService, transferRepository);
    }

    @Test
    void rejectsATransferToYourself() {
        givenSenderAndReceiver(user(1L, "Asha", "ACTIVE", false));

        TransferRejectedException e = assertThrows(TransferRejectedException.class,
                () -> service.transfer(command(1_000)));

        assertEquals(TransferRejection.CANNOT_TRANSFER_TO_SELF, e.getRejection());
        verifyNoInteractions(postingService, transferRepository);
    }

    @Test
    void rejectsABlockedReceiver() {
        givenSenderAndReceiver(user(2L, "Ravi", "BLOCKED", false));

        TransferRejectedException e = assertThrows(TransferRejectedException.class,
                () -> service.transfer(command(1_000)));

        assertEquals(TransferRejection.RECEIVER_UNAVAILABLE, e.getRejection());
        verifyNoInteractions(postingService, transferRepository);
    }

    @Test
    void rejectsADeletedReceiver() {
        givenSenderAndReceiver(user(2L, "Ravi", "ACTIVE", true));

        TransferRejectedException e = assertThrows(TransferRejectedException.class,
                () -> service.transfer(command(1_000)));

        assertEquals(TransferRejection.RECEIVER_UNAVAILABLE, e.getRejection());
        verifyNoInteractions(postingService, transferRepository);
    }

    @Test
    void stopsAtOnceWhenTheSenderIsNotActive() {
        when(userService.getActiveUser(1L)).thenThrow(new AccountNotActiveException("not available"));

        assertThrows(AccountNotActiveException.class, () -> service.transfer(command(1_000)));

        verify(userService, never()).findByPhone(any(), any());
        verifyNoInteractions(postingService, transferRepository);
    }

    @Test
    void leavesNoTransferRecordWhenThePostingFails() {
        givenSenderAndReceiver(user(2L, "Ravi", "ACTIVE", false));
        when(postingService.post(any(PostingRequest.class)))
                .thenThrow(new InsufficientFundsException("Insufficient funds"));

        assertThrows(InsufficientFundsException.class, () -> service.transfer(command(5_000)));

        verify(transferRepository, never()).save(any());
    }

}
