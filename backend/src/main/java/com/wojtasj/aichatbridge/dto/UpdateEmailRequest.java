package com.wojtasj.aichatbridge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for updating user email requests in the AI Chat Bridge application.
 * @param email the new email for the user
 * @since 1.0
 */
public record UpdateEmailRequest(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email must be a valid email address")
        String email) {
}
