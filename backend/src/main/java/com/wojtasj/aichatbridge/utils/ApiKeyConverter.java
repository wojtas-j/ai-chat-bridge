package com.wojtasj.aichatbridge.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * JPA converter for encrypting and decrypting the apiKey field in UserEntity.
 * @since 1.0
 */
@Converter
@RequiredArgsConstructor
public class ApiKeyConverter implements AttributeConverter<String, String> {

    private final TextEncryptor textEncryptor;

    /**
     * Encrypts the apiKey before storing it in the database.
     * @param apiKey the plaintext API key
     * @return the encrypted API key
     */
    @Override
    public String convertToDatabaseColumn(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return apiKey;
        }
        return textEncryptor.encrypt(apiKey);
    }

    /**
     * Decrypts the apiKey after retrieving it from the database.
     * @param encryptedApiKey the encrypted API key
     * @return the plaintext API key
     */
    @Override
    public String convertToEntityAttribute(String encryptedApiKey) {
        if (encryptedApiKey == null || encryptedApiKey.trim().isEmpty()) {
            return encryptedApiKey;
        }
        return textEncryptor.decrypt(encryptedApiKey);
    }
}
