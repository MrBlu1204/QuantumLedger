package com.quantumledger.app.payment;

public interface PaymentGateway {


    /** Which payment method this gateway handles. The factory uses it as the lookup key. */
    PaymentMethod method();

    /** Collects money from the payer. Throws GatewayUnavailableException when the outcome is unknown. */
    GatewayResult collect(GatewayRequest request);

}
