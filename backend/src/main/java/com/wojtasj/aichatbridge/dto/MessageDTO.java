package com.wojtasj.aichatbridge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A data transfer object (DTO) for representing message content in the AI Chat Bridge application, used in REST API requests.
 * @param content the content of the message, must not be blank
 * @since 1.0
 * @see com.wojtasj.aichatbridge.entity.MessageEntity
 */
public record MessageDTO(
        @NotBlank(message = "Content cannot be blank")
        String content) {}
