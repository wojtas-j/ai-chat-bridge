package com.wojtasj.aichatbridge.repository;

import com.wojtasj.aichatbridge.configuration.TestBeansConfig;
import com.wojtasj.aichatbridge.entity.RefreshTokenEntity;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the CRUD operations of {@link RefreshTokenRepository} in the AI Chat Bridge application.
 * @since 1.0
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestBeansConfig.class)
public class RefreshTokenRepositoryTest {

    private static final String TEST_USER = "testuser";
    private static final String TEST_TOKEN1 = "test-token1";
    private static final String TEST_TOKEN2 = "test-token2";

    @Autowired
    private RefreshTokenRepository repository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity testUser;
    private RefreshTokenEntity token1;
    private RefreshTokenEntity token2;

    /**
     * Sets up the test environment by creating a sample UserEntity and RefreshTokensEntity.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .username(TEST_USER)
                .email("test@example.com")
                .password("password123P!")
                .roles(Set.of(Role.USER))
                .apiKey("testapikey")
                .maxTokens(100)
                .model("testmodel")
                .build();
        testUser = userRepository.save(testUser);

        token1 = RefreshTokenEntity.builder()
                .token(TEST_TOKEN1)
                .user(testUser)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
        token1 = repository.save(token1);

        token2 = RefreshTokenEntity.builder()
                .token(TEST_TOKEN2)
                .user(testUser)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
        token2 = repository.save(token2);
    }

    /**
     * Tests saving a refresh token and finding it by token value.
     * @since 1.0
     */
    @Test
    void shouldSaveAndFindByToken() {
        // Act
        Optional<RefreshTokenEntity> foundToken = repository.findByToken(TEST_TOKEN1);

        // Assert
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getToken()).isEqualTo(TEST_TOKEN1);
        assertThat(foundToken.get().getUser().getUsername()).isEqualTo(TEST_USER);
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
     * Tests finding all refresh tokens for a user.
     * @since 1.0
     */
    @Test
    void shouldFindByUser() {
        // Act
        List<RefreshTokenEntity> userTokens = repository.findByUser(testUser);

        // Assert
        assertThat(userTokens).hasSize(2);
        assertThat(userTokens).extracting(RefreshTokenEntity::getToken)
                .containsExactlyInAnyOrder(TEST_TOKEN1, TEST_TOKEN2);
        assertThat(userTokens).allMatch(token -> token.getUser().equals(testUser));
    }

    /**
     * Tests finding refresh tokens for a user when no tokens exist.
     * @since 1.0
     */
    @Test
    void shouldReturnEmptyListWhenNoTokensForUser() {
        // Arrange
        UserEntity newUser = UserEntity.builder()
                .username("nouser")
                .email("nouser@example.com")
                .password("password123")
                .roles(Set.of(Role.USER))
                .apiKey("testapikey")
                .maxTokens(100)
                .model("testmodel")
                .build();
        newUser = userRepository.save(newUser);

        // Act
        List<RefreshTokenEntity> userTokens = repository.findByUser(newUser);


        // Assert
        assertThat(userTokens).isEmpty();
    }

    /**
     * Tests deleting refresh tokens by user.
     * @since 1.0
     */
    @Test
    void shouldDeleteByUser() {
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
        assertThat(repository.findByToken(TEST_TOKEN2)).isPresent();
        assertThat(repository.findByToken(TEST_TOKEN1)).isEmpty();
    }
}
