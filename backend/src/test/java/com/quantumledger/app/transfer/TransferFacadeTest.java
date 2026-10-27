package com.quantumledger.app.transfer;

import com.quantumledger.app.account.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferFacadeTest {

    @Mock
    TransferService transferService;

    private final List<Long> sleeps = new ArrayList<>();
    private final TransferService.TransferCommand command =
            new TransferService.TransferCommand(1L, "+91", "9876543210", 1_000);
    private final TransferService.TransferReceipt receipt =
            new TransferService.TransferReceipt(null, "Ravi");

    private TransferFacade facade;

    @BeforeEach
    void setUp() {
        facade = new TransferFacade(transferService, new TransferRetryProperties(3, 50), sleeps::add);
    }

    @Test
    void returnsTheResultWhenTheFirstAttemptSucceeds() {
        when(transferService.transfer(command)).thenReturn(receipt);

        assertSame(receipt, facade.transfer(command));

        verify(transferService, times(1)).transfer(command);
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void retriesAfterALockConflictAndThenSucceeds() {
        when(transferService.transfer(command))
                .thenThrow(new CannotAcquireLockException("deadlock"))
                .thenReturn(receipt);

        assertSame(receipt, facade.transfer(command));

        verify(transferService, times(2)).transfer(command);
        assertEquals(1, sleeps.size());
    }

    @Test
    void recognisesADeadlockHiddenInsideOtherExceptions() {
        RuntimeException wrapped = new RuntimeException("wrapper",
                new SQLException("Deadlock found when trying to get lock", "40001", 1213));
        when(transferService.transfer(command)).thenThrow(wrapped).thenReturn(receipt);

        assertSame(receipt, facade.transfer(command));

        verify(transferService, times(2)).transfer(command);
    }

    @Test
    void givesUpAfterTheMaximumNumberOfAttempts() {
        when(transferService.transfer(command)).thenThrow(new CannotAcquireLockException("deadlock"));

        TransferBusyException e = assertThrows(TransferBusyException.class,
                () -> facade.transfer(command));

        verify(transferService, times(3)).transfer(command);
        assertEquals(2, sleeps.size());                        // no sleep after the last attempt
        assertInstanceOf(CannotAcquireLockException.class, e.getCause());
    }

    @Test
    void doesNotRetryInsufficientFunds() {
        when(transferService.transfer(command)).thenThrow(new InsufficientFundsException("no"));

        assertThrows(InsufficientFundsException.class, () -> facade.transfer(command));

        verify(transferService, times(1)).transfer(command);
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void doesNotRetryARejectedTransfer() {
        when(transferService.transfer(command))
                .thenThrow(new TransferRejectedException(TransferRejection.CANNOT_TRANSFER_TO_SELF));

        assertThrows(TransferRejectedException.class, () -> facade.transfer(command));

        verify(transferService, times(1)).transfer(command);
    }

    @Test
    void doesNotRetryUnrelatedErrors() {
        when(transferService.transfer(command)).thenThrow(new IllegalStateException("bug"));

        assertThrows(IllegalStateException.class, () -> facade.transfer(command));

        verify(transferService, times(1)).transfer(command);
    }

    @Test
    void backoffDelaysStayWithinTheirCaps() {
        when(transferService.transfer(command)).thenThrow(new CannotAcquireLockException("deadlock"));

        assertThrows(TransferBusyException.class, () -> facade.transfer(command));

        assertTrue(sleeps.get(0) >= 0 && sleeps.get(0) <= 50, "first delay: " + sleeps.get(0));
        assertTrue(sleeps.get(1) >= 0 && sleeps.get(1) <= 100, "second delay: " + sleeps.get(1));
    }

    @Test
    void refusesToRunInsideATransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThrows(IllegalStateException.class, () -> facade.transfer(command));
            verifyNoInteractions(transferService);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

}
