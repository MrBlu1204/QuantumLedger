package com.quantumledger.app.transfer;

public class TransferBusyException  extends RuntimeException {
    public TransferBusyException(Throwable cause) {
        super("The system is busy, please try again after sometime", cause);
    }
}
