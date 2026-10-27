package com.quantumledger.app.transfer;

import java.time.Instant;

public record TransferResponse(String transferRef, String status, long amountMinor,
                               String currency, Receiver receiver, Instant createTime) {

    public record Receiver(String firstName){ }

    public static TransferResponse from(TransferService.TransferReceipt receipt) {
        Transfer transfer = receipt.transfer();
        return new TransferResponse(transfer.getTransferRef(), transfer.getStatus().name(),
                transfer.getAmountMinor(), transfer.getCurrency(),
                new Receiver(receipt.receiverFirstName()), transfer.getCreateTime());
    }
}
