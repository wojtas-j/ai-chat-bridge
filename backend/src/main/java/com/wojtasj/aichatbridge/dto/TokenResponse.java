package com.wojtasj.aichatbridge.dto;

/**
 * DTO for authentication response containing access and refresh tokens in the AI Chat Bridge application.
 * @param accessToken the JWT access token
 * @param refreshToken the refresh token
 * @since 1.0
 */
public record TokenResponse(String accessToken, String refreshToken) {
}
