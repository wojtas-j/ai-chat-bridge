package com.wojtasj.aichatbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.dto.MessageDTO;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.exception.GlobalExceptionHandler;
import com.wojtasj.aichatbridge.repository.MessageRepository;
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
    private static final String TEST_CONTENT = "Test message";
    private static final String HELLO_CONTENT = "Hello!";

    private MockMvc mockMvc;

    @Mock
    private MessageRepository repository;

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
