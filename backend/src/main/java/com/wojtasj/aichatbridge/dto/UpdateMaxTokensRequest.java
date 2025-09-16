package com.wojtasj.aichatbridge.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for updating user max tokens value requests in the AI Chat Bridge application.
 * @param maxTokens the new max tokens value for the user
 * @since 1.0
 */
public record UpdateMaxTokensRequest(
        @NotNull(message = "Max tokens cannot be null")
        @Min(value = 1, message = "Max tokens must be at least 1")
        @Max(value = 999999, message = "Max tokens cannot exceed 999999")
        Integer maxTokens) {
}
