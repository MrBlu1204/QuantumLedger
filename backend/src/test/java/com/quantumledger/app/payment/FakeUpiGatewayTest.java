package com.quantumledger.app.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakeUpiGatewayTest {

    private final FakeUpiGateway gateway = new FakeUpiGateway();

    private GatewayRequest request(String upiId) {
        return new GatewayRequest("ref-1", 50_000, "INR", upiId);
    }

    @Test
    void approvesAnOrdinaryHandle() {
        GatewayResult result = gateway.collect(request("asha@okbank"));

        assertThat(result.successful()).isTrue();
        assertThat(result.gatewayRef()).startsWith("FAKE-");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void declinesAFailHandle() {
        GatewayResult result = gateway.collect(request("asha@fail"));

        assertThat(result.successful()).isFalse();
        assertThat(result.failureReason()).isEqualTo("DECLINED_BY_BANK");
    }

    @Test
    void reportsUnavailableForATimeoutHandle() {
        assertThatThrownBy(() -> gateway.collect(request("asha@timeout")))
                .isInstanceOf(GatewayUnavailableException.class);
    }

    @Test
    void requestToStringHidesTheUpiHandle() {
        assertThat(request("secret@okbank").toString()).doesNotContain("secret@okbank");
    }
}
