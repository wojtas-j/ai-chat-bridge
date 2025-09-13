package com.wojtasj.aichatbridge.repository;

import com.wojtasj.aichatbridge.configuration.TestBeansConfig;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the CRUD operations of {@link MessageRepository} in the AI Chat Bridge application.
 * @since 1.0
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestBeansConfig.class)
public class MessageRepositoryTest {

    @Autowired
    private MessageRepository repository;

    /**
     * Tests saving a message to the database and retrieving it.
     * @since 1.0
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
