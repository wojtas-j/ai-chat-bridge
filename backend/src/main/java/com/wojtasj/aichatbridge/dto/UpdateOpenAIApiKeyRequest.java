package com.wojtasj.aichatbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for updating user API key requests in the AI Chat Bridge application.
 * @param apiKey the new API key for the user
 * @since 1.0
 */
public record UpdateOpenAIApiKeyRequest(
        @NotBlank(message = "API key cannot be blank")
        @Pattern(regexp = "^sk-[a-zA-Z0-9_-]+$", message = "API key must start with 'sk-' and contain only letters, numbers, underscores, or hyphens")
        String apiKey) {
}
