package com.quantumledger.app.payment;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentGatewayFactory {
    public final Map<PaymentMethod, PaymentGateway> gateways = new EnumMap<>(PaymentMethod.class);

    public PaymentGatewayFactory(List<PaymentGateway> available) {

        for(PaymentGateway gateway : available){
            PaymentGateway previous = gateways.put(gateway.method(), gateway);

            if(previous != null) {
                throw new IllegalStateException("Two payment gateways registered for " + gateway.method()
                + ": " + previous.getClass().getSimpleName()
                + "and " + gateway.getClass().getSimpleName());
            }
        }
    }

    public PaymentGateway forMethod(PaymentMethod method){
        PaymentGateway gateway =  method == null ? null: gateways.get(method);

        if(gateway == null){
            throw new UnsupportedPaymentMethodException(method);
        }

        return gateway;
    }
}
