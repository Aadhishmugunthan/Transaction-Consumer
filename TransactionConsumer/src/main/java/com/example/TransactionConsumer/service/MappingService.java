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
     * Extract a single value according to mapping rule
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
     * Extract JSON path with validation + default handling
     */
    private Object extractFromJson(String json, FieldMapping mapping) {
        try {
            Object value = JsonPath.read(json, mapping.getPath());

            validateValue(value, mapping);
            return value;

        } catch (PathNotFoundException e) {
            if (Boolean.TRUE.equals(mapping.getRequired())) {
                throw new IllegalArgumentException("Missing required field: " + mapping.getPath());
            }
            return mapping.getDefaultValue();
        }
    }

    /**
     * Validate field value based on provided rules
     */
    private void validateValue(Object value, FieldMapping mapping) {
        if (value == null || mapping.getValidation() == null) {
            return;
        }

        ValidationRules rules = mapping.getValidation();

        // allowed values
        if (rules.getAllowed() != null && !rules.getAllowed().contains(value.toString())) {
            throw new IllegalArgumentException("Value not allowed: " + value);
        }

        // regex pattern
        if (rules.getPattern() != null && !value.toString().matches(rules.getPattern())) {
            throw new IllegalArgumentException("Invalid format: " + value);
        }

        // max length
        if (rules.getMaxLength() != null && value.toString().length() > rules.getMaxLength()) {
            throw new IllegalArgumentException("Value too long: " + value);
        }

        // numeric validations
        if (value instanceof Number) {
            int num = ((Number) value).intValue();
            if (rules.getMin() != null && num < rules.getMin()) {
                throw new IllegalArgumentException("Value less than minimum");
            }
            if (rules.getMax() != null && num > rules.getMax()) {
                throw new IllegalArgumentException("Value more than maximum");
            }
        }
    }

    /**
     * Extract all values for a given mapping
     */
    public Map<String, Object> extractAllValues(String json, Map<String, FieldMapping> mappings) {
        Map<String, Object> result = new HashMap<>();

        if (mappings == null) return result;

        for (Map.Entry<String, FieldMapping> entry : mappings.entrySet()) {
            try {
                Object value = extractValue(json, entry.getValue());
                if (value != null) {
                    result.put(entry.getKey(), value);
                }
            } catch (Exception ex) {
                System.err.println("Mapping failed for " + entry.getKey() + ": " + ex.getMessage());
            }
        }
        return result;
    }

    // =========================================================
    //           MAPPINGS FETCH METHODS (CODE YOU ASKED)
    // =========================================================

    public Map<String, FieldMapping> getSenderPartyMappings() {
        return mappingConfig.getSender().getParty();
    }

    public Map<String, FieldMapping> getRecipientPartyMappings() {
        return mappingConfig.getRecipient().getParty();
    }

    // ⭐ NEW METHODS EXACTLY FOR BOSS REQUIREMENT ⭐
    public Map<String, FieldMapping> getSenderAddressMappings() {
        return mappingConfig.getAddress()
                .getOrDefault("sender", new HashMap<>());
    }

    public Map<String, FieldMapping> getRecipientAddressMappings() {
        return mappingConfig.getAddress()
                .getOrDefault("recipient", new HashMap<>());
    }
}
