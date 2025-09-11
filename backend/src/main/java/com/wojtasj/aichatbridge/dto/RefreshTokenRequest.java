package com.wojtasj.aichatbridge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for refresh token request in the AI Chat Bridge application.
 * @param refreshToken the refresh token to validate
 * @since 1.0
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token cannot be blank")
        String refreshToken
) {
}
