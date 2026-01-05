package com.example.TransactionConsumer.controller;

import com.example.TransactionConsumer.service.TransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;
    private final ObjectMapper objectMapper;

    public TransactionController(TransactionService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> create(@RequestBody Map<String, Object> payload)
            throws JsonProcessingException {

        String json = objectMapper.writeValueAsString(payload);
        service.processTransaction(json);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Transaction Created Successfully");
    }
}
