package com.wojtasj.aichatbridge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A data transfer object (DTO) for representing message content in the AI Chat Bridge application, used in REST API requests.
 * @param content the content of the message, must not be blank
 * @since 1.0
 * @see com.wojtasj.aichatbridge.entity.MessageEntity
 */
public record MessageDTO(
        @NotBlank(message = "Content cannot be blank")
        @Size(min = 1, max = 5000, message = "Content length must be between {min} and {max} characters")
        String content) {}
