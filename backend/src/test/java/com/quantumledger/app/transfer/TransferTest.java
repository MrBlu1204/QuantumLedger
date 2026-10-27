package com.quantumledger.app.transfer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferTest {

    @Test
    void createsCompletedTransferWithAReference() {
        Transfer transfer = Transfer.completed(1L, 2L, 10L, 20L, 25_000, "INR", 99L);

        assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
        assertEquals(25_000, transfer.getAmountMinor());
        assertEquals(36, transfer.getTransferRef().length());   //UUID
    }

    @Test
    void rejectsZeroAmount() {
        assertThrows(IllegalArgumentException.class, () -> Transfer.completed(1L, 2L, 10L, 20L, 0, "INR", 99L));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> Transfer.completed(1L, 2L, 10L, 20L, -5, "INR", 99L));
    }

    @Test
    void rejectsSendingToYourself() {
        assertThrows(IllegalArgumentException.class, () -> Transfer.completed(1L, 1L, 10L, 20L, 100, "INR", 99L));
    }
}
