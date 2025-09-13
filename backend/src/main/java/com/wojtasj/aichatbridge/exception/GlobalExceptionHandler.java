package com.wojtasj.aichatbridge.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
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
     * @param request the HTTP request
     * @return a ResponseEntity containing problem details with HTTP status 500
     * @since 1.0
     */
    @ExceptionHandler(OpenAIServiceException.class)
    public ResponseEntity<Map<String, Object>> handleOpenAIServiceException(OpenAIServiceException ex, HttpServletRequest request) {
        log.error("OpenAI error: {}", ex.getMessage(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex.getMessage().contains("Invalid OpenAI API key")) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex.getMessage().contains("TooManyRequests")) {
            status = HttpStatus.TOO_MANY_REQUESTS;
        }
        return buildProblemDetailsResponse(
                status,
                "OpenAI Service Error",
                ex.getMessage(),
                "/problems/openai-service-error",
                request.getRequestURI()
        );
    }

    /**
     * Handles {@link DiscordServiceException} by returning a problem details response.
     * @param ex the Discord service exception
     * @param request the HTTP request
     * @return a ResponseEntity containing problem details with HTTP status 500
     * @since 1.0
     */
    @ExceptionHandler(DiscordServiceException.class)
    public ResponseEntity<Map<String, Object>> handleDiscordServiceException(DiscordServiceException ex, HttpServletRequest request) {
        log.error("Discord error: {}", ex.getMessage(), ex);
        return buildProblemDetailsResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Discord Service Error",
                "Failed to process Discord request",
                "/problems/discord-service-error",
                request.getRequestURI()
        );
    }

    /**
     * Handles {@link HttpMessageNotReadableException} for malformed JSON or unrecognized fields.
     * @param ex the HTTP message not readable exception
     * @param request the HTTP request
     * @return a ResponseEntity containing problem details with HTTP status 400
     * @since 1.0
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        if (ex.getCause() instanceof UnrecognizedPropertyException unrecognized) {
            String fieldName = unrecognized.getPropertyName();
            log.error("Unknown field in request: {}", fieldName, ex);
            return buildProblemDetailsResponse(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Request",
                    "Unknown field: " + fieldName,
                    "/problems/invalid-request",
                    request.getRequestURI()
            );
        }

        log.error("Malformed JSON request", ex);
        return buildProblemDetailsResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Request",
                "Malformed JSON request",
                "/problems/malformed-json",
                request.getRequestURI()
        );
    }

    /**
     * Handles {@link MethodArgumentNotValidException} which occurs when
     * validation of a request body annotated with {@code @Valid} fails.
     * @param ex the exception containing field errors
     * @param request the HTTP request
     * @return a {@link ResponseEntity} containing problem details with HTTP status 400
     * @since 1.0
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errorMessage = ex.getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.error("Validation error: {}", errorMessage, ex);
        return buildProblemDetailsResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                errorMessage.isEmpty() ? "Validation failed" : errorMessage,
                "/problems/validation-error",
                request.getRequestURI()
        );
    }

    /**
     * Handles {@link ConstraintViolationException} which occurs when
     * validation of method parameters, path variables, or request parameters fails.
     * @param ex the exception containing constraint violations
     * @param request the HTTP request
     * @return a {@link ResponseEntity} containing problem details with HTTP status 400
     * @since 1.0
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining("; "));
        log.error("Constraint violation error: {}", errorMessage, ex);
        return buildProblemDetailsResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                errorMessage.isEmpty() ? "Validation failed" : errorMessage,
                "/problems/validation-error",
                request.getRequestURI()
        );
    }

    /**
     * Handles {@link BadCredentialsException} for invalid authentication credentials.
     * @param ex the bad credentials exception
     * @param request the HTTP request
     * @return a ResponseEntity containing problem details with HTTP status 401
     * @since 1.0
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        log.error("Authentication error: {}", ex.getMessage(), ex);
        return buildProblemDetailsResponse(
                HttpStatus.UNAUTHORIZED,
                "Authentication Failed",
                "Invalid username or password",
                "/problems/authentication-failed",
                request.getRequestURI()
        );
    }

    /**
     * Handles {@link AccessDeniedException} for unauthorized access attempts.
     * @param ex the custom access denied exception
     * @param request the HTTP request
     * @return a ResponseEntity containing problem details with HTTP status 403
     * @since 1.0
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleCustomAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.error("Access denied: {}", ex.getMessage(), ex);
        return buildProblemDetailsResponse(
                HttpStatus.FORBIDDEN,
                "Access Denied",
                ex.getMessage(),
                "/problems/access-denied",
                request.getRequestURI()
        );
    }

    /**
     * Handles {@link AuthenticationException} for authentication-related errors, including {@link UserAlreadyExistsException}.
     * @param ex the authentication exception
     * @param request the HTTP request
     * @return a ResponseEntity containing problem details with HTTP status 401 or 409
     * @since 1.0
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.error("Authentication error: {}", ex.getMessage(), ex);
        HttpStatus status = ex instanceof UserAlreadyExistsException ? HttpStatus.CONFLICT : HttpStatus.UNAUTHORIZED;
        String title = ex instanceof UserAlreadyExistsException ? "Registration Failed" : "Authentication Failed";
        String type = ex instanceof UserAlreadyExistsException ? "/problems/registration-failed" : "/problems/authentication-failed";
        return buildProblemDetailsResponse(
                status,
                title,
                ex.getMessage(),
                type,
                request.getRequestURI()
        );
    }

    /**
     * Handles {@link org.springframework.security.access.AccessDeniedException} for unauthorized access attempts.
     * @param ex the Spring Security access denied exception
     * @param request the HTTP request
     * @return a ResponseEntity containing problem details with HTTP status 403
     * @since 1.0
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        log.error("Access denied: {}", ex.getMessage(), ex);
        return buildProblemDetailsResponse(
                HttpStatus.FORBIDDEN,
                "Access Denied",
                "You do not have permission to access this resource",
                "/problems/access-denied",
                request.getRequestURI()
        );
    }

    /**
     * Handles {@link ResponseStatusException} for HTTP status errors.
     * @param ex the response status exception
     * @param request the HTTP request
     * @return a ResponseEntity containing problem details with the corresponding HTTP status
     * @since 1.0
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        log.error("Response status error: {}", ex.getReason(), ex);
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return buildProblemDetailsResponse(
                status,
                status.getReasonPhrase(),
                ex.getReason() != null ? ex.getReason() : "Internal server error",
                "/problems/response-status-error",
                request.getRequestURI()
        );
    }

    /**
     * Handles unexpected exceptions not caught by specific handlers.
     * @param ex the generic exception
     * @param request the HTTP request
     * @return a ResponseEntity containing problem details with HTTP status 500
     * @since 1.0
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildProblemDetailsResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Unexpected error: " + ex.getMessage(),
                "/problems/internal-server-error",
                request.getRequestURI()
        );
    }

    /**
     * Builds a problem details response in RFC 7807 format.
     * @param status the HTTP status code
     * @param title the title of the error
     * @param detail the detailed error message
     * @param type the URI identifying the error type
     * @param instance the URI of the request causing the error
     * @return a ResponseEntity containing the problem details
     * @since 1.0
     */
    private ResponseEntity<Map<String, Object>> buildProblemDetailsResponse(HttpStatus status, String title, String detail, String type, String instance) {
        Map<String, Object> problemDetails = new HashMap<>();
        problemDetails.put("type", type);
        problemDetails.put("title", title);
        problemDetails.put("status", status.value());
        problemDetails.put("detail", detail);
        problemDetails.put("instance", instance);
        return new ResponseEntity<>(problemDetails, status);
    }
}
