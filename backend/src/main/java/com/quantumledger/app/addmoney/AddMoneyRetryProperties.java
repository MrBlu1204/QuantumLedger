package com.quantumledger.app.addmoney;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "app.add-money.retry")
public record AddMoneyRetryProperties(int maxAttempts, long baseDelayMs) {

    public AddMoneyRetryProperties {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("app.add-money.retry.max-attempts must be at least 1");
        }
        if (baseDelayMs < 0) {
            throw new IllegalArgumentException("app.add-money.retry.base-delay-ms must not be negative");
        }
    }
}
