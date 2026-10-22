package com.quantumledger.app.transfer;

public class TransferRejectedException extends RuntimeException {
    private final TransferRejection rejection;

    public TransferRejectedException(TransferRejection rejection) {

        super(rejection.message());
        this.rejection = rejection;
    }

    public TransferRejection getRejection() {
        return rejection;
    }
}
