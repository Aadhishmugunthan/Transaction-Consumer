package com.example.TransactionConsumer.service;

import com.example.TransactionConsumer.repository.TransactionRepository;
import com.example.TransactionConsumer.validator.PayloadValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository repo;
    private final PayloadValidator validator;

    public TransactionService(TransactionRepository repo, PayloadValidator validator) {
        this.repo = repo;
        this.validator = validator;
    }

    @Transactional
    public void processTransaction(String json) {
        validator.validate(json);
        repo.insertAll(json);
    }
}