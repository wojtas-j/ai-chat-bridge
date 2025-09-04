package com.wojtasj.aichatbridge.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global exception handler for handling application-specific exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(OpenAIServiceException.class)
    public ResponseStatusException handleOpenAIServiceException(OpenAIServiceException ex) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process OpenAI request", ex);
    }

    @ExceptionHandler(DiscordServiceException.class)
    public ResponseStatusException handleDiscordServiceException(DiscordServiceException ex) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process Discord request", ex);
    }

    @ExceptionHandler(UnrecognizedPropertyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> handleUnknownJsonProperty(UnrecognizedPropertyException ex) {
        return ResponseEntity.badRequest().body("Unknown field in request: " + ex.getPropertyName());
    }
}
