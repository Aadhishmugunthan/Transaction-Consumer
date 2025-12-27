package com.example.TransactionConsumer.controller;

import com.example.TransactionConsumer.service.TransactionService;
import com.example.TransactionConsumer.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody String payload) {
        try {
            service.processTransaction(payload);
            return ResponseEntity.status(201).body("Transaction Created Successfully");
        } catch (ErrorResponse ex) {
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getReason());
            return ResponseEntity.badRequest().body(error);
        }
    }
}