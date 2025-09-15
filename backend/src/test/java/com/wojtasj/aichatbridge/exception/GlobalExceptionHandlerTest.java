package com.wojtasj.aichatbridge.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.dto.MessageDTO;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;

import jakarta.validation.Validation;

import java.util.Locale;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the exception handling of {@link GlobalExceptionHandler} in the AI Chat Bridge application.
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    /**
     * Sets up the test environment with MockMvc, GlobalExceptionHandler, and Validator.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        Locale.setDefault(Locale.ENGLISH);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController(validator))
                .setControllerAdvice(exceptionHandler)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    /**
     * Tests handling of {@link OpenAIServiceException}.
     * @since 1.0
     */
    @Test
    void shouldHandleOpenAIServiceException() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/test/openai")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/problems/openai-service-error"))
                .andExpect(jsonPath("$.title").value("OpenAI Service Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Failed to process OpenAI request"))
                .andExpect(jsonPath("$.instance").value("/test/openai"));
    }

    /**
     * Tests handling of {@link OpenAIServiceException} for invalid model.
     * @since 1.0
     */
    @Test
    void shouldHandleInvalidOpenAIModelException() throws Exception {
        mockMvc.perform(post("/test/openai-invalid-model")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/openai-service-error"))
                .andExpect(jsonPath("$.title").value("OpenAI Service Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Invalid OpenAI model: test-model"))
                .andExpect(jsonPath("$.instance").value("/test/openai-invalid-model"));
    }

    /**
     * Tests handling of {@link OpenAIServiceException} for invalid api-key.
     * @since 1.0
     */
    @Test
    void shouldHandleInvalidOpenAIApiKeyException() throws Exception {
        mockMvc.perform(post("/test/openai-invalid-api-key")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/openai-service-error"))
                .andExpect(jsonPath("$.title").value("OpenAI Service Error"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid OpenAI API key"))
                .andExpect(jsonPath("$.instance").value("/test/openai-invalid-api-key"));
    }

    /**
     * Tests handling of {@link OpenAIServiceException} for too many requests.
     * @since 1.0
     */
    @Test
    void shouldHandleTooManyRequestsOpenAIException() throws Exception {
        mockMvc.perform(post("/test/openai-too-many-requests")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("/problems/openai-service-error"))
                .andExpect(jsonPath("$.title").value("OpenAI Service Error"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.detail").value("Too many requests to OpenAI API"))
                .andExpect(jsonPath("$.instance").value("/test/openai-too-many-requests"));
    }


    /**
     * Tests handling of {@link DiscordServiceException}.
     * @since 1.0
     */
    @Test
    void shouldHandleDiscordServiceException() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/test/discord")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/problems/discord-service-error"))
                .andExpect(jsonPath("$.title").value("Discord Service Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Failed to process Discord request"))
                .andExpect(jsonPath("$.instance").value("/test/discord"));
    }

    /**
     * Tests handling of {@link com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException} within {@link org.springframework.http.converter.HttpMessageNotReadableException}.
     * @since 1.0
     */
    @Test
    void shouldHandleUnrecognizedPropertyException() throws Exception {
        // Arrange
        String invalidJson = "{\"contentx\":\"test\",\"unknownField\":1}";

        // Act & Assert
        mockMvc.perform(post("/test/invalid-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/invalid-request"))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Unknown field: contentx"))
                .andExpect(jsonPath("$.instance").value("/test/invalid-json"));
    }

    /**
     * Tests handling of {@link org.springframework.http.converter.HttpMessageNotReadableException} for malformed JSON.
     * @since 1.0
     */
    @Test
    void shouldHandleMalformedJson() throws Exception {
        // Arrange
        String malformedJson = "{\"content\": \"test\"";

        // Act & Assert
        mockMvc.perform(post("/test/invalid-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/malformed-json"))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Malformed JSON request"))
                .andExpect(jsonPath("$.instance").value("/test/invalid-json"));
    }

    /**
     * Tests handling of {@link org.springframework.web.bind.MethodArgumentNotValidException} for validation errors.
     * @since 1.0
     */
    @Test
    void shouldHandleValidationException() throws Exception {
        // Arrange
        MessageDTO messageDTO = new MessageDTO("");

        // Act & Assert
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/test/validation"));
    }

    /**
     * Tests handling of {@link ConstraintViolationException} for parameter validation errors.
     * @since 1.0
     */
    @Test
    void shouldHandleConstraintViolationException() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/test/constraint-violation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("value", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("value must not be blank"))
                .andExpect(jsonPath("$.instance").value("/test/constraint-violation"));
    }

    /**
     * Tests handling of {@link BadCredentialsException}.
     * @since 1.0
     */
    @Test
    void shouldHandleBadCredentialsException() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/test/bad-credentials")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.title").value("Authentication Failed"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid username or password"))
                .andExpect(jsonPath("$.instance").value("/test/bad-credentials"));
    }

    /**
     * Tests handling of {@link AuthenticationException} for authentication errors.
     * @since 1.0
     */
    @Test
    void shouldHandleAuthenticationException() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/test/authentication")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                .andExpect(jsonPath("$.title").value("Authentication Failed"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication failed"))
                .andExpect(jsonPath("$.instance").value("/test/authentication"));
    }

    /**
     * Tests handling of {@link AuthenticationException} for registration errors.
     * @since 1.0
     */
    @Test
    void shouldHandleAuthenticationExceptionForRegistration() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/test/authentication-registration")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/problems/registration-failed"))
                .andExpect(jsonPath("$.title").value("Registration Failed"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Username already taken"))
                .andExpect(jsonPath("$.instance").value("/test/authentication-registration"));
    }

    /**
     * Tests handling of {@link AccessDeniedException}.
     * @since 1.0
     */
    @Test
    void shouldHandleAccessDeniedException() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/test/access-denied")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/access-denied"))
                .andExpect(jsonPath("$.title").value("Access Denied"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.instance").value("/test/access-denied"));
    }

    /**
     * Tests handling of {@link ResponseStatusException}.
     * @since 1.0
     */
    @Test
    void shouldHandleResponseStatusException() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/test/response-status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/problems/response-status-error"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Test response status error"))
                .andExpect(jsonPath("$.instance").value("/test/response-status"));
    }

    /**
     * Tests handling of {@link MessageNotFoundException}.
     * @since 1.0
     */
    @Test
    void shouldHandleMessageNotFoundException() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/test/message-not-found")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/problems/message-not-found"))
                .andExpect(jsonPath("$.title").value("Message Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Message not found with ID: 999"))
                .andExpect(jsonPath("$.instance").value("/test/message-not-found"));
    }

    /**
     * Tests handling of generic {@link Exception}.
     * @since 1.0
     */
    @Test
    void shouldHandleGenericException() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/test/generic")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/problems/internal-server-error"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("Unexpected error: Test generic error"))
                .andExpect(jsonPath("$.instance").value("/test/generic"));
    }

    /**
     * Tests handling of {@link RequestNotPermitted} for rate limit exceeded errors.
     * @since 1.0
     */
    @Test
    void shouldHandleRateLimitExceeded() throws Exception {
        mockMvc.perform(post("/test/rate-limit")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("/problems/rate-limit-exceeded"))
                .andExpect(jsonPath("$.title").value("Rate Limit Exceeded"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Too many requests - rate limit exceeded for RateLimiter 'refreshToken' does not permit further calls")))
                .andExpect(jsonPath("$.instance").value("/test/rate-limit"));
    }


    /**
     * Test controller to simulate various exceptions for testing {@link GlobalExceptionHandler}.
     * @since 1.0
     */
    @RestController
    @Validated
    static class TestController {

        private final Validator validator;

        public TestController(Validator validator) {
            this.validator = validator;
        }

        @PostMapping("/test/openai")
        public void throwOpenAIException() {
            throw new OpenAIServiceException("Failed to process OpenAI request");
        }

        @PostMapping("/test/discord")
        public void throwDiscordException() {
            throw new DiscordServiceException("Failed to process Discord request");
        }

        @SuppressWarnings("unused")
        @PostMapping("/test/invalid-json")
        public void throwHttpMessageNotReadableException(@RequestBody MessageDTO messageDTO) {
            // HttpMessageNotReadableException for invalid JSON
        }

        @SuppressWarnings("unused")
        @PostMapping("/test/validation")
        public void throwValidationException(@Validated @RequestBody MessageDTO messageDTO) {
            // MethodArgumentNotValidException due to validation
        }

        @PostMapping("/test/constraint-violation")
        public void throwConstraintViolationException(@RequestParam("value") @NotBlank String value) {
            class ValueWrapper {
                @NotBlank
                final String value;
                ValueWrapper(String value) { this.value = value; }
            }

            Set<ConstraintViolation<ValueWrapper>> violations =
                    validator.validate(new ValueWrapper(value));

            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        @PostMapping("/test/bad-credentials")
        public void throwBadCredentialsException() {
            throw new BadCredentialsException("Invalid username or password");
        }

        @PostMapping("/test/authentication")
        public void throwAuthenticationException() {
            throw new AuthenticationException("Authentication failed");
        }

        @PostMapping("/test/authentication-registration")
        public void throwAuthenticationExceptionForRegistration() {
            throw new UserAlreadyExistsException("Username already taken");
        }

        @PostMapping("/test/access-denied")
        public void throwAccessDeniedException() {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }

        @PostMapping("/test/response-status")
        public void throwResponseStatusException() {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Test response status error");
        }

        @PostMapping("/test/generic")
        public void throwGenericException() {
            throw new RuntimeException("Test generic error");
        }

        @PostMapping("/test/message-not-found")
        public void throwMessageNotFoundException() {
            throw new MessageNotFoundException("Message not found with ID: 999");
        }

        @PostMapping("/test/openai-invalid-model")
        public void throwInvalidOpenAIModelException() {
            throw new OpenAIServiceException("Invalid OpenAI model: test-model");
        }

        @PostMapping("/test/openai-too-many-requests")
        public void throwTooManyRequestsOpenAIException() {
            throw new OpenAIServiceException("Too many requests to OpenAI API");
        }

        @PostMapping("/test/openai-invalid-api-key")
        public void throwTInvalidApiKeyOpenAIException() {
            throw new OpenAIServiceException("Invalid OpenAI API key");
        }

        @PostMapping("/test/rate-limit")
        public void throwRateLimitExceeded() {
            RateLimiter rateLimiter =
                    RateLimiter.of("refreshToken",
                            RateLimiterConfig.custom()
                                    .limitForPeriod(1)
                                    .limitRefreshPeriod(java.time.Duration.ofSeconds(1))
                                    .timeoutDuration(java.time.Duration.ZERO)
                                    .build());
            throw RequestNotPermitted.createRequestNotPermitted(rateLimiter);
        }

    }
}
