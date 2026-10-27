package com.quantumledger.app.transfer;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransferResponseTest {

    @Test
    void mapsAReceiptToAResponseThatRevealsOnlyTheReceiversFirstName(){
        Transfer transfer = Transfer.completed(1L, 2L, 10L, 20L, 25_000, "INR", 99L);

        TransferResponse response = TransferResponse.from(new TransferService.TransferReceipt(transfer, "Ravi"));

        assertEquals(transfer.getTransferRef(), response.transferRef());
        assertEquals("COMPLETED", response.status());
        assertEquals(25_000, response.amountMinor());
        assertEquals("INR", response.currency());
        assertEquals("Ravi", response.receiver().firstName());

    }
}
