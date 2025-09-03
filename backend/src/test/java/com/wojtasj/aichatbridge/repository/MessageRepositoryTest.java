package com.wojtasj.aichatbridge.repository;

import com.wojtasj.aichatbridge.entity.MessageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MessageRepository.
 */
@DataJpaTest
@ActiveProfiles("test")
public class MessageRepositoryTest {

    @Autowired
    private MessageRepository repository;

    /**
     * Tests saving and retrieving a message.
     */
    @Test
    void shouldSaveAndFindMessage() {
        // Arrange
        MessageEntity message = new MessageEntity();
        message.setContent("Test message");

        // Act
        repository.save(message);

        // Assert
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().getFirst().getContent()).isEqualTo("Test message");
    }
}