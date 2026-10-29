package com.quantumledger.app.ledger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LedgerEntryTest {

    @Test
    void rejectsZeroAmount(){
        assertThrows(IllegalArgumentException.class,
                () -> new LedgerEntry(1L, 1L, Direction.CREDIT, 0, "INR",0));
    }

    @Test
    void rejectsNegativeAmount(){
        assertThrows(IllegalArgumentException.class,
                ()  -> new LedgerEntry(1L, -1L, Direction.DEBIT, -50, "INR",-50));
    }

    @Test
    void acceptsPositiveAmount(){
        LedgerEntry entry = new LedgerEntry(1L, 1L, Direction.CREDIT, 50_000, "INR",50_000);
        assertEquals(50_000, entry.getAmountMinor());
        assertEquals(Direction.CREDIT, entry.getDirection());
    }
}
