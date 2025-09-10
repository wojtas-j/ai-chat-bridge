package com.wojtasj.aichatbridge.service;

import com.wojtasj.aichatbridge.configuration.JwtProperties;
import com.wojtasj.aichatbridge.entity.RefreshTokenEntity;
import com.wojtasj.aichatbridge.entity.Role;
import com.wojtasj.aichatbridge.entity.UserEntity;
import com.wojtasj.aichatbridge.exception.AuthenticationException;
import com.wojtasj.aichatbridge.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RefreshTokenServiceImpl} in the AI Chat Bridge application.
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private UserEntity userEntity;
    private LocalDateTime now;

    /**
     * Sets up the test environment with mock UserEntity and LocalDateTime.
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        userEntity = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .roles(Set.of(Role.USER))
                .build();
        now = LocalDateTime.now();
    }

    /**
     * Tests successful generation of a refresh token.
     * @since 1.0
     */
    @Test
    void shouldGenerateRefreshTokenSuccessfully() {
        // Arrange
        when(jwtProperties.getRefreshExpirationDays()).thenReturn(7L);
        String token = UUID.randomUUID().toString();
        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .token(token)
                .user(userEntity)
                .expiryDate(now.plusDays(7))
                .build();
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenReturn(refreshToken);

        // Act
        RefreshTokenEntity result = refreshTokenService.generateRefreshToken(userEntity);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(token);
        assertThat(result.getUser()).isEqualTo(userEntity);
        assertThat(result.getExpiryDate()).isEqualTo(now.plusDays(7));
        verify(jwtProperties).getRefreshExpirationDays();
        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    /**
     * Tests throwing IllegalArgumentException when refreshExpirationDays is invalid.
     * @since 1.0
     */
    @Test
    void shouldThrowIllegalArgumentExceptionForInvalidExpirationDays() {
        // Arrange
        when(jwtProperties.getRefreshExpirationDays()).thenReturn(0L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.generateRefreshToken(userEntity));
        assertThat(exception.getMessage()).contains("Refresh token expiration time must be positive");
        verify(jwtProperties).getRefreshExpirationDays();
        verify(refreshTokenRepository, never()).save(any());
    }

    /**
     * Tests successful validation of a refresh token.
     * @since 1.0
     */
    @Test
    void shouldValidateRefreshTokenSuccessfully() {
        // Arrange
        String token = UUID.randomUUID().toString();
        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .token(token)
                .user(userEntity)
                .expiryDate(now.plusDays(1))
                .build();
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

        // Act
        RefreshTokenEntity result = refreshTokenService.validateRefreshToken(token);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo(token);
        assertThat(result.getUser()).isEqualTo(userEntity);
        assertThat(result.getExpiryDate()).isEqualTo(now.plusDays(1));
        verify(refreshTokenRepository).findByToken(token);
        verify(refreshTokenRepository, never()).delete(any());
    }

    /**
     * Tests throwing AuthenticationException for an invalid refresh token.
     * @since 1.0
     */
    @Test
    void shouldThrowAuthenticationExceptionForInvalidRefreshToken() {
        // Arrange
        String token = UUID.randomUUID().toString();
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> refreshTokenService.validateRefreshToken(token));
        assertThat(exception.getMessage()).contains("Invalid refresh token");
        verify(refreshTokenRepository).findByToken(token);
        verify(refreshTokenRepository, never()).delete(any());
    }

    /**
     * Tests throwing AuthenticationException for an expired refresh token and deleting it.
     * @since 1.0
     */
    @Test
    void shouldThrowAuthenticationExceptionForExpiredRefreshToken() {
        // Arrange
        String token = UUID.randomUUID().toString();
        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .token(token)
                .user(userEntity)
                .expiryDate(now.minusDays(1))
                .build();
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));
        doNothing().when(refreshTokenRepository).delete(refreshToken);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
                () -> refreshTokenService.validateRefreshToken(token));
        assertThat(exception.getMessage()).contains("Refresh token expired");
        verify(refreshTokenRepository).findByToken(token);
        verify(refreshTokenRepository).delete(refreshToken);
    }

    /**
     * Tests successful deletion of refresh tokens for a user.
     * @since 1.0
     */
    @Test
    void shouldDeleteByUserSuccessfully() {
        // Arrange
        doNothing().when(refreshTokenRepository).deleteByUser(userEntity);

        // Act
        refreshTokenService.deleteByUser(userEntity);

        // Assert
        verify(refreshTokenRepository).deleteByUser(userEntity);
    }

    /**
     * Tests successful deletion of expired refresh tokens.
     * @since 1.0
     */
    @Test
    void shouldDeleteExpiredTokensSuccessfully() {
        // Arrange
        doNothing().when(refreshTokenRepository).deleteByExpiryDateBefore(any(LocalDateTime.class));

        // Act
        refreshTokenService.deleteExpiredTokens();

        // Assert
        verify(refreshTokenRepository).deleteByExpiryDateBefore(any(LocalDateTime.class));
    }
}
