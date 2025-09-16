package com.wojtasj.aichatbridge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for updating user model requests in the AI Chat Bridge application.
 * @param model the new model for the user
 * @since 1.0
 */
public record UpdateModelRequest(
        @NotBlank(message = "Model cannot be blank")
        String model) {
}
