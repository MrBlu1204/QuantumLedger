package com.quantumledger.app.transfer;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({TransferProperties.class, TransferRetryProperties.class})
class TransferConfig {

    @Bean
    Sleeper retrySleeper() {
        return millis -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting to retry",e);
            }
        };
    }
}
