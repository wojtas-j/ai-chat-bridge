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
import discord4j.core.spec.MessageCreateSpec;
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
 * Tests for DiscordBotService.
 */
@ExtendWith(MockitoExtension.class)
class DiscordBotServiceTest {

    private static final String BOT_PREFIX = "!ai ";
    private static final String USER_MESSAGE = "Hello!";
    private static final String AI_RESPONSE = "Hi, hello!";

    @Mock
    private OpenAIService openAIService;

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
    private DiscordBotService discordBotService;

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
        Message mockedResponseMessage = mock(Message.class);
        when(message.getContent()).thenReturn(BOT_PREFIX + USER_MESSAGE);
        when(messageCreateEvent.getMessage()).thenReturn(message);
        when(message.getChannel()).thenReturn(Mono.just(messageChannel));
        when(messageChannel.createMessage(anyString())).thenAnswer(inv ->
                MessageCreateMono.of(messageChannel).withContent(inv.getArgument(0, String.class)));
        when(messageChannel.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.just(mockedResponseMessage));

        MessageEntity userMessage = createMessageEntity();
        MessageEntity aiResponse = createMessageEntity();
        aiResponse.setContent(AI_RESPONSE);
        when(messageRepository.save(any(MessageEntity.class))).thenReturn(userMessage, aiResponse);
        when(openAIService.sendMessageToOpenAI(any(MessageEntity.class))).thenReturn(Mono.just(aiResponse));

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .expectNext(mockedResponseMessage)
                .expectComplete()
                .verify();

        verify(messageRepository, times(2)).save(any(MessageEntity.class));
        verify(openAIService).sendMessageToOpenAI(userMessage);
        verify(messageChannel).createMessage(AI_RESPONSE);
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
                .expectComplete()
                .verify();

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

        MessageEntity userMessage = createMessageEntity();
        when(messageRepository.save(any(MessageEntity.class))).thenReturn(userMessage);
        when(openAIService.sendMessageToOpenAI(any())).thenReturn(Mono.error(new OpenAIServiceException("OpenAI API error")));

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .expectComplete()
                .verify();

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
                .expectComplete()
                .verify();

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
                .expectComplete()
                .verify();

        verifyNoInteractions(messageRepository, openAIService, messageChannel);
    }

    private MessageEntity createMessageEntity() {
        MessageEntity entity = new MessageEntity();
        entity.setContent(DiscordBotServiceTest.USER_MESSAGE);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}