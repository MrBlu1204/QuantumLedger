package com.quantumledger.app.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakeNetBankingGatewayTest {

    private final FakeNetBankingGateway gateway = new FakeNetBankingGateway();

    private GatewayRequest request(String bank) {
        return new GatewayRequest("ref-1", 50_000, "INR", bank);
    }

    @Test
    void approvesAnOrdinaryBank() {
        GatewayResult result = gateway.collect(request("HDFC"));

        assertThat(result.successful()).isTrue();
        assertThat(result.gatewayRef()).startsWith("FAKE-NB-");
    }

    @Test
    void declinesTheDeclineBank() {
        GatewayResult result = gateway.collect(request("DECLINEBANK"));

        assertThat(result.successful()).isFalse();
        assertThat(result.failureReason()).isEqualTo("DECLINED_BY_BANK");
    }

    @Test
    void reportsUnavailableForTheDownBank() {
        assertThatThrownBy(() -> gateway.collect(request("DOWNBANK")))
                .isInstanceOf(GatewayUnavailableException.class);
    }
}
