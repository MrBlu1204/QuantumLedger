package com.quantumledger.app.transfer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.transfer.retry")
public record TransferRetryProperties(int maxAttempts, long baseDelayMs) {

    public TransferRetryProperties{
        if(maxAttempts < 1){
            throw new IllegalArgumentException("app.transfer.retry.max-attempts must be at least 1");
        }
        if(baseDelayMs < 0){
            throw new IllegalArgumentException("app.transfer.retry.base-delay-ms must not be negative");
        }
    }
}
