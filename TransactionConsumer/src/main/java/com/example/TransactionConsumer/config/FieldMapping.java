package com.example.TransactionConsumer.config;

import lombok.Data;

@Data
public class FieldMapping {
    private String source;
    private String path;
    private String value;
    private Boolean required;
    private String defaultValue;
    private ValidationRules validation;
}