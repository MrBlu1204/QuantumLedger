package com.quantumledger.app.addmoney;

public record AddMoneyResponse(
        String requestRef,
        AddMoneyStatus status,
        long amountMinor,
        String currency,
        String failureReason
) {

    static AddMoneyResponse from(AddMoneyService.AddMoneyReceipt receipt) {
        return new AddMoneyResponse(
                receipt.requestRef(), receipt.status(), receipt.amountMinor(), receipt.currency(), receipt.failureReason()
        );
    }
}
