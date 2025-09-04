package com.wojtasj.aichatbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wojtasj.aichatbridge.dto.MessageDTO;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.exception.GlobalExceptionHandler;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import com.wojtasj.aichatbridge.service.OpenAIService;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for MessageController.
 */
@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    private static final String MESSAGES_URL = "/api/messages";
    private static final String OPENAI_URL = "/api/messages/openai";
    private static final String TEST_CONTENT = "Test message";
    private static final String HELLO_CONTENT = "Hello!";
    private static final String AI_RESPONSE = "Hi, hello!";

    private WebTestClient webTestClient;

    @Mock
    private MessageRepository repository;

    @Mock
    private OpenAIService openAIService;

    @InjectMocks
    private MessageController messageController;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    @BeforeEach
    void setUp() {
        messageController = new MessageController(repository, openAIService);

        webTestClient = WebTestClient.bindToController(messageController)
                .controllerAdvice(new GlobalExceptionHandler())
                .httpMessageCodecs(configurer -> configurer.defaultCodecs()
                        .jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON)))
                .build();
    }

    /**
     * Tests retrieving all messages.
     */
    @Test
    void shouldGetAllMessages() {
        // Arrange
        MessageEntity message = createMessageEntity(TEST_CONTENT);
        message.setId(1L);
        when(repository.findAll()).thenReturn(List.of(message));

        // Act & Assert
        webTestClient.get()
                .uri(MESSAGES_URL)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MessageEntity.class)
                .hasSize(1)
                .value(messages -> {
                    MessageEntity entity = messages.getFirst();
                    assertThat(entity.getContent(), equalTo(TEST_CONTENT));
                    assertThat(entity.getId(), equalTo(1L));
                    assertThat(entity.getCreatedAt(), notNullValue());
                });
    }

    /**
     * Tests creating a new message.
     */
    @Test
    void shouldCreateMessage() throws Exception {
        // Arrange
        MessageDTO messageDTO = new MessageDTO(TEST_CONTENT);
        MessageEntity message = createMessageEntity(TEST_CONTENT);
        when(repository.save(any(MessageEntity.class))).thenReturn(message);

        // Act & Assert
        webTestClient.post()
                .uri(MESSAGES_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(messageDTO))
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .value(entity -> {
                    assertThat(entity.getContent(), equalTo(TEST_CONTENT));
                    assertThat(entity.getCreatedAt(), notNullValue());
                });
    }

    /**
     * Tests sending a message to OpenAI and saving the response.
     */
    @Test
    void shouldSendToOpenAI() throws Exception {
        // Arrange
        MessageDTO inputDTO = new MessageDTO(HELLO_CONTENT);
        MessageEntity input = createMessageEntity(HELLO_CONTENT);
        MessageEntity response = createMessageEntity(AI_RESPONSE);
        when(repository.save(any(MessageEntity.class))).thenReturn(input, response);
        when(openAIService.sendMessageToOpenAI(any(MessageEntity.class))).thenReturn(Mono.just(response));

        // Act & Assert
        webTestClient.post()
                .uri(OPENAI_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(inputDTO))
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .value(entity -> {
                    assertThat(entity.getContent(), equalTo(AI_RESPONSE));
                    assertThat(entity.getCreatedAt(), notNullValue());
                });
    }

    /**
     * Tests handling OpenAI error in sendToOpenAI.
     */
    @Test
    void shouldHandleOpenAIError() throws Exception {
        // Arrange
        MessageDTO inputDTO = new MessageDTO(HELLO_CONTENT);
        MessageEntity input = createMessageEntity(HELLO_CONTENT);
        when(repository.save(any(MessageEntity.class))).thenReturn(input);
        when(openAIService.sendMessageToOpenAI(any(MessageEntity.class)))
                .thenReturn(Mono.error(new OpenAIServiceException("OpenAI API error")));

        // Act & Assert
        webTestClient.post()
                .uri(OPENAI_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(inputDTO))
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Failed to process OpenAI request");
    }

    /**
     * Tests sending a message with unknown fields (e.g., id).
     */
    @Test
    void shouldRejectMessageWithExistingId() {
        // Arrange
        String invalidJson = "{\"content\":\"" + HELLO_CONTENT + "\",\"id\":1}";

        // Act & Assert
        webTestClient.post()
                .uri(OPENAI_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .isEqualTo("Unknown field in request: id");
    }

    private MessageEntity createMessageEntity(String content) {
        MessageEntity entity = new MessageEntity();
        entity.setContent(content);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}