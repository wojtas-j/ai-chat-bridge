package com.wojtasj.aichatbridge.repository;

import com.wojtasj.aichatbridge.configuration.TestBeansConfig;
import com.wojtasj.aichatbridge.entity.MessageEntity;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the CRUD operations of {@link MessageRepository} in the AI Chat Bridge application.
 * @since 1.0
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestBeansConfig.class)
class MessageRepositoryTest {

    @Autowired
    private MessageRepository repository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Tests saving a message to the database and retrieving it.
     * @since 1.0
     */
    @Test
    void shouldSaveAndFindMessage() {
        // Arrange
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .password("Password123!")
                .apiKey("test-api-key")
                .maxTokens(100)
                .roles(Set.of(Role.USER))
                .build();
        user = userRepository.save(user);

        MessageEntity message = MessageEntity.builder()
                .content("Test message")
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        // Act
        MessageEntity savedMessage = repository.save(message);

        // Assert
        assertThat(savedMessage).isNotNull();
        assertThat(savedMessage.getId()).isNotNull();
        assertThat(repository.findAll()).hasSize(1);
        UserEntity finalUser = user;
        assertThat(repository.findById(savedMessage.getId()).orElse(null))
                .isNotNull()
                .satisfies(found -> {
                    assertThat(found.getContent()).isEqualTo("Test message");
                    assertThat(found.getUser().getId()).isEqualTo(finalUser.getId());
                    assertThat(found.getCreatedAt()).isNotNull();
                });
    }

    /**
     * Tests finding messages by user ID with pagination.
     * @since 1.0
     */
    @Test
    @Transactional
    void shouldFindMessagesByUserIdWithPagination() {
        // Arrange
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .password("Password123!")
                .apiKey("test-api-key")
                .maxTokens(100)
                .roles(Set.of(Role.USER))
                .build();
        user = userRepository.save(user);

        MessageEntity message1 = MessageEntity.builder()
                .content("First message")
                .user(user)
                .build();
        repository.save(message1);
        message1.setCreatedAt(LocalDateTime.now());

        MessageEntity message2 = MessageEntity.builder()
                .content("Second message")
                .user(user)
                .build();
        repository.save(message2);
        message1.setCreatedAt(LocalDateTime.now().minusDays(1));
        
        Pageable pageable = PageRequest.of(0, 1, Sort.by("createdAt").descending());

        // Act
        Page<MessageEntity> page = repository.findByUserId(user.getId(), pageable);

        // Assert
        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getContent()).isEqualTo("Second message");
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getPageable().getPageNumber()).isEqualTo(0);
        assertThat(page.getPageable().getPageSize()).isEqualTo(1);
    }

    /**
     * Tests finding messages by user ID when no messages exist.
     * @since 1.0
     */
    @Test
    void shouldReturnEmptyPageWhenNoMessagesForUser() {
        // Arrange
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .password("Password123!")
                .apiKey("test-api-key")
                .maxTokens(100)
                .roles(Set.of(Role.USER))
                .build();
        user = userRepository.save(user);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

        // Act
        Page<MessageEntity> page = repository.findByUserId(user.getId(), pageable);

        // Assert
        assertThat(page).isNotNull();
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0);
        assertThat(page.getTotalPages()).isEqualTo(0);
        assertThat(page.getPageable().getPageNumber()).isEqualTo(0);
        assertThat(page.getPageable().getPageSize()).isEqualTo(20);
    }

    /**
     * Tests filtering messages by user ID when multiple users exist.
     * @since 1.0
     */
    @Test
    void shouldFilterMessagesByUserId() {
        // Arrange
        UserEntity user1 = UserEntity.builder()
                .username("user1")
                .email("user1@example.com")
                .password("Password123!")
                .apiKey("api-key-1")
                .maxTokens(100)
                .roles(Set.of(Role.USER))
                .build();
        UserEntity user2 = UserEntity.builder()
                .username("user2")
                .email("user2@example.com")
                .password("Password123!")
                .apiKey("api-key-2")
                .maxTokens(100)
                .roles(Set.of(Role.USER))
                .build();
        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);

        MessageEntity message1 = MessageEntity.builder()
                .content("Message for user1")
                .user(user1)
                .createdAt(LocalDateTime.now())
                .build();
        MessageEntity message2 = MessageEntity.builder()
                .content("Message for user2")
                .user(user2)
                .createdAt(LocalDateTime.now())
                .build();
        repository.saveAll(List.of(message1, message2));

        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

        // Act
        Page<MessageEntity> user1Messages = repository.findByUserId(user1.getId(), pageable);
        Page<MessageEntity> user2Messages = repository.findByUserId(user2.getId(), pageable);

        // Assert
        assertThat(user1Messages.getContent()).hasSize(1);
        assertThat(user1Messages.getContent().getFirst().getContent()).isEqualTo("Message for user1");
        assertThat(user1Messages.getContent().getFirst().getUser().getId()).isEqualTo(user1.getId());

        assertThat(user2Messages.getContent()).hasSize(1);
        assertThat(user2Messages.getContent().getFirst().getContent()).isEqualTo("Message for user2");
        assertThat(user2Messages.getContent().getFirst().getUser().getId()).isEqualTo(user2.getId());
    }
}
