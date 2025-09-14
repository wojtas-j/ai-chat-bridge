package com.wojtasj.aichatbridge.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.configuration.SecurityConfig;
import com.wojtasj.aichatbridge.dto.MessageDTO;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.exception.GlobalExceptionHandler;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import com.wojtasj.aichatbridge.service.AuthenticationService;
import com.wojtasj.aichatbridge.service.JwtTokenProviderImpl;
import com.wojtasj.aichatbridge.service.OpenAIServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the functionality of {@link OpenAIController} in the AI Chat Bridge application.
 * @since 1.0
 */
@WebMvcTest(OpenAIController.class)
@ContextConfiguration(classes = {OpenAIController.class, GlobalExceptionHandler.class, SecurityConfig.class})
@Import(SecurityConfig.class)
class OpenAIControllerTest {

    private static final String OPENAI_URL = "/api/openai";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123P!";
    private static final String TEST_API_KEY = "sk-test-1234567890";
    private static final String TEST_MESSAGE_CONTENT = "Hello, AI!";
    private static final String TEST_RESPONSE_CONTENT = "AI response";
    private static final int TEST_MAX_TOKENS = 1000;

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("unused")
    @MockitoBean
    private MessageRepository messageRepository;

    @SuppressWarnings("unused")
    @MockitoBean
    private OpenAIServiceImpl openAIService;

    @SuppressWarnings("unused")
    @MockitoBean
    private AuthenticationService authenticationService;

    @SuppressWarnings("unused")
    @MockitoBean
    private JwtTokenProviderImpl jwtTokenProvider;

    @SuppressWarnings("unused")
    @MockitoBean
    private UserDetailsService userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    private UserEntity userEntity;
    private MessageEntity inputMessage;
    private MessageEntity responseMessage;

