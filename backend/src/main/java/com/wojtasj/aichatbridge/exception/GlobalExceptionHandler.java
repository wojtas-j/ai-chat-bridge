package com.wojtasj.aichatbridge.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles application-specific exceptions in the AI Chat Bridge application, returning responses in RFC 7807 Problem Details format.
 * @since 1.0
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles {@link OpenAIServiceException} by returning a problem details response.
     * @param ex the OpenAI service exception
     * @return a ResponseEntity containing problem details with HTTP status 500
     * @since 1.0
     */
    @ExceptionHandler(OpenAIServiceException.class)
    public ResponseEntity<Map<String, Object>> handleOpenAIServiceException(OpenAIServiceException ex) {
        log.error("OpenAI error: {}", ex.getMessage(), ex);
        return buildProblemDetailsResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "OpenAI Service Error",
                "Failed to process OpenAI request",
                "/problems/openai-service-error"
        );
    }

    /**
     * Handles {@link OpenAIServiceException} by returning a problem details response.
     * @param ex the OpenAI service exception
     * @return a ResponseEntity containing problem details with HTTP status 500
     * @since 1.0
     */
    @ExceptionHandler(DiscordServiceException.class)
    public ResponseEntity<Map<String, Object>> handleDiscordServiceException(DiscordServiceException ex) {
        log.error("Discord error: {}", ex.getMessage(), ex);
        return buildProblemDetailsResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Discord Service Error",
                "Failed to process Discord request",
                "/problems/discord-service-error"
        );
    }

    /**
     * Handles {@link HttpMessageNotReadableException} for malformed JSON or unrecognized fields.
     * @param ex the HTTP message not readable exception
     * @return a ResponseEntity containing problem details with HTTP status 400
     * @since 1.0
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof UnrecognizedPropertyException unrecognized) {
            String fieldName = unrecognized.getPropertyName();
            log.error("Unknown field in request: {}", fieldName, ex);
            return buildProblemDetailsResponse(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Request",
                    "Unknown field: " + fieldName,
                    "/problems/invalid-request"
            );
        }

        log.error("Malformed JSON request", ex);
        return buildProblemDetailsResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Request",
                "Malformed JSON request",
                "/problems/malformed-json"
        );
    }

    /**
     * Handles {@link MethodArgumentNotValidException} for validation errors in request data.
     * @param ex the validation exception
     * @return a ResponseEntity containing problem details with HTTP status 400
     * @since 1.0
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.error("Validation error: {}", errorMessage, ex);
        return buildProblemDetailsResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                errorMessage.isEmpty() ? "Validation failed" : errorMessage,
                "/problems/validation-error"
        );
    }

    /**
     * Handles {@link ResponseStatusException} for HTTP status errors.
     * @param ex the response status exception
     * @return a ResponseEntity containing problem details with the corresponding HTTP status
     * @since 1.0
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        log.error("Response status error: {}", ex.getReason(), ex);
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return buildProblemDetailsResponse(
                status,
                status.getReasonPhrase(),
                ex.getReason() != null ? ex.getReason() : "Internal server error",
                "/problems/response-status-error"
        );
    }

    /**
     * Handles unexpected exceptions not caught by specific handlers.
     * @param ex the generic exception
     * @return a ResponseEntity containing problem details with HTTP status 500
     * @since 1.0
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildProblemDetailsResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Unexpected error: " + ex.getMessage(),
                "/problems/internal-server-error"
        );
    }

    /**
     * Builds a problem details response in RFC 7807 format.
     * @param status the HTTP status code
     * @param title the title of the error
     * @param detail the detailed error message
     * @param type the URI identifying the error type
     * @return a ResponseEntity containing the problem details
     * @since 1.0
     */
    private ResponseEntity<Map<String, Object>> buildProblemDetailsResponse(HttpStatus status, String title, String detail, String type) {
        Map<String, Object> problemDetails = new HashMap<>();
        problemDetails.put("type", type);
        problemDetails.put("title", title);
        problemDetails.put("status", status.value());
        problemDetails.put("detail", detail);
        return new ResponseEntity<>(problemDetails, status);
    }
}
