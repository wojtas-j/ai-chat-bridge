package com.wojtasj.aichatbridge.exception;

import com.wojtasj.aichatbridge.service.DiscordBotServiceImpl;

/**
 * Exception thrown when an error occurs in the {@link DiscordBotServiceImpl}.
 */
public class DiscordServiceException extends RuntimeException {
    public DiscordServiceException(String message) {
        super(message);
    }

    public DiscordServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
