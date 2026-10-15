package com.quantumledger.app.ledger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LedgerEntryTest {

    @Test
    void rejectsZeroAmount(){
        assertThrows(IllegalArgumentException.class,
                () -> new LedgerEntry(1L, 1L, Direction.CREDIT, 0, "INR"));
    }

    @Test
    void rejectsNegativeAmount(){
        assertThrows(IllegalArgumentException.class,
                ()  -> new LedgerEntry(1L, -1L, Direction.DEBIT, -50, "INR"));
    }

    @Test
    void acceptsPositiveAmount(){
        LedgerEntry entry = new LedgerEntry(1L, 1L, Direction.CREDIT, 50_000, "INR");
        assertEquals(50_000, entry.getAmountMinor());
        assertEquals(Direction.CREDIT, entry.getDirection());
    }
}
