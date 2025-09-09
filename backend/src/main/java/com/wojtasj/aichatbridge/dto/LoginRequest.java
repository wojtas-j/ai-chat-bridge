package com.wojtasj.aichatbridge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for user login requests in the AI Chat Bridge application.
 * @param username the username or email for login
 * @param password the password for login
 * @since 1.0
 */
public record LoginRequest(
        @NotBlank(message = "Username or email cannot be blank")
        String username,

        @NotBlank(message = "Password cannot be blank")
        String password) {
}
