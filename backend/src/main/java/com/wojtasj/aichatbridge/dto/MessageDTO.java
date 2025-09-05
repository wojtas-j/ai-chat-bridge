package com.wojtasj.aichatbridge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for message content in the AI Chat Bridge application for {@link com.wojtasj.aichatbridge.entity.MessageEntity}.
 */
public record MessageDTO(
        @NotBlank(message = "Content cannot be blank")
        String content) {}
