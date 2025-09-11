package com.wojtasj.aichatbridge.dto;

/**
 * DTO for user login response in the AI Chat Bridge application.
 * @param token the JWT token for authenticated user
 * @since 1.0
 */
public record LoginResponse(String token) {
}
