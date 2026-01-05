package com.example.TransactionConsumer.service;

import com.example.TransactionConsumer.dto.ErrorResponse;
import com.example.TransactionConsumer.repository.TransactionRepository;
import com.example.TransactionConsumer.validator.PayloadValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private PayloadValidator validator;

    @InjectMocks
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

    // ==================== SUCCESSFUL PROCESSING TESTS ====================

    @Test
    void shouldProcessTransactionSuccessfully() {
        // Arrange
        doNothing().when(validator).validate(anyString());
        doNothing().when(repository).insertAll(anyString());

        // Act
        transactionService.processTransaction(validJson);

        // Assert
        verify(validator, times(1)).validate(validJson);
        verify(repository, times(1)).insertAll(validJson);
    }

    @Test
    void shouldCallValidatorBeforeRepository() {
        // Arrange
        doNothing().when(validator).validate(anyString());
        doNothing().when(repository).insertAll(anyString());

        // Act
        transactionService.processTransaction(validJson);

        // Assert
        var inOrder = inOrder(validator, repository);
        inOrder.verify(validator).validate(validJson);
        inOrder.verify(repository).insertAll(validJson);
    }

    // ==================== VALIDATION FAILURE TESTS ====================

    @Test
    void shouldThrowErrorWhenValidationFails() {
        // Arrange
        doThrow(new ErrorResponse("transactionId is required"))
                .when(validator).validate(anyString());

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> transactionService.processTransaction(validJson)
        );

        assertEquals("transactionId is required", exception.getReason());
        verify(validator, times(1)).validate(validJson);
        verify(repository, never()).insertAll(anyString());
    }

    @Test
    void shouldNotCallRepositoryWhenValidationFails() {
        // Arrange
        doThrow(new ErrorResponse("Invalid payload"))
                .when(validator).validate(anyString());

        // Act & Assert
        assertThrows(
                ErrorResponse.class,
                () -> transactionService.processTransaction(validJson)
        );

        verify(repository, never()).insertAll(anyString());
    }

    // ==================== REPOSITORY FAILURE TESTS ====================

    @Test
    void shouldPropagateRepositoryException() {
        // Arrange
        doNothing().when(validator).validate(anyString());
        doThrow(new RuntimeException("Database error"))
                .when(repository).insertAll(anyString());

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> transactionService.processTransaction(validJson)
        );

        assertEquals("Database error", exception.getMessage());
        verify(validator, times(1)).validate(validJson);
        verify(repository, times(1)).insertAll(validJson);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void shouldHandleNullJson() {
        // Arrange
        doThrow(new ErrorResponse("Invalid JSON"))
                .when(validator).validate(null);

        // Act & Assert
        assertThrows(
                ErrorResponse.class,
                () -> transactionService.processTransaction(null)
        );

        verify(validator, times(1)).validate(null);
        verify(repository, never()).insertAll(anyString());
    }

    @Test
    void shouldHandleEmptyJson() {
        // Arrange
        String emptyJson = "";
        doThrow(new ErrorResponse("transactionId is required"))
                .when(validator).validate(emptyJson);

        // Act & Assert
        assertThrows(
                ErrorResponse.class,
                () -> transactionService.processTransaction(emptyJson)
        );

        verify(validator, times(1)).validate(emptyJson);
        verify(repository, never()).insertAll(anyString());
    }

    @Test
    void shouldProcessTransactionWithCompletePayload() {
        // Arrange
        String completeJson = """
            {
              "transactionId": "TXN1101",
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": "INR",
              "customerReferenceNumber": "CRN001",
              "originatingInstitution": "ICICI Bank",
              "transferAcceptorName": "XYZ Services",
              "paymentReference": "PMT-001",
              "fundingSource": "BANK",
              "paymentType": "IMPS",
              "sender": {
                "firstName": "Aadhish",
                "lastName": "M",
                "email": "aadhish@example.com",
                "phone": "9876543210"
              },
              "recipient": {
                "firstName": "Raj",
                "lastName": "K",
                "email": "raj@example.com",
                "phone": "9876501234"
              }
            }
            """;

        doNothing().when(validator).validate(anyString());
        doNothing().when(repository).insertAll(anyString());

        // Act
        transactionService.processTransaction(completeJson);

        // Assert
        verify(validator, times(1)).validate(completeJson);
        verify(repository, times(1)).insertAll(completeJson);
    }

    // ==================== TRANSACTIONAL BEHAVIOR TESTS ====================

    @Test
    void shouldBeAnnotatedWithTransactional() {
        // This test verifies the @Transactional annotation exists
        var method = assertDoesNotThrow(() ->
                TransactionService.class.getMethod("processTransaction", String.class)
        );

        assertNotNull(method);
        assertTrue(
                method.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)
        );
    }
}