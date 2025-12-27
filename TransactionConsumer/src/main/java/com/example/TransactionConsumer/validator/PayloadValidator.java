package com.example.TransactionConsumer.validator;

import com.example.TransactionConsumer.dto.ErrorResponse;
import com.jayway.jsonpath.JsonPath;
import org.springframework.stereotype.Component;

@Component
public class PayloadValidator {

    public void validate(String json) {
        validateField(json, "$.transactionId", "transactionId");
        validateField(json, "$.transactionType", "transactionType");
        validateField(json, "$.amount", "amount");
        validateField(json, "$.currency", "currency");
    }

    private void validateField(String json, String path, String fieldName) {
        try {
            Object value = JsonPath.read(json, path);
            if (value == null || value.toString().isBlank()) {
                throw new ErrorResponse(fieldName + " is required");
            }
        } catch (Exception ex) {
            throw new ErrorResponse(fieldName + " is missing or invalid");
        }
    }
}