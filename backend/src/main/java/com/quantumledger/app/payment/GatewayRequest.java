package com.quantumledger.app.payment;

/** payerRef is a UPI handle or a bank code, depending on the method. */
public record GatewayRequest(String requestRef, long amountMinor, String currency, String payerRef) {

    /** Handwritten so the payer reference can never leak into logs. */
    @Override
    public String toString() {
        return "GatewayRequest[requestRef=" + requestRef
                + ", amountMinor=" + amountMinor + ", currency=" + currency + "]";
    }
}
