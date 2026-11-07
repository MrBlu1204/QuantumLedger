package com.quantumledger.app.payment;

import com.quantumledger.app.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentGatewayWiringTest extends AbstractIntegrationTest {

    @Autowired
    PaymentGatewayFactory factory;

    @Test
    void springRegistersEveryGatewayAsAStrategy() {
        assertThat(factory.forMethod(PaymentMethod.UPI)).isInstanceOf(FakeUpiGateway.class);
        assertThat(factory.forMethod(PaymentMethod.NET_BANKING)).isInstanceOf(FakeNetBankingGateway.class);
    }
}
