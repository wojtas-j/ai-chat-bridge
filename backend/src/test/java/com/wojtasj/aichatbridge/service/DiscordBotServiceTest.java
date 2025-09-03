package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.configuration.DiscordProperties;
import com.wojtasj.aichatbridge.entity.MessageEntity;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for DiscordBotService.
 */
@ExtendWith(MockitoExtension.class)
public class DiscordBotServiceTest {

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
        when(discordProperties.getBotPrefix()).thenReturn("!ai ");
    }

    /**
     * Tests handling a message with the correct prefix.
     */
    @Test
    void shouldHandleMessageWithPrefix() {
        // Arrange
        when(message.getContent()).thenReturn("!ai Hello!");
        when(messageCreateEvent.getMessage()).thenReturn(message);
        when(message.getChannel()).thenReturn(Mono.just(messageChannel));
        when(messageChannel.createMessage("Hi, hello!")).thenReturn(MessageCreateMono.of(messageChannel).withContent("Hi, hello!"));
        Message mockedResponseMessage = mock(Message.class);
        when(messageChannel.createMessage(any(MessageCreateSpec.class)))
                .thenReturn(Mono.just(mockedResponseMessage));

        MessageEntity userMessage = new MessageEntity();
        userMessage.setContent("Hello!");
        userMessage.setCreatedAt(LocalDateTime.now());

        MessageEntity aiResponse = new MessageEntity();
        aiResponse.setContent("Hi, hello!");
        aiResponse.setCreatedAt(LocalDateTime.now());

        when(messageRepository.save(any(MessageEntity.class))).thenReturn(userMessage, aiResponse);
        when(openAIService.sendMessageToOpenAI(any(MessageEntity.class))).thenReturn(aiResponse);

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .expectNext(mockedResponseMessage)
                .expectComplete()
                .verify();

        verify(messageRepository, times(2)).save(any(MessageEntity.class));
        verify(openAIService).sendMessageToOpenAI(userMessage);
        verify(messageChannel).createMessage("Hi, hello!"); // sprawdzamy overload ze String
    }

    /**
     * Tests ignoring a message without the correct prefix.
     */
    @Test
    void shouldIgnoreMessageWithoutPrefix() {
        // Arrange
        when(message.getContent()).thenReturn("Hello!");
        when(messageCreateEvent.getMessage()).thenReturn(message);

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(messageRepository, never()).save(any(MessageEntity.class));
        verify(openAIService, never()).sendMessageToOpenAI(any(MessageEntity.class));
        verify(messageChannel, never()).createMessage(anyString());
    }
}