    /**
     * Sets up the test environment with mocked dependencies.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        userEntity = UserEntity.builder()
                .username(TEST_USERNAME)
                .email("test@example.com")
                .password(TEST_PASSWORD)
                .roles(Set.of(Role.USER))
                .apiKey(TEST_API_KEY)
                .maxTokens(TEST_MAX_TOKENS)
                .build();

        inputMessage = createMessageEntity(1L, TEST_MESSAGE_CONTENT, userEntity);
        responseMessage = createMessageEntity(2L, TEST_RESPONSE_CONTENT, userEntity);

        lenient().when(authenticationService.findByUsername(TEST_USERNAME)).thenReturn(userEntity);
    }

    @Nested
    class SendToOpenAITests {
        /**
         * Tests sending a message to OpenAI and saving the response successfully.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldSendToOpenAISuccessfully() throws Exception {
            // Arrange
            MessageDTO messageDTO = new MessageDTO(TEST_MESSAGE_CONTENT);
            when(messageRepository.save(any(MessageEntity.class))).thenReturn(inputMessage, responseMessage);
            when(openAIService.sendMessageToOpenAI(eq(inputMessage), eq(false), eq(userEntity)))
                    .thenReturn(responseMessage);

            // Act & Assert
            mockMvc.perform(post(OPENAI_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(responseMessage.getId()))
                    .andExpect(jsonPath("$.content").value(TEST_RESPONSE_CONTENT))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.user").doesNotExist());

            // Verify
            verify(authenticationService).findByUsername(TEST_USERNAME);
            verify(messageRepository, times(2)).save(argThat(message ->
                    message.getContent().equals(TEST_MESSAGE_CONTENT) || message.getContent().equals(TEST_RESPONSE_CONTENT)));
            verify(openAIService).sendMessageToOpenAI(eq(inputMessage), eq(false), eq(userEntity));
        }

        /**
         * Tests rejecting a message with empty content during OpenAI processing.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectEmptyContent() throws Exception {
            // Arrange
            MessageDTO messageDTO = new MessageDTO("");

            // Act & Assert
            mockMvc.perform(post(OPENAI_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("content Content cannot be blank")));

            // Verify
            verify(authenticationService, never()).findByUsername(any());
            verify(messageRepository, never()).save(any());
            verify(openAIService, never()).sendMessageToOpenAI(any(), anyBoolean(), any());
        }

        /**
         * Tests rejecting a message with unknown fields (e.g., id) in the request.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectMessageWithUnknownFields() throws Exception {
            // Arrange
            String invalidJson = "{\"content\":\"" + TEST_MESSAGE_CONTENT + "\",\"id\":1}";

            // Act & Assert
            mockMvc.perform(post(OPENAI_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/invalid-request"))
                    .andExpect(jsonPath("$.title").value("Invalid Request"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value("Unknown field: id"));

            // Verify
            verify(authenticationService, never()).findByUsername(any());
            verify(messageRepository, never()).save(any());
            verify(openAIService, never()).sendMessageToOpenAI(any(), anyBoolean(), any());
        }

        /**
         * Tests rejecting a request when the user is not authenticated.
         * @since 1.0
         */
        @Test
        void shouldRejectWhenNotAuthenticated() throws Exception {
            // Arrange
            MessageDTO messageDTO = new MessageDTO(TEST_MESSAGE_CONTENT);

            // Act & Assert
            mockMvc.perform(post(OPENAI_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").exists());

            // Verify
            verify(authenticationService, never()).findByUsername(any());
            verify(messageRepository, never()).save(any());
            verify(openAIService, never()).sendMessageToOpenAI(any(), anyBoolean(), any());
        }

        /**
         * Tests rejecting a request when the user is not found.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectWhenUserNotFound() throws Exception {
            // Arrange
            MessageDTO messageDTO = new MessageDTO(TEST_MESSAGE_CONTENT);
            when(authenticationService.findByUsername(TEST_USERNAME))
                    .thenThrow(new AuthenticationException("User not found: " + TEST_USERNAME));

            // Act & Assert
            mockMvc.perform(post(OPENAI_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value("User not found: " + TEST_USERNAME));

            // Verify
            verify(authenticationService).findByUsername(TEST_USERNAME);
            verify(messageRepository, never()).save(any());
            verify(openAIService, never()).sendMessageToOpenAI(any(), anyBoolean(), any());
        }

        /**
         * Tests handling of {@link OpenAIServiceException} in the sendToOpenAI endpoint.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldHandleOpenAIError() throws Exception {
            // Arrange
            MessageDTO messageDTO = new MessageDTO(TEST_MESSAGE_CONTENT);
            when(messageRepository.save(any(MessageEntity.class))).thenReturn(inputMessage);
            when(openAIService.sendMessageToOpenAI(eq(inputMessage), eq(false), eq(userEntity)))
                    .thenThrow(new OpenAIServiceException("Invalid OpenAI API key"));

            // Act & Assert
            mockMvc.perform(post(OPENAI_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/openai-service-error"))
                    .andExpect(jsonPath("$.title").value("OpenAI Service Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value("Invalid OpenAI API key"));

            // Verify
            verify(authenticationService).findByUsername(TEST_USERNAME);
            verify(messageRepository).save(any(MessageEntity.class));
            verify(openAIService).sendMessageToOpenAI(eq(inputMessage), eq(false), eq(userEntity));
            verify(messageRepository, never()).save(eq(responseMessage));
        }

        /**
         * Tests handling of unexpected exceptions in the sendToOpenAI endpoint.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldHandleUnexpectedError() throws Exception {
            // Arrange
            MessageDTO messageDTO = new MessageDTO(TEST_MESSAGE_CONTENT);
            when(messageRepository.save(any(MessageEntity.class))).thenReturn(inputMessage);
            when(openAIService.sendMessageToOpenAI(eq(inputMessage), eq(false), eq(userEntity)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // Act & Assert
            mockMvc.perform(post(OPENAI_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageDTO)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.type").value("/problems/internal-server-error"))
                    .andExpect(jsonPath("$.title").value("Internal Server Error"))
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.detail").value("Unexpected error: Unexpected error"));

            // Verify
            verify(authenticationService).findByUsername(TEST_USERNAME);
            verify(messageRepository).save(any(MessageEntity.class));
            verify(openAIService).sendMessageToOpenAI(eq(inputMessage), eq(false), eq(userEntity));
            verify(messageRepository, never()).save(eq(responseMessage));
        }

        /**
         * Tests that the saved message and response are associated with the correct user.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldAssociateMessageWithUser() throws Exception {
            // Arrange
            MessageDTO messageDTO = new MessageDTO(TEST_MESSAGE_CONTENT);
            when(messageRepository.save(any(MessageEntity.class))).thenReturn(inputMessage, responseMessage);
            when(openAIService.sendMessageToOpenAI(eq(inputMessage), eq(false), eq(userEntity)))
                    .thenReturn(responseMessage);

            // Act & Assert
            mockMvc.perform(post(OPENAI_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(responseMessage.getId()))
                    .andExpect(jsonPath("$.content").value(TEST_RESPONSE_CONTENT))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.user").doesNotExist());

            // Verify
            verify(authenticationService).findByUsername(TEST_USERNAME);
            verify(messageRepository, times(2)).save(argThat(message ->
                    message.getUser() != null && message.getUser().equals(userEntity)));
            verify(openAIService).sendMessageToOpenAI(eq(inputMessage), eq(false), eq(userEntity));
        }

        /**
         * Tests rejecting a message with content exceeding the maximum length.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldRejectMessageWithTooLongContent() throws Exception {
            // Arrange
            String longContent = "a".repeat(5001);
            MessageDTO messageDTO = new MessageDTO(longContent);

            // Act & Assert
            mockMvc.perform(post(OPENAI_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value(containsString("content Content length must be between 1 and 5000 characters")));

            // Verify
            verify(authenticationService, never()).findByUsername(any());
            verify(messageRepository, never()).save(any());
            verify(openAIService, never()).sendMessageToOpenAI(any(), anyBoolean(), any());
        }

        /**
         * Tests handling of database error during message save.
         * @since 1.0
         */
        @Test
        @WithMockUser(username = TEST_USERNAME)
        void shouldHandleDatabaseError() throws Exception {
            // Arrange
            MessageDTO messageDTO = new MessageDTO(TEST_MESSAGE_CONTENT);
            when(messageRepository.save(any(MessageEntity.class)))
                    .thenThrow(new org.springframework.dao.DataAccessException("Database error") {});

            // Act & Assert
            mockMvc.perform(post(OPENAI_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageDTO)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.type").value("/problems/internal-server-error"))
                    .andExpect(jsonPath("$.title").value("Internal Server Error"))
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.detail").value(containsString("Database error")));

            // Verify
            verify(authenticationService).findByUsername(TEST_USERNAME);
            verify(messageRepository).save(any(MessageEntity.class));
            verify(openAIService, never()).sendMessageToOpenAI(any(), anyBoolean(), any());
        }
    }

    /**
     * Creates a mock MessageEntity for testing purposes.
     * @param id the ID of the message
     * @param content the content of the message
     * @param user the user associated with the message
     * @return a MessageEntity with the specified ID, content, and user
     * @since 1.0
     */
    private MessageEntity createMessageEntity(Long id, String content, UserEntity user) {
        MessageEntity entity = new MessageEntity();
        entity.setId(id);
        entity.setContent(content);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUser(user);
        return entity;
    }
}
