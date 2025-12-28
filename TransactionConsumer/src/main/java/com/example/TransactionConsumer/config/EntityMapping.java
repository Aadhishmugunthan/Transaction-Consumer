package com.example.TransactionConsumer.config;

import lombok.Data;
import java.util.Map;

@Data
public class EntityMapping {
    private Map<String, FieldMapping> party;
    private Map<String, FieldMapping> address;
    private Map<String, FieldMapping> transaction;
    private Map<String, FieldMapping> detail;
}
