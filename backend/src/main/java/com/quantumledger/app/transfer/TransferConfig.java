package com.quantumledger.app.transfer;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TransferProperties.class)
class TransferConfig {
}
