package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.dto.RegisterRequest;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthenticationServiceImpl} in the AI Chat Bridge application.
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthenticationServiceTest {

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "testuser@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private RegisterRequest registerRequest;
    private UserEntity userEntity;

    /**
     * Sets up the test environment with mock RegisterRequest and UserEntity.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);
        userEntity = UserEntity.builder()
                .username(TEST_USERNAME)
                .email(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .roles(Set.of(Role.USER))
                .build();
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        // Arrange
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        // Act
        UserEntity result = authenticationService.register(registerRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(result.getRoles()).hasSize(1).contains(Role.USER);
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).encode(TEST_PASSWORD);
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void shouldThrowAuthenticationExceptionForTakenUsername() {
        // Arrange
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(userEntity));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authenticationService.register(registerRequest));
        assertThat(exception.getMessage()).isEqualTo("Username already taken");
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void shouldThrowAuthenticationExceptionForTakenEmail() {
        // Arrange
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(userEntity));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authenticationService.register(registerRequest));
        assertThat(exception.getMessage()).isEqualTo("Email already taken");
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void shouldFindByUsernameSuccessfully() {
        // Arrange
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(userEntity));

        // Act
        UserEntity result = authenticationService.findByUsername(TEST_USERNAME);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(result.getRoles()).hasSize(1).contains(Role.USER);
        verify(userRepository).findByUsername(TEST_USERNAME);
    }

    @Test
    void shouldThrowAuthenticationExceptionForNonExistentUsername() {
        // Arrange
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> authenticationService.findByUsername(TEST_USERNAME));
        assertThat(exception.getMessage()).isEqualTo("User not found: " + TEST_USERNAME);
        verify(userRepository).findByUsername(TEST_USERNAME);
    }

    @Test
    void shouldLoadUserByUsernameSuccessfully() {
        // Arrange
        when(userRepository.findByUsernameOrEmail(TEST_USERNAME, TEST_USERNAME)).thenReturn(Optional.of(userEntity));

        // Act
        UserDetails result = authenticationService.loadUserByUsername(TEST_USERNAME);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(result.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        verify(userRepository).findByUsernameOrEmail(TEST_USERNAME, TEST_USERNAME);
    }

    @Test
    void shouldLoadUserByEmailSuccessfully() {
        // Arrange
        when(userRepository.findByUsernameOrEmail(TEST_EMAIL, TEST_EMAIL)).thenReturn(Optional.of(userEntity));

        // Act
        UserDetails result = authenticationService.loadUserByUsername(TEST_EMAIL);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(result.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        verify(userRepository).findByUsernameOrEmail(TEST_EMAIL, TEST_EMAIL);
    }

    @Test
    void shouldThrowUsernameNotFoundExceptionForNonExistentUser() {
        // Arrange
        when(userRepository.findByUsernameOrEmail(TEST_USERNAME, TEST_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> authenticationService.loadUserByUsername(TEST_USERNAME));
        assertThat(exception.getMessage()).isEqualTo("User not found with username or email: " + TEST_USERNAME);
        verify(userRepository).findByUsernameOrEmail(TEST_USERNAME, TEST_USERNAME);
    }
}
