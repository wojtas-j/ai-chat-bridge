package com.wojtasj.aichatbridge.repository;

import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
public class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    /**
     * Tests saving a user and finding it by username.
     * @since 1.0
     */
    @Test
    void shouldSaveAndFindByUsername() {
        // Arrange
        UserEntity user = buildUser();
        repository.save(user);
        // Act

        Optional<UserEntity> foundUser = repository.findByUsername("testuser");

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
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
        // Arrange
        UserEntity user = buildUser();
        repository.save(user);

        // Act
        Optional<UserEntity> foundUser = repository.findByEmail("test@example.com");

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
    }

    /**
     * Tests finding a non-existent user by email.
     * @since 1.0
     */
    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        // Act
        Optional<UserEntity> foundUser = repository.findByEmail("non-existent@example.com");

        // Assert
        assertThat(foundUser).isEmpty();
    }

    /**
     * Tests saving a user and finding it by username or email.
     * @since 1.0
     */
    @Test
    void shouldSaveAndFindByUsernameOrEmail() {
        // Arrange
        UserEntity user = buildUser();
        repository.save(user);

        // Act
        Optional<UserEntity> foundByUsername = repository.findByUsernameOrEmail("testuser", "wrong@example.com");
        Optional<UserEntity> foundByEmail = repository.findByUsernameOrEmail("wronguser", "test@example.com");

        // Assert
        assertThat(foundByUsername).isPresent();
        assertThat(foundByUsername.get().getUsername()).isEqualTo("testuser");
        assertThat(foundByEmail).isPresent();
        assertThat(foundByEmail.get().getEmail()).isEqualTo("test@example.com");
    }

    /**
     * Tests finding a non-existent user by username or email.
     * @since 1.0
     */
    @Test
    void shouldReturnEmptyWhenUsernameOrEmailNotFound() {
        // Act
        Optional<UserEntity> foundUser = repository.findByUsernameOrEmail("non-existent", "non-existent@example.com");

        // Assert
        assertThat(foundUser).isEmpty();
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
}
