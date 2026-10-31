package com.quantumledger.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class CiGateTest {

    @Test
    void deliberateFailureToProveTheGate() {
        fail("This failure is on purpose. If you can merge this PR, the gate is broken.");
    }
}
