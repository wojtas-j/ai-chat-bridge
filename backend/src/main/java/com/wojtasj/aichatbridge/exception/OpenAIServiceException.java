package com.wojtasj.aichatbridge.exception;

/**
 * Thrown when an error occurs during OpenAI API operations in the AI Chat Bridge application.
 * @since 1.0
 * @see com.wojtasj.aichatbridge.service.OpenAIServiceImpl
 */
public class OpenAIServiceException extends RuntimeException{
    /**
     * Constructs a new OpenAIServiceException with the specified message.
     * @param message the error message describing the issue
     */
    public OpenAIServiceException(String message) {
      super(message);
    }

    /**
     * Constructs a new OpenAIServiceException with the specified message and cause.
     * @param message the error message describing the issue
     * @param cause the underlying cause of the error
     */
    public OpenAIServiceException(String message, Throwable cause) {
      super(message, cause);
    }
}
