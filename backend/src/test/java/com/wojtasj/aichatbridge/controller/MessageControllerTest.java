package com.wojtasj.aichatbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.dto.MessageDTO;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.exception.GlobalExceptionHandler;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import com.wojtasj.aichatbridge.service.OpenAIServiceImpl;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the functionality of {@link MessageController} in the AI Chat Bridge application.
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class MessageControllerTest {

    private static final String MESSAGES_URL = "/api/messages";
    private static final String OPENAI_URL = "/api/messages/openai";
    private static final String TEST_CONTENT = "Test message";
    private static final String HELLO_CONTENT = "Hello!";
    private static final String AI_RESPONSE = "Hi, hello!";

    private MockMvc mockMvc;

    @Mock
    private MessageRepository repository;

    @Mock
    private OpenAIServiceImpl openAIService;

    @InjectMocks
    private MessageController messageController;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    /**
     * Sets up the test environment with MockMvc and dependencies.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(messageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    /**
     * Tests retrieving an empty page when no messages exist in the database.
     * @since 1.0
     */
    @Test
    void shouldReturnEmptyListWhenNoMessages() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        // Act & Assert
        mockMvc.perform(get(MESSAGES_URL)
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /**
     * Tests retrieving all messages with pagination from the database.
     * @since 1.0
     */
    @Test
    void shouldGetAllMessages() throws Exception {
        // Arrange
        MessageEntity message1 = createMessageEntity(1L, TEST_CONTENT);
        MessageEntity message2 = createMessageEntity(2L, HELLO_CONTENT);
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        Page<MessageEntity> page = new PageImpl<>(List.of(message1, message2), pageable, 2);
        when(repository.findAll(pageable)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(MESSAGES_URL)
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].content").value(TEST_CONTENT))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[1].id").value(2L))
                .andExpect(jsonPath("$.content[1].content").value(HELLO_CONTENT))
                .andExpect(jsonPath("$.content[1].createdAt").exists())
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    /**
     * Tests creating a new message and saving it to the database.
     * @since 1.0
     */
    @Test
    void shouldCreateMessage() throws Exception {
        // Arrange
        MessageDTO messageDTO = new MessageDTO(TEST_CONTENT);
        MessageEntity savedMessage = createMessageEntity(1L, TEST_CONTENT);
        when(repository.save(any(MessageEntity.class))).thenReturn(savedMessage);

        // Act & Assert
        mockMvc.perform(post(MESSAGES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value(TEST_CONTENT))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    /**
     * Tests sending a message to OpenAI and saving the response in the database.
     * @since 1.0
     */
    @Test
    void shouldSendToOpenAI() throws Exception {
        // Arrange
        MessageDTO messageDTO = new MessageDTO(HELLO_CONTENT);
        MessageEntity input = createMessageEntity(1L, HELLO_CONTENT);
        MessageEntity response = createMessageEntity(2L, AI_RESPONSE);
        when(repository.save(any(MessageEntity.class))).thenReturn(input, response);
        when(openAIService.sendMessageToOpenAI(any(MessageEntity.class), eq(false))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(OPENAI_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.content").value(AI_RESPONSE))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    /**
     * Tests rejecting a message with empty content during OpenAI processing.
     * @since 1.0
     */
    @Test
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
                .andExpect(jsonPath("$.detail").value("content Content cannot be blank"));
    }

    /**
     * Tests rejecting a message with unknown fields (e.g., id) in the request.
     * @since 1.0
     */
    @Test
    void shouldRejectMessageWithExistingId() throws Exception {
        // Arrange
        String invalidJson = "{\"content\":\"" + HELLO_CONTENT + "\",\"id\":1}";

        // Act & Assert
        mockMvc.perform(post(OPENAI_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/invalid-request"))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Unknown field: id"));
    }

    /**
     * Tests handling of {@link OpenAIServiceException} in the sendToOpenAI endpoint.
     * @since 1.0
     */
    @Test
    void shouldHandleOpenAIError() throws Exception {
        // Arrange
        MessageDTO messageDTO = new MessageDTO(HELLO_CONTENT);
        MessageEntity input = createMessageEntity(1L, HELLO_CONTENT);
        when(repository.save(any(MessageEntity.class))).thenReturn(input);
        when(openAIService.sendMessageToOpenAI(any(MessageEntity.class), eq(false)))
                .thenThrow(new OpenAIServiceException("OpenAI API error"));

        // Act & Assert
        mockMvc.perform(post(OPENAI_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageDTO)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/problems/openai-service-error"))
                .andExpect(jsonPath("$.title").value("OpenAI Service Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Failed to process OpenAI request"));
    }

    /**
     * Creates a mock MessageEntity for testing purposes.
     * @param id the ID of the message
     * @param content the content of the message
     * @return a MessageEntity with the specified ID and content
     * @since 1.0
     */
    private MessageEntity createMessageEntity(Long id, String content) {
        MessageEntity entity = new MessageEntity();
        entity.setId(id);
        entity.setContent(content);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
