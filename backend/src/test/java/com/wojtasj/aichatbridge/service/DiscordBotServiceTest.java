package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.configuration.DiscordProperties;
import com.wojtasj.aichatbridge.entity.DiscordMessageEntity;
import com.wojtasj.aichatbridge.repository.DiscordMessageRepository;
import com.wojtasj.aichatbridge.exception.DiscordServiceException;
import com.wojtasj.aichatbridge.exception.OpenAIServiceException;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DiscordBotServiceImpl} in the AI Chat Bridge application.
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class DiscordBotServiceTest {

    private static final String BOT_PREFIX = "!ai ";
    private static final String USER_MESSAGE = "Hello!";
    private static final String AI_RESPONSE = "Hi, hello!";
    private static final String EMPTY_MESSAGE = "!ai ";
    private static final String DISCORD_NICK = "TestUser";

    @Mock
    private OpenAIServiceImpl openAIService;

    @Mock
    private DiscordMessageRepository discordMessageRepository;

    @Mock
    private DiscordProperties discordProperties;

    @Mock
    private MessageCreateEvent messageCreateEvent;

    @Mock
    private Message message;

    @Mock
    private MessageChannel messageChannel;

    @Mock
    private User user;

    @InjectMocks
    private DiscordBotServiceImpl discordBotService;

    /**
     * Sets up the test environment with mocked dependencies.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        when(discordProperties.getBotPrefix()).thenReturn(BOT_PREFIX);
    }

    /**
     * Tests handling a message with the correct prefix, including saving to database, OpenAI processing, and Discord response.
     * @since 1.0
     */
    @Test
    void shouldHandleMessageWithPrefix() {
        // Arrange
        String inputMessage = BOT_PREFIX + USER_MESSAGE;
        DiscordMessageEntity userMessage = createDiscordMessageEntity(1L, USER_MESSAGE, DISCORD_NICK);
        DiscordMessageEntity aiResponse = createDiscordMessageEntity(2L, AI_RESPONSE, "AI-Bot");
        Message responseMessage = mock(Message.class);

        when(message.getContent()).thenReturn(inputMessage);
        when(messageCreateEvent.getMessage()).thenReturn(message);
        when(message.getAuthor()).thenReturn(Optional.of(user));
        when(user.getUsername()).thenReturn(DISCORD_NICK);
        when(message.getChannel()).thenReturn(Mono.just(messageChannel));
        when(discordMessageRepository.save(any(DiscordMessageEntity.class)))
                .thenReturn(userMessage)
                .thenReturn(aiResponse);
        when(openAIService.sendMessageToOpenAI(any(DiscordMessageEntity.class), eq(true), eq(null)))
                .thenReturn(aiResponse);

        MessageCreateMono mockCreateMono = mock(MessageCreateMono.class);
        when(messageChannel.createMessage(eq(AI_RESPONSE))).thenReturn(mockCreateMono);
        when(mockCreateMono.onErrorMap(any())).thenReturn(Mono.just(responseMessage));

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .expectNext(responseMessage)
                .verifyComplete();

        verify(discordMessageRepository, times(2)).save(any(DiscordMessageEntity.class));
        verify(openAIService).sendMessageToOpenAI(any(DiscordMessageEntity.class), eq(true), eq(null));
        verify(messageChannel).createMessage(eq(AI_RESPONSE));
        verify(mockCreateMono).onErrorMap(any());
    }

    /**
     * Tests ignoring a message without the correct prefix.
     * @since 1.0
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

        verifyNoInteractions(discordMessageRepository, openAIService, messageChannel);
    }

    /**
     * Tests ignoring a message with empty content after prefix removal.
     * @since 1.0
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

        verifyNoInteractions(discordMessageRepository, openAIService, messageChannel);
    }

    /**
     * Tests handling of {@link OpenAIServiceException} during OpenAI processing.
     * @since 1.0
     */
    @Test
    void shouldHandleOpenAIError() {
        // Arrange
        String inputMessage = BOT_PREFIX + USER_MESSAGE;
        DiscordMessageEntity userMessage = createDiscordMessageEntity(1L, USER_MESSAGE, DISCORD_NICK);
        when(message.getContent()).thenReturn(inputMessage);
        when(messageCreateEvent.getMessage()).thenReturn(message);
        when(message.getAuthor()).thenReturn(Optional.of(user));
        when(user.getUsername()).thenReturn(DISCORD_NICK);
        when(message.getChannel()).thenReturn(Mono.just(messageChannel));
        when(discordMessageRepository.save(any(DiscordMessageEntity.class))).thenReturn(userMessage);
        when(openAIService.sendMessageToOpenAI(any(DiscordMessageEntity.class), eq(true), eq(null)))
                .thenThrow(new OpenAIServiceException("OpenAI API error"));

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(discordMessageRepository).save(any(DiscordMessageEntity.class));
        verify(openAIService).sendMessageToOpenAI(any(DiscordMessageEntity.class), eq(true), eq(null));
        verifyNoInteractions(messageChannel);
    }

    /**
     * Tests handling of {@link DiscordServiceException} during database save operation.
     * @since 1.0
     */
    @Test
    void shouldHandleDatabaseSaveError() {
        // Arrange
        String inputMessage = BOT_PREFIX + USER_MESSAGE;
        when(message.getContent()).thenReturn(inputMessage);
        when(messageCreateEvent.getMessage()).thenReturn(message);
        when(message.getAuthor()).thenReturn(Optional.of(user));
        when(user.getUsername()).thenReturn(DISCORD_NICK);
        when(message.getChannel()).thenReturn(Mono.just(messageChannel));
        when(discordMessageRepository.save(any(DiscordMessageEntity.class)))
                .thenThrow(new DiscordServiceException("Database save error"));

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(discordMessageRepository).save(any(DiscordMessageEntity.class));
        verifyNoInteractions(openAIService, messageChannel);
    }

    /**
     * Tests handling of {@link DiscordServiceException} when accessing the Discord channel.
     * @since 1.0
     */
    @Test
    void shouldHandleDiscordChannelError() {
        // Arrange
        String inputMessage = BOT_PREFIX + USER_MESSAGE;
        when(message.getContent()).thenReturn(inputMessage);
        when(messageCreateEvent.getMessage()).thenReturn(message);
        when(message.getAuthor()).thenReturn(Optional.of(user));
        when(user.getUsername()).thenReturn(DISCORD_NICK);
        when(message.getChannel()).thenReturn(Mono.error(new DiscordServiceException("Channel not found")));

        // Act
        Mono<Message> result = discordBotService.handleMessageCreateEvent(messageCreateEvent);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verifyNoInteractions(discordMessageRepository, openAIService, messageChannel);
    }

    /**
     * Tests handling of a message when the author is not present in the event.
     * @since 1.0
     */
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    @Test
    void shouldHandleMissingAuthor() {
        // Arrange
        String inputMessage = BOT_PREFIX + USER_MESSAGE;
        when(message.getContent()).thenReturn(inputMessage);
        when(messageCreateEvent.getMessage()).thenReturn(message);
        when(message.getAuthor()).thenReturn(Optional.empty());

        // Act & Assert
        DiscordServiceException ex = assertThrows(
                DiscordServiceException.class,
                () -> discordBotService.handleMessageCreateEvent(messageCreateEvent)
        );

        assertThat(ex.getMessage()).contains("Failed to retrieve Discord username");

        verifyNoInteractions(discordMessageRepository, openAIService, messageChannel);
    }

    /**
     * Creates a mock DiscordMessageEntity for testing purposes.
     * @param id the ID of the message
     * @param content the content of the message
     * @param discordNick the Discord username
     * @return a DiscordMessageEntity with the specified ID, content, and discordNick
     * @since 1.0
     */
    private DiscordMessageEntity createDiscordMessageEntity(Long id, String content, String discordNick) {
        DiscordMessageEntity entity = new DiscordMessageEntity();
        entity.setId(id);
        entity.setContent(content);
        entity.setDiscordNick(discordNick);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
