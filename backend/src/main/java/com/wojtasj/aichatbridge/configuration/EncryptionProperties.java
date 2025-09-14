package com.wojtasj.aichatbridge.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for encryption settings in the AI Chat Bridge application.
 * @since 1.0
 */
@Component
@ConfigurationProperties(prefix = "encryption")
@Validated
@Getter
@Setter
public class EncryptionProperties {

    /**
     * The encryption key used for encrypting sensitive data like apiKey.
     */
    @NotBlank(message = "Encryption key cannot be blank")
    private String key;

    /**
     * The salt used for encryption.
     */
    @NotBlank(message = "Encryption salt cannot be blank")
    private String salt;
}
