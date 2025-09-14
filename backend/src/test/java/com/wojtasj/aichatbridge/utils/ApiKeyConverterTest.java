package com.wojtasj.aichatbridge.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ApiKeyConverter} in the AI Chat Bridge application.
 * @since 1.0
 */
class ApiKeyConverterTest {

    private static final String PLAIN_API_KEY = "sk-test-1234567890";
    private static final String ENCRYPTED_API_KEY = "encrypted-api-key";

    private TextEncryptor textEncryptor;
    private ApiKeyConverter apiKeyConverter;

    /**
     * Sets up the test environment with mock TextEncryptor and ApiKeyConverter.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        textEncryptor = mock(TextEncryptor.class);
        apiKeyConverter = new ApiKeyConverter(textEncryptor);
    }

    /**
     * Tests successful encryption of an API key.
     * @since 1.0
     */
    @Test
    void shouldEncryptApiKeySuccessfully() {
        // Arrange
        when(textEncryptor.encrypt(PLAIN_API_KEY)).thenReturn(ENCRYPTED_API_KEY);

        // Act
        String result = apiKeyConverter.convertToDatabaseColumn(PLAIN_API_KEY);

        // Assert
        assertEquals(ENCRYPTED_API_KEY, result);
        verify(textEncryptor).encrypt(PLAIN_API_KEY);
    }

    /**
     * Tests returning null when the API key is null.
     * @since 1.0
     */
    @Test
    void shouldReturnNullWhenApiKeyIsNull() {
        // Arrange & Act
        String result = apiKeyConverter.convertToDatabaseColumn(null);

        // Assert
        assertNull(result);
        verify(textEncryptor, never()).encrypt(any());
    }

    /**
     * Tests returning an empty string when the API key is empty.
     * @since 1.0
     */
    @Test
    void shouldReturnEmptyStringWhenApiKeyIsEmpty() {
        // Arrange & Act
        String result = apiKeyConverter.convertToDatabaseColumn("");

        // Assert
        assertEquals("", result);
        verify(textEncryptor, never()).encrypt(any());
    }

    /**
     * Tests successful decryption of an encrypted API key.
     * @since 1.0
     */
    @Test
    void shouldDecryptApiKeySuccessfully() {
        // Arrange
        when(textEncryptor.decrypt(ENCRYPTED_API_KEY)).thenReturn(PLAIN_API_KEY);

        // Act
        String result = apiKeyConverter.convertToEntityAttribute(ENCRYPTED_API_KEY);

        // Assert
        assertEquals(PLAIN_API_KEY, result);
        verify(textEncryptor).decrypt(ENCRYPTED_API_KEY);
    }

    /**
     * Tests returning null when the encrypted API key is null.
     * @since 1.0
     */
    @Test
    void shouldReturnNullWhenEncryptedApiKeyIsNull() {
        // Arrange & Act
        String result = apiKeyConverter.convertToEntityAttribute(null);

        // Assert
        assertNull(result);
        verify(textEncryptor, never()).decrypt(any());
    }

    /**
     * Tests returning an empty string when the encrypted API key is empty.
     * @since 1.0
     */
    @Test
    void shouldReturnEmptyStringWhenEncryptedApiKeyIsEmpty() {
        // Arrange & Act
        String result = apiKeyConverter.convertToEntityAttribute("");

        // Assert
        assertEquals("", result);
        verify(textEncryptor, never()).decrypt(any());
    }
}
