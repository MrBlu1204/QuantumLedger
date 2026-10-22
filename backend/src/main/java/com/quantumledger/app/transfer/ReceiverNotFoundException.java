package com.quantumledger.app.transfer;

public class ReceiverNotFoundException extends RuntimeException {
    public ReceiverNotFoundException() {
        super("No wallet found for the phone number");
    }
}
