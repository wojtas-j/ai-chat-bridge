package com.wojtasj.aichatbridge.repository;

import com.wojtasj.aichatbridge.configuration.TestBeansConfig;
import com.wojtasj.aichatbridge.entity.DiscordMessageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the CRUD operations of {@link DiscordMessageRepository} in the AI Chat Bridge application.
 * @since 1.0
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestBeansConfig.class)
class DiscordMessageRepositoryTest {

    private static final String TEST_CONTENT = "Test message";
    private static final String TEST_DISCORD_NICK = "TestUser#1234";
    private static final String ANOTHER_CONTENT = "Another message";
    private static final String ANOTHER_DISCORD_NICK = "AnotherUser#5678";

    @Autowired
    private DiscordMessageRepository repository;

    private DiscordMessageEntity testMessage;

    /**
     * Sets up the test environment by creating a sample DiscordMessageEntity.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        testMessage = DiscordMessageEntity.builder()
                .content(TEST_CONTENT)
                .discordNick(TEST_DISCORD_NICK)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Tests saving a Discord message and finding it by ID.
     * @since 1.0
     */
    @Test
    void shouldSaveAndFindById() {
        // Act
        DiscordMessageEntity saved = repository.save(testMessage);
        Optional<DiscordMessageEntity> found = repository.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo(TEST_CONTENT);
        assertThat(found.get().getDiscordNick()).isEqualTo(TEST_DISCORD_NICK);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    /**
     * Tests finding all Discord messages.
     * @since 1.0
     */
    @Test
    void shouldFindAllDiscordMessages() {
        // Arrange
        DiscordMessageEntity anotherMessage = DiscordMessageEntity.builder()
                .content(ANOTHER_CONTENT)
                .discordNick(ANOTHER_DISCORD_NICK)
                .createdAt(LocalDateTime.now())
                .build();
        repository.saveAll(List.of(testMessage, anotherMessage));

        // Act
        List<DiscordMessageEntity> messages = repository.findAll();

        // Assert
        assertThat(messages).hasSize(2);
        assertThat(messages).extracting(DiscordMessageEntity::getContent)
                .containsExactlyInAnyOrder(TEST_CONTENT, ANOTHER_CONTENT);
        assertThat(messages).extracting(DiscordMessageEntity::getDiscordNick)
                .containsExactlyInAnyOrder(TEST_DISCORD_NICK, ANOTHER_DISCORD_NICK);
    }

    /**
     * Tests deleting a Discord message by ID.
     * @since 1.0
     */
    @Test
    void shouldDeleteDiscordMessageById() {
        // Arrange
        DiscordMessageEntity saved = repository.save(testMessage);

        // Act
        repository.deleteById(saved.getId());
        Optional<DiscordMessageEntity> found = repository.findById(saved.getId());

        // Assert
        assertThat(found).isEmpty();
    }

    /**
     * Tests finding a non-existent Discord message by ID.
     * @since 1.0
     */
    @Test
    void shouldReturnEmptyWhenMessageNotFound() {
        // Act
        Optional<DiscordMessageEntity> found = repository.findById(999L);

        // Assert
        assertThat(found).isEmpty();
    }
}
