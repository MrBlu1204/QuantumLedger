package com.quantumledger.app.payment;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class FakeUpiGateway implements PaymentGateway {

    @Override
    public PaymentMethod method(){
        return PaymentMethod.UPI;
    }

    @Override
    public GatewayResult collect(GatewayRequest request) {

        String handle = request.payerRef() == null ? "" : request.payerRef().toLowerCase();

        if(handle.endsWith("@timeout")){
            throw new GatewayUnavailableException("UPI provider did not respond");
        }

        String ref = "FAKE-" + UUID.randomUUID();  //41 chars, fits getway_ref VARCHAR(64)

        if(handle.endsWith("@fail")){
            return GatewayResult.declined(ref, "DECLINED_BY_BANK");
        }

        return GatewayResult.success(ref);
    }
}
