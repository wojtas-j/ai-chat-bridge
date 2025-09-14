package com.wojtasj.aichatbridge.exception;

/**
 * Exception thrown when a message is not found.
 * @since 1.0
 */
public class MessageNotFoundException extends RuntimeException {
    /**
     * Constructs a new MessageNotFoundException with the specified message.
     * @param message the error message describing the issue
     */
    public MessageNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new MessageNotFoundException with the specified message and cause.
     * @param message the error message describing the issue
     * @param cause the underlying cause of the error
     */
    public MessageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
