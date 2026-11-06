package com.quantumledger.app.addmoney;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AddMoneyProperties.class)
class AddMoneyConfig {
}
