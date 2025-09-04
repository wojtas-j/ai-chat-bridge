package com.wojtasj.aichatbridge.exception;

/**
 * Exception thrown when an error occurs in the OpenAI service.
 */
public class OpenAIServiceException extends RuntimeException{
    public OpenAIServiceException(String message) {
      super(message);
    }

    public OpenAIServiceException(String message, Throwable cause) {
      super(message, cause);
    }
}