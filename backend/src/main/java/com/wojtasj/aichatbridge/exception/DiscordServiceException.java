package com.wojtasj.aichatbridge.exception;

/**
 * Exception thrown when an error occurs in the Discord service.
 */
public class DiscordServiceException extends RuntimeException {
    public DiscordServiceException(String message) {
        super(message);
    }

    public DiscordServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
