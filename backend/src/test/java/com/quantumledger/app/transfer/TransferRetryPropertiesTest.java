package com.quantumledger.app.transfer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferRetryPropertiesTest {

    @Test
    void acceptsValidValues() {
        assertEquals(3, new TransferRetryProperties(3, 50).maxAttempts());
    }

    @Test
    void rejectsZeroAttempts() {
        assertThrows(IllegalArgumentException.class, () -> new TransferRetryProperties(0, 50));
    }

    @Test
    void rejectsNegativeDelay() {
        assertThrows(IllegalArgumentException.class, () -> new TransferRetryProperties(3, -1));
    }
}
