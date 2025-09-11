package com.wojtasj.aichatbridge.repository;

import com.wojtasj.aichatbridge.entity.RefreshTokenEntity;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the CRUD operations of {@link RefreshTokenRepository} in the AI Chat Bridge application.
 * @since 1.0
 */
@DataJpaTest
@ActiveProfiles("test")
public class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository repository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity testUser;
    private RefreshTokenEntity token1;
    private RefreshTokenEntity token2;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .roles(Set.of(Role.USER))
                .build();
        testUser = userRepository.save(testUser);

        token1 = RefreshTokenEntity.builder()
                .token("test-token1")
                .user(testUser)
                .build();

        token2 = RefreshTokenEntity.builder()
                .token("test-token2")
                .user(testUser)
                .build();
    }

    /**
     * Tests saving a refresh token and finding it by token value.
     * @since 1.0
     */
    @Test
    void shouldSaveAndFindByToken() {
        // Act
        repository.save(token1);
        Optional<RefreshTokenEntity> foundToken = repository.findByToken("test-token1");

        // Assert
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getToken()).isEqualTo("test-token1");
        assertThat(foundToken.get().getUser().getUsername()).isEqualTo("testuser");
    }

    /**
     * Tests finding a non-existent token.
     * @since 1.0
     */
    @Test
    void shouldReturnEmptyWhenTokenNotFound() {
        // Act
        Optional<RefreshTokenEntity> foundToken = repository.findByToken("non-existent-token");

        // Assert
        assertThat(foundToken).isEmpty();
    }

    /**
     * Tests deleting refresh tokens by user.
     * @since 1.0
     */
    @Test
    void shouldDeleteByUser() {
        // Arrange
        repository.save(token1);
        repository.save(token2);

        // Act
        repository.deleteByUser(testUser);

        // Assert
        assertThat(repository.findAll()).isEmpty();
    }

    /**
     * Tests deleting expired refresh tokens.
     * @since 1.0
     */
    @Test
    void shouldDeleteByExpiryDateBefore() {
        // Arrange
        token1.setExpiryDate(LocalDateTime.now().minusDays(1));
        token2.setExpiryDate(LocalDateTime.now().plusDays(1));

        repository.save(token1);
        repository.save(token2);


        // Act
        repository.deleteByExpiryDateBefore(LocalDateTime.now());

        // Assert
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findByToken("test-token2")).isPresent();
        assertThat(repository.findByToken("test-token1")).isEmpty();
    }
}
