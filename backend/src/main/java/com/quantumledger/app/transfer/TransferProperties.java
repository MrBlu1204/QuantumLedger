package com.quantumledger.app.transfer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.transfer")
public record TransferProperties(long minAmountMinor, long maxAmountMinor) {

    public TransferProperties{
        if(minAmountMinor <= 0){
            throw new IllegalArgumentException("app.transfer.min-amount-minor must be positive");
        }
        if(maxAmountMinor < minAmountMinor){
            throw new IllegalArgumentException("app.transfer.max-amount-minor must be greater than min");
        }
    }
}
