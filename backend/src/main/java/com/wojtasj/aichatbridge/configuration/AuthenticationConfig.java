package com.wojtasj.aichatbridge.configuration;

import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Configuration for authentication-related beans in the AI Chat Bridge application.
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(EncryptionProperties.class)
@AllArgsConstructor
public class AuthenticationConfig {

    private final EncryptionProperties encryptionProperties;

    /**
     * Provides the AuthenticationManager for authenticating users.
     * @param authenticationConfiguration the authentication configuration
     * @return the AuthenticationManager instance
     * @throws Exception if an error occurs during configuration
     * @since 1.0
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Provides a BCryptPasswordEncoder for password encryption.
     * @return a BCryptPasswordEncoder instance
     * @since 1.0
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provides a TextEncryptor for encrypting/decrypting sensitive data like apiKey.
     * @return a TextEncryptor instance
     * @since 1.0
     */
    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.text(encryptionProperties.getKey(), encryptionProperties.getSalt());
    }
}
