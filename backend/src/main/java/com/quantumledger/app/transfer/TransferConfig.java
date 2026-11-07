package com.quantumledger.app.transfer;


import com.quantumledger.app.common.Sleeper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({TransferProperties.class, TransferRetryProperties.class})
class TransferConfig {

}
