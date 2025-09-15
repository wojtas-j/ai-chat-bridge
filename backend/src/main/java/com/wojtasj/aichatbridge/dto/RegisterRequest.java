package com.wojtasj.aichatbridge.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

/**
 * DTO for user registration requests in the AI Chat Bridge application.
 * @param username the username for the new user
 * @param email the email address for the new user
 * @param password the password for the new user
 * @param apiKey the OpenAI API key for the user
 * @param maxTokens the maximum tokens for OpenAI requests
 * @since 1.0
 */
public record RegisterRequest(
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
        String username,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
        String password,

        @NotBlank(message = "API key cannot be blank")
        String apiKey,

        @NotNull(message = "Max tokens cannot be null")
        @Min(value = 1, message = "Max tokens must be at least 1")
        Integer maxTokens,

        @NotBlank(message = "Model cannot be blank")
        String model) {

        public RegisterRequest(
                String username,
                String email,
                String password,
                String apiKey,
                Integer maxTokens) {
                this(username, email, password, apiKey, maxTokens, "gpt-4o-mini");
        }
}
