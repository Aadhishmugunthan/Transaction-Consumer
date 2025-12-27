package com.example.TransactionConsumer.dto;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ErrorResponse extends ResponseStatusException {

    public ErrorResponse(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}