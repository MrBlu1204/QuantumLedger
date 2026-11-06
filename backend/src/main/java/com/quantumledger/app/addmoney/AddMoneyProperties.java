package com.quantumledger.app.addmoney;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.add-money")
public record AddMoneyProperties(long minAmountMinor, long maxAmountMinor) {

    public AddMoneyProperties {
        if (minAmountMinor <= 0) {
            throw new IllegalArgumentException("app.add-money.min-amount-minor must be positive");
        }
        if(maxAmountMinor < minAmountMinor) {
            throw new IllegalArgumentException("app.add-money.max-amount-minor must be greater than min");
        }
    }
}
