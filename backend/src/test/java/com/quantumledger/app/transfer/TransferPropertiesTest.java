package com.quantumledger.app.transfer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferPropertiesTest {

    @Test
    void acceptsAValidRange() {
        TransferProperties p = new TransferProperties(100, 10_000_000);
        assertEquals(10_000_000, p.maxAmountMinor());
    }

    @Test
    void rejectsAZeroMinimum() {
        assertThrows(IllegalArgumentException.class, () -> new TransferProperties(0, 1_000));
    }

    @Test
    void rejectsAMaximumBelowMinimum() {
        assertThrows(IllegalArgumentException.class, () -> new TransferProperties(500, 100));
    }
}
