package com.wojtasj.aichatbridge.repository;

import com.wojtasj.aichatbridge.entity.RefreshTokenEntity;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
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

    /**
     * Tests saving a refresh token and finding it by token value.
     * @since 1.0
     */
    @Test
    void shouldSaveAndFindByToken() {
        // Arrange
        UserEntity user = buildUser();
        user = userRepository.save(user);

        RefreshTokenEntity token = buildRefreshToken("test-token", user);

        // Act
        repository.save(token);
        Optional<RefreshTokenEntity> foundToken = repository.findByToken("test-token");

        // Assert
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getToken()).isEqualTo("test-token");
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
        UserEntity user = buildUser();
        user = userRepository.save(user);

        RefreshTokenEntity token1 = buildRefreshToken("test-token1", user);

        RefreshTokenEntity token2 = buildRefreshToken("test-token2", user);

        repository.save(token1);
        repository.save(token2);

        // Act
        repository.deleteByUser(user);

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
        UserEntity user = buildUser();
        user = userRepository.save(user);

        RefreshTokenEntity token1 = buildRefreshToken("token1", user);
        token1.setExpiryDate(LocalDateTime.now().minusDays(1));

        RefreshTokenEntity token2 = buildRefreshToken("token2", user);

        repository.save(token1);
        repository.save(token2);

        // Act
        repository.deleteByExpiryDateBefore(LocalDateTime.now());

        // Assert
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findByToken("token2")).isPresent();
        assertThat(repository.findByToken("token1")).isEmpty();
    }

    /**
     * Builds a UserEntity object with default test values.
     * @return a UserEntity with username "testuser", email "test@example.com",
     *         password "password", and a single role USER
     * @since 1.0
     */
    private UserEntity buildUser() {
        return UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .roles(Set.of(Role.USER))
                .build();
    }

    /**
     * Builds a RefreshTokenEntity object associated with a given UserEntity.
     * @param token the refresh token string
     * @param savedUser the UserEntity to associate the token with
     * @return a RefreshTokenEntity with the specified token and associated user
     * @since 1.0
     */
    private RefreshTokenEntity buildRefreshToken(String token, UserEntity savedUser) {
        return RefreshTokenEntity.builder()
                .token(token)
                .user(savedUser)
                .build();
    }

}
