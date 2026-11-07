package com.quantumledger.app.payment;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentGatewayFactoryTest {

    @Test
    void returnsTheGatewayRegisteredForEachMethod() {
        FakeUpiGateway upi = new FakeUpiGateway();
        FakeNetBankingGateway netBanking = new FakeNetBankingGateway();
        PaymentGatewayFactory factory = new PaymentGatewayFactory(List.of(upi, netBanking));

        assertThat(factory.forMethod(PaymentMethod.UPI)).isSameAs(upi);
        assertThat(factory.forMethod(PaymentMethod.NET_BANKING)).isSameAs(netBanking);
    }

    @Test
    void rejectsAMethodWithNoGateway() {
        PaymentGatewayFactory factory = new PaymentGatewayFactory(List.of(new FakeUpiGateway()));

        assertThatThrownBy(() -> factory.forMethod(PaymentMethod.NET_BANKING))
                .isInstanceOf(UnsupportedPaymentMethodException.class);
    }

    @Test
    void refusesToStartWhenTwoGatewaysClaimTheSameMethod() {
        assertThatThrownBy(() ->
                new PaymentGatewayFactory(List.of(new FakeUpiGateway(), new FakeUpiGateway())))
                .isInstanceOf(IllegalStateException.class);
    }
}
