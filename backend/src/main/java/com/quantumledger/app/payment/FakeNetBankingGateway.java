package com.quantumledger.app.payment;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FakeNetBankingGateway implements PaymentGateway {

    @Override
    public PaymentMethod method(){
        return PaymentMethod.NET_BANKING;
    }

    @Override
    public GatewayResult collect(GatewayRequest request){
        String bank = request.payerRef() ==null ? "" : request.payerRef().toUpperCase();

        if(bank.equals("DOWNBANK")){
            throw new GatewayUnavailableException("Bank portal is down");
        }

        String ref = "FAKE-NB-" + UUID.randomUUID(); //44 char, fits gateway_ref VARCHAR(64)

        if(bank.equals("DECLINEBANK")){
            return GatewayResult.declined(ref, "DECLINED_BY_BANK");
        }
        return GatewayResult.success(ref);
    }


}
