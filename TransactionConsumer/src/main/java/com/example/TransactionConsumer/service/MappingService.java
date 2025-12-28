package com.example.TransactionConsumer.service;

import com.example.TransactionConsumer.config.FieldMapping;
import com.example.TransactionConsumer.config.TransactionMappingConfig;
import com.example.TransactionConsumer.config.ValidationRules;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MappingService {

    private final TransactionMappingConfig mappingConfig;

    public MappingService(TransactionMappingConfig mappingConfig) {
        this.mappingConfig = mappingConfig;
    }

    /**
     * Extract value from JSON using externalized mapping
     */
    public Object extractValue(String json, FieldMapping mapping) {
        if (mapping == null) {
            return null;
        }

        switch (mapping.getSource()) {
            case "json":
                return extractFromJson(json, mapping);
            case "constant":
                return mapping.getValue();
            case "generated":
                return UUID.randomUUID().toString();
            default:
                throw new IllegalArgumentException("Unknown source: " + mapping.getSource());
        }
    }

    /**
     * Extract value from JSON with error handling
     */
    private Object extractFromJson(String json, FieldMapping mapping) {
        try {
            Object value = JsonPath.read(json, mapping.getPath());

            if (value == null && mapping.getRequired() != null && mapping.getRequired()) {
                if (mapping.getDefaultValue() != null) {
                    return mapping.getDefaultValue();
                }
                throw new IllegalArgumentException(
                        "Required field missing: " + mapping.getPath()
                );
            }

            // Validate value
            validateValue(value, mapping);

            return value;
        } catch (PathNotFoundException e) {
            if (mapping.getRequired() != null && mapping.getRequired()) {
                throw new IllegalArgumentException(
                        "Required field not found: " + mapping.getPath(), e
                );
            }
            return mapping.getDefaultValue();
        }
    }

    /**
     * Validate extracted value against rules
     */
    private void validateValue(Object value, FieldMapping mapping) {
        if (value == null || mapping.getValidation() == null) {
            return;
        }

        ValidationRules rules = mapping.getValidation();

        // Check allowed values (enum validation)
        if (rules.getAllowed() != null && !rules.getAllowed().isEmpty()) {
            if (!rules.getAllowed().contains(value.toString())) {
                throw new IllegalArgumentException(
                        "Value '" + value + "' not in allowed list: " + rules.getAllowed()
                );
            }
        }

        // Check pattern (regex)
        if (rules.getPattern() != null) {
            if (!value.toString().matches(rules.getPattern())) {
                throw new IllegalArgumentException(
                        "Value '" + value + "' does not match pattern: " + rules.getPattern()
                );
            }
        }

        // Check max length
        if (rules.getMaxLength() != null) {
            if (value.toString().length() > rules.getMaxLength()) {
                throw new IllegalArgumentException(
                        "Value exceeds max length of " + rules.getMaxLength()
                );
            }
        }

        // Check numeric range
        if (value instanceof Number) {
            int numValue = ((Number) value).intValue();

            if (rules.getMin() != null && numValue < rules.getMin()) {
                throw new IllegalArgumentException(
                        "Value " + numValue + " is less than minimum " + rules.getMin()
                );
            }

            if (rules.getMax() != null && numValue > rules.getMax()) {
                throw new IllegalArgumentException(
                        "Value " + numValue + " exceeds maximum " + rules.getMax()
                );
            }
        }
    }

    /**
     * Get all sender party field mappings - FIXED
     */
    public Map<String, FieldMapping> getSenderPartyMappings() {
        if (mappingConfig.getSender() == null || mappingConfig.getSender().getParty() == null) {
            return new HashMap<>();
        }
        return mappingConfig.getSender().getParty();
    }

    /**
     * Get all sender address mappings - FIXED
     */
    public Map<String, FieldMapping> getSenderAddressMappings() {
        if (mappingConfig.getSender() == null || mappingConfig.getSender().getAddress() == null) {
            return new HashMap<>();
        }
        return mappingConfig.getSender().getAddress();
    }

    /**
     * Get all recipient party mappings - FIXED
     */
    public Map<String, FieldMapping> getRecipientPartyMappings() {
        if (mappingConfig.getRecipient() == null || mappingConfig.getRecipient().getParty() == null) {
            return new HashMap<>();
        }
        return mappingConfig.getRecipient().getParty();
    }

    /**
     * Get all recipient address mappings - FIXED
     */
    public Map<String, FieldMapping> getRecipientAddressMappings() {
        if (mappingConfig.getRecipient() == null || mappingConfig.getRecipient().getAddress() == null) {
            return new HashMap<>();
        }
        return mappingConfig.getRecipient().getAddress();
    }

    /**
     * Get transaction mappings - NEW
     */
    public Map<String, FieldMapping> getTransactionMappings() {
        if (mappingConfig.getPayment() == null || mappingConfig.getPayment().getTransaction() == null) {
            return new HashMap<>();
        }
        return mappingConfig.getPayment().getTransaction();
    }

    /**
     * Get transaction detail mappings - NEW
     */
    public Map<String, FieldMapping> getTransactionDetailMappings() {
        if (mappingConfig.getTransactionDetail() == null || mappingConfig.getTransactionDetail().getDetail() == null) {
            return new HashMap<>();
        }
        return mappingConfig.getTransactionDetail().getDetail();
    }

    /**
     * Extract all values for a given entity type
     */
    public Map<String, Object> extractAllValues(String json, Map<String, FieldMapping> mappings) {
        Map<String, Object> result = new HashMap<>();

        if (mappings == null || mappings.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, FieldMapping> entry : mappings.entrySet()) {
            String fieldName = entry.getKey();
            FieldMapping mapping = entry.getValue();

            try {
                Object value = extractValue(json, mapping);
                if (value != null) {
                    result.put(fieldName, value);
                }
            } catch (Exception e) {
                // Log error but continue processing other fields
                System.err.println("Error extracting field " + fieldName + ": " + e.getMessage());
            }
        }

        return result;
    }
}