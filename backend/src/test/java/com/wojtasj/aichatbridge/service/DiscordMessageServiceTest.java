package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.DiscordMessageEntity;
import com.wojtasj.aichatbridge.exception.MessageNotFoundException;
import com.wojtasj.aichatbridge.repository.DiscordMessageRepository;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DiscordMessageServiceImpl} in the AI Chat Bridge application.
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class DiscordMessageServiceTest {

    private static final Long MESSAGE_ID = 1L;
    private static final String TEST_CONTENT = "Test Discord message";
    private static final String TEST_DISCORD_NICK = "TestUser#1234";
    private static final String ANOTHER_CONTENT = "Another Discord message";
    private static final String ANOTHER_DISCORD_NICK = "AnotherUser#5678";

    @Mock
    private DiscordMessageRepository discordMessageRepository;

    @InjectMocks
    private DiscordMessageServiceImpl discordMessageService;

    private DiscordMessageEntity discordMessage;

    /**
     * Sets up the test environment with mock UserEntity and DiscordMessageEntity.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        discordMessage = DiscordMessageEntity.builder()
                .id(MESSAGE_ID)
                .content(TEST_CONTENT)
                .discordNick(TEST_DISCORD_NICK)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Tests retrieving all Discord messages successfully.
     * @since 1.0
     */
    @Test
    void shouldGetAllMessagesSuccessfully() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        DiscordMessageEntity anotherMessage = DiscordMessageEntity.builder()
                .id(2L)
                .content(ANOTHER_CONTENT)
                .discordNick(ANOTHER_DISCORD_NICK)
                .createdAt(LocalDateTime.now())
                .build();
        Page<DiscordMessageEntity> page = new PageImpl<>(List.of(discordMessage, anotherMessage), pageable, 2);
        when(discordMessageRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<DiscordMessageEntity> result = discordMessageService.getAllMessages(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(MESSAGE_ID);
        assertThat(result.getContent().get(0).getContent()).isEqualTo(TEST_CONTENT);
        assertThat(result.getContent().get(0).getDiscordNick()).isEqualTo(TEST_DISCORD_NICK);
        assertThat(result.getContent().get(1).getId()).isEqualTo(2L);
        assertThat(result.getContent().get(1).getContent()).isEqualTo(ANOTHER_CONTENT);
        assertThat(result.getContent().get(1).getDiscordNick()).isEqualTo(ANOTHER_DISCORD_NICK);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(discordMessageRepository).findAll(pageable);
    }

    /**
     * Tests retrieving an empty page of Discord messages successfully.
     * @since 1.0
     */
    @Test
    void shouldGetEmptyPageWhenNoMessagesExist() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        Page<DiscordMessageEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(discordMessageRepository.findAll(pageable)).thenReturn(emptyPage);

        // Act
        Page<DiscordMessageEntity> result = discordMessageService.getAllMessages(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(discordMessageRepository).findAll(pageable);
    }

    /**
     * Tests deleting a Discord message successfully.
     * @since 1.0
     */
    @Test
    void shouldDeleteMessageSuccessfully() {
        // Arrange
        when(discordMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(discordMessage));
        doNothing().when(discordMessageRepository).delete(discordMessage);

        // Act
        discordMessageService.deleteMessage(MESSAGE_ID);

        // Assert
        verify(discordMessageRepository).findById(MESSAGE_ID);
        verify(discordMessageRepository).delete(discordMessage);
    }

    /**
     * Tests throwing MessageNotFoundException when deleting a non-existent Discord message.
     * @since 1.0
     */
    @Test
    void shouldThrowMessageNotFoundExceptionForNonExistentMessage() {
        // Arrange
        when(discordMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

        // Act & Assert
        MessageNotFoundException exception = assertThrows(MessageNotFoundException.class,
                () -> discordMessageService.deleteMessage(MESSAGE_ID));
        assertThat(exception.getMessage()).isEqualTo("Discord message not found with ID: " + MESSAGE_ID);
        verify(discordMessageRepository).findById(MESSAGE_ID);
        verify(discordMessageRepository, never()).delete(any());
    }
}
