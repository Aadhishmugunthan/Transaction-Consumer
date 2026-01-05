package com.example.TransactionConsumer.service;

import com.example.TransactionConsumer.config.EntityMapping;
import com.example.TransactionConsumer.config.FieldMapping;
import com.example.TransactionConsumer.config.TransactionMappingConfig;
import com.example.TransactionConsumer.config.ValidationRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MappingServiceTest {

    @Mock
    private TransactionMappingConfig mappingConfig;

    @Mock
    private EntityMapping senderEntityMapping;

    @Mock
    private EntityMapping recipientEntityMapping;

    @InjectMocks
    private MappingService mappingService;

    private String sampleJson;

    @BeforeEach
    void setUp() {
        sampleJson = """
            {
              "transactionId": "TXN1101",
              "amount": 500,
              "currency": "INR",
              "sender": {
                "firstName": "Aadhish",
                "email": "test@example.com"
              }
            }
            """;
    }

    // ==================== JSON PATH EXTRACTION TESTS ====================

    @Test
    void shouldExtractValueFromJsonPath() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.transactionId");

        // Act
        Object result = mappingService.extractValue(sampleJson, mapping);

        // Assert
        assertNotNull(result);
        assertEquals("TXN1101", result);
    }

    @Test
    void shouldExtractNestedJsonPath() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.sender.firstName");

        // Act
        Object result = mappingService.extractValue(sampleJson, mapping);

        // Assert
        assertEquals("Aadhish", result);
    }

    @Test
    void shouldReturnDefaultValueWhenPathNotFound() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.nonExistentField");
        mapping.setRequired(false);
        mapping.setDefaultValue("DEFAULT_VALUE");

        // Act
        Object result = mappingService.extractValue(sampleJson, mapping);

        // Assert
        assertEquals("DEFAULT_VALUE", result);
    }

    @Test
    void shouldThrowExceptionWhenRequiredFieldMissing() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.missingField");
        mapping.setRequired(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mappingService.extractValue(sampleJson, mapping)
        );
        assertTrue(exception.getMessage().contains("Missing required field"));
    }

    // ==================== CONSTANT SOURCE TESTS ====================

    @Test
    void shouldReturnConstantValue() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("constant");
        mapping.setValue("CONSTANT_VALUE");

        // Act
        Object result = mappingService.extractValue(sampleJson, mapping);

        // Assert
        assertEquals("CONSTANT_VALUE", result);
    }

    // ==================== GENERATED SOURCE TESTS ====================

    @Test
    void shouldGenerateUUID() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("generated");

        // Act
        Object result = mappingService.extractValue(sampleJson, mapping);

        // Assert
        assertNotNull(result);
        assertTrue(result.toString().matches(
                "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"
        ));
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    void shouldValidateAllowedValues() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.currency");

        ValidationRules rules = new ValidationRules();
        rules.setAllowed(List.of("INR", "USD", "EUR"));
        mapping.setValidation(rules);

        // Act & Assert - Valid value
        assertDoesNotThrow(() -> mappingService.extractValue(sampleJson, mapping));
    }

    @Test
    void shouldThrowExceptionForInvalidAllowedValue() {
        // Arrange
        String invalidJson = """
            {
              "currency": "XYZ"
            }
            """;

        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.currency");

        ValidationRules rules = new ValidationRules();
        rules.setAllowed(List.of("INR", "USD", "EUR"));
        mapping.setValidation(rules);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mappingService.extractValue(invalidJson, mapping)
        );
        assertTrue(exception.getMessage().contains("Value not allowed"));
    }

    @Test
    void shouldValidatePattern() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.sender.email");

        ValidationRules rules = new ValidationRules();
        rules.setPattern("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        mapping.setValidation(rules);

        // Act & Assert
        assertDoesNotThrow(() -> mappingService.extractValue(sampleJson, mapping));
    }

    @Test
    void shouldThrowExceptionForInvalidPattern() {
        // Arrange
        String invalidJson = """
            {
              "email": "invalid-email"
            }
            """;

        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.email");

        ValidationRules rules = new ValidationRules();
        rules.setPattern("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        mapping.setValidation(rules);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mappingService.extractValue(invalidJson, mapping)
        );
        assertTrue(exception.getMessage().contains("Invalid format"));
    }

    @Test
    void shouldValidateMaxLength() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.transactionId");

        ValidationRules rules = new ValidationRules();
        rules.setMaxLength(10);
        mapping.setValidation(rules);

        // Act & Assert
        assertDoesNotThrow(() -> mappingService.extractValue(sampleJson, mapping));
    }

    @Test
    void shouldThrowExceptionForExceededMaxLength() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.transactionId");

        ValidationRules rules = new ValidationRules();
        rules.setMaxLength(5);
        mapping.setValidation(rules);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mappingService.extractValue(sampleJson, mapping)
        );
        assertTrue(exception.getMessage().contains("Value too long"));
    }

    @Test
    void shouldValidateMinValue() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.amount");

        ValidationRules rules = new ValidationRules();
        rules.setMin(100);
        mapping.setValidation(rules);

        // Act & Assert
        assertDoesNotThrow(() -> mappingService.extractValue(sampleJson, mapping));
    }

    @Test
    void shouldThrowExceptionForBelowMinValue() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.amount");

        ValidationRules rules = new ValidationRules();
        rules.setMin(1000);
        mapping.setValidation(rules);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mappingService.extractValue(sampleJson, mapping)
        );
        assertTrue(exception.getMessage().contains("less than minimum"));
    }

    @Test
    void shouldValidateMaxValue() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.amount");

        ValidationRules rules = new ValidationRules();
        rules.setMax(1000);
        mapping.setValidation(rules);

        // Act & Assert
        assertDoesNotThrow(() -> mappingService.extractValue(sampleJson, mapping));
    }

    @Test
    void shouldThrowExceptionForAboveMaxValue() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.amount");

        ValidationRules rules = new ValidationRules();
        rules.setMax(100);
        mapping.setValidation(rules);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mappingService.extractValue(sampleJson, mapping)
        );
        assertTrue(exception.getMessage().contains("more than maximum"));
    }

    // ==================== EXTRACT ALL VALUES TESTS ====================

    @Test
    void shouldExtractAllValuesSuccessfully() {
        // Arrange
        Map<String, FieldMapping> mappings = new HashMap<>();

        FieldMapping idMapping = new FieldMapping();
        idMapping.setSource("json");
        idMapping.setPath("$.transactionId");
        mappings.put("ID", idMapping);

        FieldMapping amountMapping = new FieldMapping();
        amountMapping.setSource("json");
        amountMapping.setPath("$.amount");
        mappings.put("AMOUNT", amountMapping);

        // Act
        Map<String, Object> result = mappingService.extractAllValues(sampleJson, mappings);

        // Assert
        assertEquals(2, result.size());
        assertEquals("TXN1101", result.get("ID"));
        assertEquals(500, result.get("AMOUNT"));
    }

    @Test
    void shouldSkipNullValuesInExtractAll() {
        // Arrange
        Map<String, FieldMapping> mappings = new HashMap<>();

        FieldMapping validMapping = new FieldMapping();
        validMapping.setSource("json");
        validMapping.setPath("$.transactionId");
        mappings.put("ID", validMapping);

        FieldMapping invalidMapping = new FieldMapping();
        invalidMapping.setSource("json");
        invalidMapping.setPath("$.nonExistent");
        invalidMapping.setRequired(false);
        mappings.put("MISSING", invalidMapping);

        // Act
        Map<String, Object> result = mappingService.extractAllValues(sampleJson, mappings);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.containsKey("ID"));
        assertFalse(result.containsKey("MISSING"));
    }

    @Test
    void shouldHandleEmptyMappings() {
        // Act
        Map<String, Object> result = mappingService.extractAllValues(sampleJson, new HashMap<>());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleNullMappings() {
        // Act
        Map<String, Object> result = mappingService.extractAllValues(sampleJson, null);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ==================== MAPPING GETTER TESTS ====================

    @Test
    void shouldGetSenderPartyMappings() {
        // Arrange
        Map<String, FieldMapping> expectedMappings = new HashMap<>();
        when(mappingConfig.getSender()).thenReturn(senderEntityMapping);
        when(senderEntityMapping.getParty()).thenReturn(expectedMappings);

        // Act
        Map<String, FieldMapping> result = mappingService.getSenderPartyMappings();

        // Assert
        assertNotNull(result);
        verify(mappingConfig, times(1)).getSender();
        verify(senderEntityMapping, times(1)).getParty();
    }

    @Test
    void shouldGetRecipientPartyMappings() {
        // Arrange
        Map<String, FieldMapping> expectedMappings = new HashMap<>();
        when(mappingConfig.getRecipient()).thenReturn(recipientEntityMapping);
        when(recipientEntityMapping.getParty()).thenReturn(expectedMappings);

        // Act
        Map<String, FieldMapping> result = mappingService.getRecipientPartyMappings();

        // Assert
        assertNotNull(result);
        verify(mappingConfig, times(1)).getRecipient();
        verify(recipientEntityMapping, times(1)).getParty();
    }

    @Test
    void shouldGetSenderAddressMappings() {
        // Arrange
        Map<String, Map<String, FieldMapping>> addressMappings = new HashMap<>();
        Map<String, FieldMapping> senderAddress = new HashMap<>();
        addressMappings.put("sender", senderAddress);
        when(mappingConfig.getAddress()).thenReturn(addressMappings);

        // Act
        Map<String, FieldMapping> result = mappingService.getSenderAddressMappings();

        // Assert
        assertNotNull(result);
        assertEquals(senderAddress, result);
    }

    @Test
    void shouldReturnEmptyMapWhenSenderAddressMappingNotFound() {
        // Arrange
        Map<String, Map<String, FieldMapping>> addressMappings = new HashMap<>();
        when(mappingConfig.getAddress()).thenReturn(addressMappings);

        // Act
        Map<String, FieldMapping> result = mappingService.getSenderAddressMappings();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGetRecipientAddressMappings() {
        // Arrange
        Map<String, Map<String, FieldMapping>> addressMappings = new HashMap<>();
        Map<String, FieldMapping> recipientAddress = new HashMap<>();
        addressMappings.put("recipient", recipientAddress);
        when(mappingConfig.getAddress()).thenReturn(addressMappings);

        // Act
        Map<String, FieldMapping> result = mappingService.getRecipientAddressMappings();

        // Assert
        assertNotNull(result);
        assertEquals(recipientAddress, result);
    }

    @Test
    void shouldReturnEmptyMapWhenRecipientAddressMappingNotFound() {
        // Arrange
        Map<String, Map<String, FieldMapping>> addressMappings = new HashMap<>();
        when(mappingConfig.getAddress()).thenReturn(addressMappings);

        // Act
        Map<String, FieldMapping> result = mappingService.getRecipientAddressMappings();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void shouldHandleNullMapping() {
        // Act
        Object result = mappingService.extractValue(sampleJson, null);

        // Assert
        assertNull(result);
    }

    @Test
    void shouldThrowExceptionForUnknownSource() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("unknown");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mappingService.extractValue(sampleJson, mapping)
        );
        assertTrue(exception.getMessage().contains("Unknown source"));
    }

    @Test
    void shouldHandleNullValidationRules() {
        // Arrange
        FieldMapping mapping = new FieldMapping();
        mapping.setSource("json");
        mapping.setPath("$.transactionId");
        mapping.setValidation(null);

        // Act & Assert
        assertDoesNotThrow(() -> mappingService.extractValue(sampleJson, mapping));
    }
}