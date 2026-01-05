package com.example.TransactionConsumer.controller;

import com.example.TransactionConsumer.dto.ErrorResponse;
import com.example.TransactionConsumer.exception.GlobalExceptionHandler;
import com.example.TransactionConsumer.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    private String validJson;

    @BeforeEach
    void setUp() {
        validJson = """
            {
              "transactionId": "TXN1101",
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": "INR"
            }
            """;
    }

    // ==================== SUCCESS ====================

    @Test
    void shouldCreateTransactionSuccessfully() throws Exception {
        doNothing().when(transactionService).processTransaction(anyString());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isCreated())
                .andExpect(content().string("Transaction Created Successfully"));

        verify(transactionService, times(1)).processTransaction(anyString());
    }

    // ==================== VALIDATION ERRORS ====================

    @Test
    void shouldReturnBadRequestWhenTransactionIdMissing() throws Exception {
        String invalidJson = """
            {
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": "INR"
            }
            """;

        doThrow(new ErrorResponse("transactionId is required"))
                .when(transactionService).processTransaction(anyString());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("transactionId is required"));
    }

    @Test
    void shouldReturnBadRequestWhenTransactionTypeMissing() throws Exception {
        String invalidJson = """
            {
              "transactionId": "TXN1101",
              "amount": 500,
              "currency": "INR"
            }
            """;

        doThrow(new ErrorResponse("transactionType is required"))
                .when(transactionService).processTransaction(anyString());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("transactionType is required"));
    }

    @Test
    void shouldHandleNullValues() throws Exception {
        String jsonWithNulls = """
            {
              "transactionId": null,
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": "INR"
            }
            """;

        doThrow(new ErrorResponse("transactionId is required"))
                .when(transactionService).processTransaction(anyString());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithNulls))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("transactionId is required"));
    }

    // ==================== MALFORMED JSON ====================

    @Test
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        String malformedJson = "{ invalid json }";

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).processTransaction(anyString());
    }

    // ==================== CONTENT TYPE ====================

    @Test
    void shouldReturnUnsupportedMediaTypeForText() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(validJson))
                .andExpect(status().isUnsupportedMediaType());

        verify(transactionService, never()).processTransaction(anyString());
    }

    @Test
    void shouldReturnUnsupportedMediaTypeForXml() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<transaction></transaction>"))
                .andExpect(status().isUnsupportedMediaType());

        verify(transactionService, never()).processTransaction(anyString());
    }

    // ==================== COMPLETE PAYLOAD ====================

    @Test
    void shouldProcessCompleteTransactionPayload() throws Exception {
        String completeJson = """
            {
              "transactionId": "TXN1101",
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": "INR",
              "customerReferenceNumber": "CRN001",
              "sender": {
                "firstName": "Aadhish"
              },
              "recipient": {
                "firstName": "Raj"
              }
            }
            """;

        doNothing().when(transactionService).processTransaction(anyString());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeJson))
                .andExpect(status().isCreated());

        verify(transactionService, times(1)).processTransaction(anyString());
    }
}
