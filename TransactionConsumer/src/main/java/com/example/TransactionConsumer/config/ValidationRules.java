package com.example.TransactionConsumer.config;

import lombok.Data;
import java.util.List;

@Data
public class ValidationRules {
    private Integer min;
    private Integer max;
    private Integer maxLength;
    private String pattern;
    private List<String> allowed;
}