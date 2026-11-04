package com.quantumledger.app.payment;

public record GatewayResult(
        boolean successful, String gatewayRef, String failureReason
) {
    public static GatewayResult success(String gatewayRef){
        return new GatewayResult(true, gatewayRef, null);
    }

    public static GatewayResult declined(String gatewayRef, String failureReason){
        return new GatewayResult(false, gatewayRef, failureReason);
    }
}
