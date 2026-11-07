package com.quantumledger.app.payment;

public class UnsupportedPaymentMethodException extends RuntimeException {
    public UnsupportedPaymentMethodException(PaymentMethod method)
    {
        super("payment method not supported: " + method);
    }
}
