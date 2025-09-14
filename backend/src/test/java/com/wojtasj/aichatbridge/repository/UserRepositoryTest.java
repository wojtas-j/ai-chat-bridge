package com.wojtasj.aichatbridge.repository;

import com.wojtasj.aichatbridge.configuration.TestBeansConfig;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the CRUD operations of {@link UserRepository} in the AI Chat Bridge application.
 * @since 1.0
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestBeansConfig.class)
public class UserRepositoryTest {

    private static final String TEST_USER = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_NON_EXISTENT_EMAIL = "non-existent@example.com";

    @BeforeEach
    void setUp() {
        UserEntity testUser = UserEntity.builder()
                .username(TEST_USER)
                .email(TEST_EMAIL)
                .password("password123P!")
                .roles(Set.of(Role.USER))
                .apiKey("testapikey")
                .maxTokens(100)
                .build();
        repository.save(testUser);
    }

    @Autowired
    private UserRepository repository;

    /**
     * Tests saving a user and finding it by username.
     * @since 1.0
     */
    @Test
    void shouldSaveAndFindByUsername() {
        // Act
        Optional<UserEntity> foundUser = repository.findByUsername(TEST_USER);

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo(TEST_USER);
        assertThat(foundUser.get().getEmail()).isEqualTo(TEST_EMAIL);
    }

    /**
     * Tests finding a non-existent user by username.
     * @since 1.0
     */
    @Test
    void shouldReturnEmptyWhenUsernameNotFound() {
        // Act
        Optional<UserEntity> foundUser = repository.findByUsername("non-existent");

        // Assert
        assertThat(foundUser).isEmpty();
    }

    /**
     * Tests saving a user and finding it by email.
     * @since 1.0
     */
    @Test
    void shouldSaveAndFindByEmail() {
        // Act
        Optional<UserEntity> foundUser = repository.findByEmail(TEST_EMAIL);

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo(TEST_USER);
        assertThat(foundUser.get().getEmail()).isEqualTo(TEST_EMAIL);
    }

    /**
     * Tests finding a non-existent user by email.
     * @since 1.0
     */
    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        // Act
        Optional<UserEntity> foundUser = repository.findByEmail(TEST_NON_EXISTENT_EMAIL);

        // Assert
        assertThat(foundUser).isEmpty();
    }

    /**
     * Tests saving a user and finding it by username or email.
     * @since 1.0
     */
    @Test
    void shouldSaveAndFindByUsernameOrEmail() {
        // Act
        Optional<UserEntity> foundByUsername = repository.findByUsernameOrEmail(TEST_USER, "wrong@example.com");
        Optional<UserEntity> foundByEmail = repository.findByUsernameOrEmail("wronguser", TEST_EMAIL);

        // Assert
        assertThat(foundByUsername).isPresent();
        assertThat(foundByUsername.get().getUsername()).isEqualTo(TEST_USER);
        assertThat(foundByEmail).isPresent();
        assertThat(foundByEmail.get().getEmail()).isEqualTo(TEST_EMAIL);
    }

    /**
     * Tests finding a non-existent user by username or email.
     * @since 1.0
     */
    @Test
    void shouldReturnEmptyWhenUsernameOrEmailNotFound() {
        // Act
        Optional<UserEntity> foundUser = repository.findByUsernameOrEmail("non-existent", TEST_NON_EXISTENT_EMAIL);

        // Assert
        assertThat(foundUser).isEmpty();
    }
}
