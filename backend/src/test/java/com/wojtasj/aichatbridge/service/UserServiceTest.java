package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.exception.UserAlreadyExistsException;
import com.wojtasj.aichatbridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests the functionality of {@link UserServiceImpl} in the AI Chat Bridge application.
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceTest {

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Password123!";
    private static final String TEST_API_KEY = "test-api-key";
    private static final int TEST_MAX_TOKENS = 100;
    private static final String TEST_MODEL = "gpt-4o-mini";
    private static final String ENCODED_PASSWORD = "encodedPassword";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity userEntity;

    /**
     * Sets up the test environment with mocked dependencies.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        userEntity = UserEntity.builder()
                .username(TEST_USERNAME)
                .email(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .roles(Set.of(Role.USER))
                .apiKey(TEST_API_KEY)
                .maxTokens(TEST_MAX_TOKENS)
                .model(TEST_MODEL)
                .build();
    }

    @Nested
    class UpdatePasswordTests {
        /**
         * Tests updating the user's password successfully.
         * @since 1.0
         */
        @SuppressWarnings({"ConstantConditions", "DataFlowIssue"})
        @Test
        void shouldUpdatePasswordSuccessfully() {
            // Arrange
            String newPassword = "NewPassword123!";
            String encodedNewPassword = "encodedNewPassword";
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
            when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
            doNothing().when(refreshTokenService).deleteByUser(userEntity);

            // Act
            userService.updatePassword(TEST_USERNAME, TEST_PASSWORD, newPassword);

            // Assert
            assertThat(userEntity.getPassword()).isEqualTo(encodedNewPassword);
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken(TEST_USERNAME, TEST_PASSWORD));
            verify(passwordEncoder).encode(newPassword);
            verify(userRepository).save(userEntity);
            verify(refreshTokenService).deleteByUser(userEntity);
        }

        /**
         * Tests throwing AuthenticationException when user is not found during password update.
         * @since 1.0
         */
        @Test
        void shouldThrowAuthenticationExceptionForNonExistentUser() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

            // Act & Assert
            AuthenticationException exception = assertThrows(AuthenticationException.class,
                    () -> userService.updatePassword(TEST_USERNAME, TEST_PASSWORD, "NewPassword123!"));
            assertThat(exception.getMessage()).isEqualTo("User not found with username: " + TEST_USERNAME);

            // Verify
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(authenticationManager, never()).authenticate(any());
            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
            verify(refreshTokenService, never()).deleteByUser(any());
        }

        /**
         * Tests throwing AuthenticationException when current password is incorrect.
         * @since 1.0
         */
        @Test
        void shouldThrowAuthenticationExceptionForIncorrectCurrentPassword() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(userEntity));
            doThrow(new BadCredentialsException("Invalid credentials"))
                    .when(authenticationManager)
                    .authenticate(new UsernamePasswordAuthenticationToken(TEST_USERNAME, "wrongpassword"));

            // Act & Assert
            AuthenticationException exception = assertThrows(AuthenticationException.class,
                    () -> userService.updatePassword(TEST_USERNAME, "wrongpassword", "NewPassword123!"));
            assertThat(exception.getMessage()).isEqualTo("Current password is incorrect");

            // Verify
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken(TEST_USERNAME, "wrongpassword"));
            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
            verify(refreshTokenService, never()).deleteByUser(any());
        }
    }

    @Nested
    class UpdateEmailTests {
        /**
         * Tests updating the user's email successfully.
         * @since 1.0
         */
        @Test
        void shouldUpdateEmailSuccessfully() {
            // Arrange
            String newEmail = "newemail@example.com";
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(userEntity));
            when(userRepository.findByEmail(newEmail)).thenReturn(Optional.empty());
            when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

            // Act
            userService.updateEmail(TEST_USERNAME, newEmail);

            // Assert
            assertThat(userEntity.getEmail()).isEqualTo(newEmail);
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(userRepository).findByEmail(newEmail);
            verify(userRepository).save(userEntity);
        }

        /**
         * Tests throwing AuthenticationException when user is not found during email update.
         * @since 1.0
         */
        @Test
        void shouldThrowAuthenticationExceptionForNonExistentUser() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

            // Act & Assert
            AuthenticationException exception = assertThrows(AuthenticationException.class,
                    () -> userService.updateEmail(TEST_USERNAME, "newemail@example.com"));
            assertThat(exception.getMessage()).isEqualTo("User not found with username: " + TEST_USERNAME);

            // Verify
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(userRepository, never()).findByEmail(any());
            verify(userRepository, never()).save(any());
        }

        /**
         * Tests throwing UserAlreadyExistsException when email is already in use.
         * @since 1.0
         */
        @Test
        void shouldThrowUserAlreadyExistsExceptionForTakenEmail() {
            // Arrange
            String newEmail = "existing@example.com";
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(userEntity));
            when(userRepository.findByEmail(newEmail)).thenReturn(Optional.of(UserEntity.builder()
                    .email(newEmail)
                    .build()));

            // Act & Assert
            UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class,
                    () -> userService.updateEmail(TEST_USERNAME, newEmail));
            assertThat(exception.getMessage()).isEqualTo("Email already in use: " + newEmail);

            // Verify
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(userRepository).findByEmail(newEmail);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateApiKeyTests {
        /**
         * Tests updating the user's OpenAI API key successfully.
         * @since 1.0
         */
        @Test
        void shouldUpdateApiKeySuccessfully() {
            // Arrange
            String newApiKey = "sk-new";
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(userEntity));
            when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

            // Act
            userService.updateApiKey(TEST_USERNAME, newApiKey);

            // Assert
            assertThat(userEntity.getApiKey()).isEqualTo(newApiKey);
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(userRepository).save(userEntity);
        }

        /**
         * Tests throwing AuthenticationException when user is not found during API key update.
         * @since 1.0
         */
        @Test
        void shouldThrowAuthenticationExceptionForNonExistentUser() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

            // Act & Assert
            AuthenticationException exception = assertThrows(AuthenticationException.class,
                    () -> userService.updateApiKey(TEST_USERNAME, "sk-newxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"));
            assertThat(exception.getMessage()).isEqualTo("User not found with username: " + TEST_USERNAME);

            // Verify
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateMaxTokensTests {
        /**
         * Tests updating the user's max tokens successfully.
         * @since 1.0
         */
        @Test
        void shouldUpdateMaxTokensSuccessfully() {
            // Arrange
            int newMaxTokens = 200;
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(userEntity));
            when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

            // Act
            userService.updateMaxTokens(TEST_USERNAME, newMaxTokens);

            // Assert
            assertThat(userEntity.getMaxTokens()).isEqualTo(newMaxTokens);
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(userRepository).save(userEntity);
        }

        /**
         * Tests throwing AuthenticationException when user is not found during max tokens update.
         * @since 1.0
         */
        @Test
        void shouldThrowAuthenticationExceptionForNonExistentUser() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

            // Act & Assert
            AuthenticationException exception = assertThrows(AuthenticationException.class,
                    () -> userService.updateMaxTokens(TEST_USERNAME, 200));
            assertThat(exception.getMessage()).isEqualTo("User not found with username: " + TEST_USERNAME);

            // Verify
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateModelTests {
        /**
         * Tests updating the user's model successfully.
         * @since 1.0
         */
        @Test
        void shouldUpdateModelSuccessfully() {
            // Arrange
            String newModel = "gpt-4";
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(userEntity));
            when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

            // Act
            userService.updateModel(TEST_USERNAME, newModel);

            // Assert
            assertThat(userEntity.getModel()).isEqualTo(newModel);
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(userRepository).save(userEntity);
        }

        /**
         * Tests throwing AuthenticationException when user is not found during model update.
         * @since 1.0
         */
        @Test
        void shouldThrowAuthenticationExceptionForNonExistentUser() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

            // Act & Assert
            AuthenticationException exception = assertThrows(AuthenticationException.class,
                    () -> userService.updateModel(TEST_USERNAME, "gpt-4"));
            assertThat(exception.getMessage()).isEqualTo("User not found with username: " + TEST_USERNAME);

            // Verify
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteAccountTests {
        /**
         * Tests deleting the user's account successfully.
         * @since 1.0
         */
        @Test
        void shouldDeleteAccountSuccessfully() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(userEntity));
            doNothing().when(refreshTokenService).deleteByUser(userEntity);
            doNothing().when(userRepository).delete(userEntity);

            // Act
            userService.deleteAccount(TEST_USERNAME);

            // Assert
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(refreshTokenService).deleteByUser(userEntity);
            verify(userRepository).delete(userEntity);
        }

        /**
         * Tests throwing AuthenticationException when user is not found during account deletion.
         * @since 1.0
         */
        @Test
        void shouldThrowAuthenticationExceptionForNonExistentUser() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

            // Act & Assert
            AuthenticationException exception = assertThrows(AuthenticationException.class,
                    () -> userService.deleteAccount(TEST_USERNAME));
            assertThat(exception.getMessage()).isEqualTo("User not found with username: " + TEST_USERNAME);

            // Verify
            verify(userRepository).findByUsername(TEST_USERNAME);
            verify(refreshTokenService, never()).deleteByUser(any());
            verify(userRepository, never()).delete(any());
        }
    }

    @Nested
    class FindByUsernameTests {
        /**
         * Tests finding a user by username successfully.
         * @since 1.0
         */
        @Test
        void shouldFindByUsernameSuccessfully() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(userEntity));

            // Act
            UserEntity result = userService.findByUsername(TEST_USERNAME);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(result.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(result.getRoles()).hasSize(1).contains(Role.USER);
            assertThat(result.getApiKey()).isEqualTo(TEST_API_KEY);
            assertThat(result.getMaxTokens()).isEqualTo(TEST_MAX_TOKENS);
            assertThat(result.getModel()).isEqualTo(TEST_MODEL);
            verify(userRepository).findByUsername(TEST_USERNAME);
        }

        /**
         * Tests throwing AuthenticationException when user is not found by username.
         * @since 1.0
         */
        @Test
        void shouldThrowAuthenticationExceptionForNonExistentUser() {
            // Arrange
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

            // Act & Assert
            AuthenticationException exception = assertThrows(AuthenticationException.class,
                    () -> userService.findByUsername(TEST_USERNAME));
            assertThat(exception.getMessage()).isEqualTo("User not found with username: " + TEST_USERNAME);

            // Verify
            verify(userRepository).findByUsername(TEST_USERNAME);
        }
    }
}
