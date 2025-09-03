package com.wojtasj.aichatbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import com.wojtasj.aichatbridge.service.OpenAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for MessageController.
 */
@ExtendWith(MockitoExtension.class)
public class MessageControllerTest {
    private MockMvc mockMvc;
    @Mock
    private MessageRepository repository;
    @Mock
    private OpenAIService openAIService;
    @InjectMocks
    private MessageController messageController;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(messageController).build();
    }

    /**
     * Tests retrieving all messages.
     */
    @Test
    void shouldGetAllMessages() throws Exception {
        MessageEntity message = new MessageEntity();
        message.setId(1L);
        message.setContent("Test message");
        message.setCreatedAt(LocalDateTime.now());

        when(repository.findAll()).thenReturn(List.of(message));

        mockMvc.perform(get("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Test message"));
    }

    /**
     * Tests creating a new message.
     */
    @Test
    void shouldCreateMessage() throws Exception {
        MessageEntity message = new MessageEntity();
        message.setContent("Test message");
        message.setCreatedAt(LocalDateTime.now());

        when(repository.save(any(MessageEntity.class))).thenReturn(message);

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Test message"));
    }

    /**
     * Tests sending a message to OpenAI and saving the response.
     */
    @Test
    void shouldSendToOpenAI() throws Exception {
        MessageEntity input = new MessageEntity();
        input.setContent("Hello!");
        input.setCreatedAt(LocalDateTime.now());

        MessageEntity response = new MessageEntity();
        response.setContent("Hi, hello!");
        response.setCreatedAt(LocalDateTime.now());

        when(repository.save(any(MessageEntity.class))).thenReturn(input, response);
        when(openAIService.sendMessageToOpenAI(any(MessageEntity.class))).thenReturn(response);

        mockMvc.perform(post("/api/messages/openai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hi, hello!"));
    }
}