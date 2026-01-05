package com.example.TransactionConsumer.validator;

import com.example.TransactionConsumer.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PayloadValidatorTest {

    @InjectMocks
    private PayloadValidator payloadValidator;

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

    // ==================== SUCCESSFUL VALIDATION TESTS ====================

    @Test
    void shouldValidateCompletePayload() {
        // Act & Assert
        assertDoesNotThrow(() -> payloadValidator.validate(validJson));
    }

    @Test
    void shouldValidatePayloadWithAllFields() {
        // Arrange
        String completeJson = """
            {
              "transactionId": "TXN1101",
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": "INR",
              "customerReferenceNumber": "CRN001",
              "sender": {
                "firstName": "Aadhish"
              }
            }
            """;

        // Act & Assert
        assertDoesNotThrow(() -> payloadValidator.validate(completeJson));
    }

    // ==================== TRANSACTION ID VALIDATION TESTS ====================

    @Test
    void shouldThrowErrorWhenTransactionIdMissing() {
        // Arrange
        String jsonWithoutTransactionId = """
            {
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": "INR"
            }
            """;

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(jsonWithoutTransactionId)
        );

        assertTrue(exception.getReason().contains("transactionId"));
    }

    @Test
    void shouldThrowErrorWhenTransactionIdIsNull() {
        // Arrange
        String jsonWithNullTransactionId = """
            {
              "transactionId": null,
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": "INR"
            }
            """;

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(jsonWithNullTransactionId)
        );

        assertTrue(exception.getReason().contains("transactionId"));
    }

    @Test
    void shouldThrowErrorWhenTransactionIdIsBlank() {
        // Arrange
        String jsonWithBlankTransactionId = """
            {
              "transactionId": "   ",
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": "INR"
            }
            """;

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(jsonWithBlankTransactionId)
        );

        assertTrue(exception.getReason().contains("transactionId"));
    }

    @Test
    void shouldThrowErrorWhenTransactionIdIsEmpty() {
        // Arrange
        String jsonWithEmptyTransactionId = """
            {
              "transactionId": "",
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": "INR"
            }
            """;

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(jsonWithEmptyTransactionId)
        );

        assertTrue(exception.getReason().contains("transactionId"));
    }

    // ==================== TRANSACTION TYPE VALIDATION TESTS ====================

    @Test
    void shouldThrowErrorWhenTransactionTypeMissing() {
        // Arrange
        String jsonWithoutTransactionType = """
            {
              "transactionId": "TXN1101",
              "amount": 500,
              "currency": "INR"
            }
            """;

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(jsonWithoutTransactionType)
        );

        assertTrue(exception.getReason().contains("transactionType"));
    }

    @Test
    void shouldThrowErrorWhenTransactionTypeIsNull() {
        // Arrange
        String jsonWithNullTransactionType = """
            {
              "transactionId": "TXN1101",
              "transactionType": null,
              "amount": 500,
              "currency": "INR"
            }
            """;

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(jsonWithNullTransactionType)
        );

        assertTrue(exception.getReason().contains("transactionType"));
    }

    @Test
    void shouldThrowErrorWhenTransactionTypeIsBlank() {
        // Arrange
        String jsonWithBlankTransactionType = """
            {
              "transactionId": "TXN1101",
              "transactionType": "   ",
              "amount": 500,
              "currency": "INR"
            }
            """;

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(jsonWithBlankTransactionType)
        );

        assertTrue(exception.getReason().contains("transactionType"));
    }

    // ==================== AMOUNT VALIDATION TESTS ====================

    @Test
    void shouldThrowErrorWhenAmountMissing() {
        // Arrange
        String jsonWithoutAmount = """
            {
              "transactionId": "TXN1101",
              "transactionType": "PAYMENT",
              "currency": "INR"
            }
            """;

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(jsonWithoutAmount)
        );

        assertTrue(exception.getReason().contains("amount"));
    }

    @Test
    void shouldThrowErrorWhenAmountIsNull() {
        // Arrange
        String jsonWithNullAmount = """
            {
              "transactionId": "TXN1101",
              "transactionType": "PAYMENT",
              "amount": null,
              "currency": "INR"
            }
            """;

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(jsonWithNullAmount)
        );

        assertTrue(exception.getReason().contains("amount"));
    }

    @Test
    void shouldAcceptZeroAmount() {
        // Arrange
        String jsonWithZeroAmount = """
            {
              "transactionId": "TXN1101",
              "transactionType": "PAYMENT",
              "amount": 0,
              "currency": "INR"
            }
            """;

        // Act & Assert
        assertDoesNotThrow(() -> payloadValidator.validate(jsonWithZeroAmount));
    }

    @Test
    void shouldAcceptDecimalAmount() {
        // Arrange
        String jsonWithDecimalAmount = """
            {
              "transactionId": "TXN1101",
              "transactionType": "PAYMENT",
              "amount": 500.50,
              "currency": "INR"
            }
            """;

        // Act & Assert
        assertDoesNotThrow(() -> payloadValidator.validate(jsonWithDecimalAmount));
    }

    // ==================== CURRENCY VALIDATION TESTS ====================

    @Test
    void shouldThrowErrorWhenCurrencyMissing() {
        // Arrange
        String jsonWithoutCurrency = """
            {
              "transactionId": "TXN1101",
              "transactionType": "PAYMENT",
              "amount": 500
            }
            """;

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(jsonWithoutCurrency)
        );

        assertTrue(exception.getReason().contains("currency"));
    }

    @Test
    void shouldThrowErrorWhenCurrencyIsNull() {
        // Arrange
        String jsonWithNullCurrency = """
            {
              "transactionId": "TXN1101",
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": null
            }
            """;

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(jsonWithNullCurrency)
        );

        assertTrue(exception.getReason().contains("currency"));
    }

    @Test
    void shouldThrowErrorWhenCurrencyIsBlank() {
        // Arrange
        String jsonWithBlankCurrency = """
            {
              "transactionId": "TXN1101",
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": "   "
            }
            """;

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(jsonWithBlankCurrency)
        );

        assertTrue(exception.getReason().contains("currency"));
    }

    // ==================== MALFORMED JSON TESTS ====================

    @Test
    void shouldThrowErrorForMalformedJson() {
        // Arrange
        String malformedJson = "{ invalid json }";

        // Act & Assert
        assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(malformedJson)
        );
    }

    @Test
    void shouldThrowErrorForEmptyJson() {
        // Arrange
        String emptyJson = "";

        // Act & Assert
        assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(emptyJson)
        );
    }

    @Test
    void shouldThrowErrorForEmptyObject() {
        // Arrange
        String emptyObject = "{}";

        // Act & Assert
        assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(emptyObject)
        );
    }

    // ==================== DIFFERENT CURRENCY CODES TESTS ====================

    @Test
    void shouldAcceptUSDCurrency() {
        // Arrange
        String jsonWithUSD = """
            {
              "transactionId": "TXN1101",
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": "USD"
            }
            """;

        // Act & Assert
        assertDoesNotThrow(() -> payloadValidator.validate(jsonWithUSD));
    }

    @Test
    void shouldAcceptEURCurrency() {
        // Arrange
        String jsonWithEUR = """
            {
              "transactionId": "TXN1101",
              "transactionType": "PAYMENT",
              "amount": 500,
              "currency": "EUR"
            }
            """;

        // Act & Assert
        assertDoesNotThrow(() -> payloadValidator.validate(jsonWithEUR));
    }

    // ==================== DIFFERENT TRANSACTION TYPES TESTS ====================

    @Test
    void shouldAcceptTransferTransactionType() {
        // Arrange
        String jsonWithTransfer = """
            {
              "transactionId": "TXN1101",
              "transactionType": "TRANSFER",
              "amount": 500,
              "currency": "INR"
            }
            """;

        // Act & Assert
        assertDoesNotThrow(() -> payloadValidator.validate(jsonWithTransfer));
    }

    @Test
    void shouldAcceptWithdrawalTransactionType() {
        // Arrange
        String jsonWithWithdrawal = """
            {
              "transactionId": "TXN1101",
              "transactionType": "WITHDRAWAL",
              "amount": 500,
              "currency": "INR"
            }
            """;

        // Act & Assert
        assertDoesNotThrow(() -> payloadValidator.validate(jsonWithWithdrawal));
    }

    // ==================== EDGE CASE: MULTIPLE ERRORS ====================

    @Test
    void shouldThrowErrorForFirstMissingField() {
        // Arrange - Missing all required fields
        String jsonWithAllMissing = "{}";

        // Act & Assert
        ErrorResponse exception = assertThrows(
                ErrorResponse.class,
                () -> payloadValidator.validate(jsonWithAllMissing)
        );

        // Should fail on the first field it checks (transactionId)
        assertTrue(exception.getReason().contains("transactionId"));
    }
}