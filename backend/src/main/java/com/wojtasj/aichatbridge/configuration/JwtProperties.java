package com.wojtasj.aichatbridge.configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for JWT authentication in the AI Chat Bridge application.
 * <p>Properties include:</p>
 * <ul>
 *     <li>{@link #secret} - the secret key used to sign JWT tokens</li>
 *     <li>{@link #expirationMs} - the expiration time for JWT tokens in milliseconds</li>
 * </ul>
 * @since 1.0
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private long expirationMs;

    /**
     * Validates the JWT configuration properties after they are set.
     * <p>
     * This method is automatically called by Spring after the properties are injected.
     * It checks that:
     * <ul>
     *     <li>{@link #secret} is not null and has at least 32 characters</li>
     *     <li>{@link #expirationMs} is positive</li>
     * </ul>
     * If any of these conditions are not met, an {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @throws IllegalArgumentException if the secret is too short or expirationMs is not positive
     * @since 1.0
     */
    @PostConstruct
    public void validate() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters long");
        }
        if (expirationMs <= 0) {
            throw new IllegalArgumentException("JWT expirationMs must be positive");
        }
    }
}
