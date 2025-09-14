package com.wojtasj.aichatbridge.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;

/**
 * Test configuration providing mock beans for the AI Chat Bridge application.
 * @since 1.0
 */
@TestConfiguration
public class TestBeansConfig {

    /**
     * Provides a no-op TextEncryptor for testing purposes.
     * @return a no-op TextEncryptor instance
     * @since 1.0
     */
    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.noOpText();
    }
}
