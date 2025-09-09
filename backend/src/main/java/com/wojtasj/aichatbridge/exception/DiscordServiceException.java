package com.wojtasj.aichatbridge.exception;

/**
 * Thrown when an error occurs during Discord bot operations in the AI Chat Bridge application.
 * @since 1.0
 * @see com.wojtasj.aichatbridge.service.DiscordBotServiceImpl
 */
public class DiscordServiceException extends RuntimeException {
    /**
     * Constructs a new DiscordServiceException with the specified message.
     * @param message the error message describing the issue
     */
    public DiscordServiceException(String message) {
        super(message);
    }

    /**
     * Constructs a new DiscordServiceException with the specified message and cause.
     * @param message the error message describing the issue
     * @param cause the underlying cause of the error
     */
    public DiscordServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
