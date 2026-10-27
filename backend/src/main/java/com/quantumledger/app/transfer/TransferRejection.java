package com.quantumledger.app.transfer;

public enum TransferRejection {
    AMOUNT_OUT_OF_RANGE("Amount is outside the allowed range"),
    CANNOT_TRANSFER_TO_SELF("You cannot transfer money to yourself"),
    RECEIVER_UNAVAILABLE("The receiver cannot accept transfers right now");

    private final String message;

    TransferRejection(String message) {
        this.message = message;
    }

    public String message() {
        return this.message;
    }
}
