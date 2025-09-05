package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.configuration.DiscordProperties;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.exception.DiscordServiceException;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import com.wojtasj.aichatbridge.repository.MessageRepository;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateMono;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DiscordBotServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class DiscordBotServiceTest {

    private static final String BOT_PREFIX = "!ai ";
    private static final String USER_MESSAGE = "Hello!";
    private static final String AI_RESPONSE = "Hi, hello!";
    private static final String EMPTY_MESSAGE = "!ai ";

    @Mock
    private OpenAIServiceImpl openAIService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private DiscordProperties discordProperties;

    @Mock
    private MessageCreateEvent messageCreateEvent;

    @Mock
    private Message message;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private DiscordBotServiceImpl discordBotService;

    @BeforeEach
    void setUp() {
        when(discordProperties.getBotPrefix()).thenReturn(BOT_PREFIX);
    }

    /**
     * Tests handling a message with the correct prefix.
     */
    @Test
    void shouldHandleMessageWithPrefix() {
        // Arrange
        String inputMessage = BOT_PREFIX + USER_MESSAGE;
        MessageEntity userMessage = createMessageEntity(USER_MESSAGE);
        MessageEntity aiResponse = createMessageEntity(AI_RESPONSE);
        Message responseMessage = mock(Message.class);

        when(message.getContent()).thenReturn(inputMessage);
        when(messageCreateEvent.getMessage()).thenReturn(message);
        when(message.getChannel()).thenReturn(Mono.just(messageChannel));
        when(messageRepository.save(any(MessageEntity.class)))
                .thenReturn(userMessage)
                .thenReturn(aiResponse);
        when(openAIService.sendMessageToOpenAI(userMessage)).thenReturn(aiResponse);

        MessageCreateMono mockCreateMono = mock(MessageCreateMono.class);
        doReturn(mockCreateMono).when(messageChannel).createMessage(eq(AI_RESPONSE));
        doReturn(Mono.just(responseMessage)).when(mockCreateMono).onErrorMap(any());

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .expectNext(responseMessage)
                .verifyComplete();

        verify(messageRepository, times(2)).save(any(MessageEntity.class));
        verify(openAIService).sendMessageToOpenAI(userMessage);
        verify(messageChannel).createMessage(eq(AI_RESPONSE));
        verify(mockCreateMono).onErrorMap(any());
    }

    /**
     * Tests ignoring a message without the correct prefix.
     */
    @Test
    void shouldIgnoreMessageWithoutPrefix() {
        // Arrange
        when(message.getContent()).thenReturn(USER_MESSAGE);
        when(messageCreateEvent.getMessage()).thenReturn(message);

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verifyNoInteractions(messageRepository, openAIService, messageChannel);
    }

    /**
     * Tests ignoring a message with empty content after prefix removal.
     */
    @Test
    void shouldIgnoreEmptyMessageContent() {
        // Arrange
        when(message.getContent()).thenReturn(EMPTY_MESSAGE);
        when(messageCreateEvent.getMessage()).thenReturn(message);

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verifyNoInteractions(messageRepository, openAIService, messageChannel);
    }

    /**
     * Tests handling OpenAI service error.
     */
    @Test
    void shouldHandleOpenAIError() {
        // Arrange
        when(message.getContent()).thenReturn(BOT_PREFIX + USER_MESSAGE);
        when(messageCreateEvent.getMessage()).thenReturn(message);
        when(message.getChannel()).thenReturn(Mono.just(messageChannel));

        MessageEntity userMessage = createMessageEntity(USER_MESSAGE);
        when(messageRepository.save(any(MessageEntity.class))).thenReturn(userMessage);
        when(openAIService.sendMessageToOpenAI(any())).thenThrow(new OpenAIServiceException("OpenAI API error"));

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(messageRepository).save(any(MessageEntity.class));
        verify(openAIService).sendMessageToOpenAI(userMessage);
        verifyNoInteractions(messageChannel);
    }

    /**
     * Tests handling database save error for user message.
     */
    @Test
    void shouldHandleDatabaseSaveError() {
        // Arrange
        when(message.getContent()).thenReturn(BOT_PREFIX + USER_MESSAGE);
        when(messageCreateEvent.getMessage()).thenReturn(message);
        when(message.getChannel()).thenReturn(Mono.just(messageChannel));

        when(messageRepository.save(any(MessageEntity.class))).thenThrow(new DiscordServiceException("Database save error"));

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(messageRepository).save(any(MessageEntity.class));
        verifyNoInteractions(openAIService, messageChannel);
    }

    /**
     * Tests handling Discord channel error.
     */
    @Test
    void shouldHandleDiscordChannelError() {
        // Arrange
        when(message.getContent()).thenReturn(BOT_PREFIX + USER_MESSAGE);
        when(messageCreateEvent.getMessage()).thenReturn(message);
        when(message.getChannel()).thenReturn(Mono.error(new DiscordServiceException("Channel not found")));

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verifyNoInteractions(messageRepository, openAIService, messageChannel);
    }

    private MessageEntity createMessageEntity(String content) {
        MessageEntity entity = new MessageEntity();
        entity.setContent(content);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}