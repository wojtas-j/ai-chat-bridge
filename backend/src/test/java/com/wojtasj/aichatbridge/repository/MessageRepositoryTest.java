package com.wojtasj.aichatbridge.repository;

import com.wojtasj.aichatbridge.entity.MessageEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
@DataJpaTest
@Testcontainers
public class MessageRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6-alpine")
            .withDatabaseName("aichatbridge")
            .withUsername("devuser")
            .withPassword("devpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MessageRepository repository;

    /**
     * Tests saving and retriving a message
     */
    @Test
    void shouldSaveAndFindMessage(){
        // Given
        MessageEntity message = new MessageEntity();
        message.setContent("Test message");

        //When
        repository.save(message);

        //Then
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().getFirst().getContent()).isEqualTo("Test message");
    }
}
