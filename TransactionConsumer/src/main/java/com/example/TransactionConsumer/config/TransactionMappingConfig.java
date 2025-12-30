package com.example.TransactionConsumer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "txn.mappings")
public class TransactionMappingConfig {
    private EntityMapping payment;
    private EntityMapping sender;
    private EntityMapping recipient;
    private EntityMapping transactionDetail;
    private Map<String, Map<String, FieldMapping>> address;
}