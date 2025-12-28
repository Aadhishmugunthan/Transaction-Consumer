package com.example.TransactionConsumer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "txn.mappings")
public class TransactionMappingConfig {
    private EntityMapping payment;
    private EntityMapping sender;
    private EntityMapping recipient;
    private EntityMapping transactionDetail;
}