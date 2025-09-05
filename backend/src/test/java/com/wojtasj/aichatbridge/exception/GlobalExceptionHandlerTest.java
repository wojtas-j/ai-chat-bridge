package com.wojtasj.aichatbridge.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.dto.MessageDTO;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for {@link GlobalExceptionHandler}.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(exceptionHandler)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    /**
     * Tests handling of OpenAIServiceException.
     */
    @Test
    void shouldHandleOpenAIServiceException() throws Exception {
        mockMvc.perform(post("/test/openai")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/problems/openai-service-error"))
                .andExpect(jsonPath("$.title").value("OpenAI Service Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Failed to process OpenAI request"));
    }

    /**
     * Tests handling of DiscordServiceException.
     */
    @Test
    void shouldHandleDiscordServiceException() throws Exception {
        mockMvc.perform(post("/test/discord")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/problems/discord-service-error"))
                .andExpect(jsonPath("$.title").value("Discord Service Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Failed to process Discord request"));
    }

    /**
     * Tests handling of HttpMessageNotReadableException with UnrecognizedPropertyException cause.
     */
    @Test
    void shouldHandleUnrecognizedPropertyException() throws Exception {
        String invalidJson = "{\"contentx\":\"test\",\"unknownField\":1}";
        mockMvc.perform(post("/test/invalid-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/invalid-request"))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Unknown field: contentx"));
    }

    /**
     * Tests handling of HttpMessageNotReadableException for malformed JSON.
     */
    @Test
    void shouldHandleMalformedJson() throws Exception {
        String malformedJson = "{\"content\": \"test\""; // Missing closing brace
        mockMvc.perform(post("/test/invalid-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/malformed-json"))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Malformed JSON request"));
    }

    /**
     * Tests handling of MethodArgumentNotValidException.
     */
    @Test
    void shouldHandleValidationException() throws Exception {
        MessageDTO messageDTO = new MessageDTO("");
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("content Content cannot be blank"));
    }

    /**
     * Tests handling of ResponseStatusException.
     */
    @Test
    void shouldHandleResponseStatusException() throws Exception {
        mockMvc.perform(post("/test/response-status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/problems/response-status-error"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Test response status error"));
    }

    /**
     * Tests handling of generic Exception.
     */
    @Test
    void shouldHandleGenericException() throws Exception {
        mockMvc.perform(post("/test/generic")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/problems/internal-server-error"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Unexpected error: Test generic error"));
    }

    /**
     * Test controller to simulate exceptions.
     */
    @RestController
    static class TestController {

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

        @PostMapping("/test/response-status")
        public void throwResponseStatusException() {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Test response status error");
        }

        @PostMapping("/test/generic")
        public void throwGenericException() {
            throw new RuntimeException("Test generic error");
        }
    }
}