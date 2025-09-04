package com.wojtasj.aichatbridge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for message content in the AI Chat Bridge application.
 */
public record MessageDTO(@NotBlank String content) {}
