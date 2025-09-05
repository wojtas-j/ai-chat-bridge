package com.wojtasj.aichatbridge.exception;

import com.wojtasj.aichatbridge.service.OpenAIServiceImpl;

/**
 * Exception thrown when an error occurs in the {@link OpenAIServiceImpl}
 */
public class OpenAIServiceException extends RuntimeException{
    public OpenAIServiceException(String message) {
      super(message);
    }

    public OpenAIServiceException(String message, Throwable cause) {
      super(message, cause);
    }
}